package com.github.aleksandar_stefanovic.kotlin_lexer.regex

import com.github.aleksandar_stefanovic.kotlin_lexer.util.splitBy

/**
 *  Kotlin standard library has a Regular expression class (see [kotlin.text.Regex]), but the issue is that, the output
 *  of a parsed Regex object is a black-box automaton that simply accepts or rejects the provided input,
 *  but this project requires the output of parsed regex to be an AST of the expression itself, so that it could be
 *  transformed into an automaton, and as such be combined with other automata to create a lexer.
 *
 *  Supported operators:
 *    - Escaping special characters (but not shorthand character groups)
 *    - Character set []
 *    - single characters [asd]
 *    - character ranges [a-z0-9]
 *    - negation [^8-9]
 *    - Grouping ()
 *    - Duplication * + ? {i,j}
 *    - Concatenation abcd
 *    - Alternation (a|b)
 *
 *
 *
 *
 *  UNSUPPORTED operators:
 *    - Character classes ([[:lower:]], [[:word:]] etc.)
 *    - Shorthand character classes (\s, \w, \d etc.)
 *    - Anchors (^, $)
 *
 */
class RegExpr(/* @Language("RegExp") */ private val expression: String, val token: Any? = null) {

    init {
        if (expression.isEmpty()) {
            error("Expression must not be empty")
        }
    }

    fun compile(): AST {
        return runPasses(expression.map(::Unparsed)).also {
            it.token = token
        }

    }

    private fun runPasses(asts: List<AST>): AST {
        return asts
            .run { escapePass(this) }
            .run { characterSetPass(this) }
            .run { groupingPass(this) }
            .run { repeatPass(this) }
            .run { rangeRepeatPass(this) }
            .run { concatenationPass(this) }
            .run { alternationPass(this) }
    }

    private fun escapePass(asts: List<AST>): List<AST> {
        val resultingList = mutableListOf<AST>()
        var index = 0
        while (index < asts.size) {
            val possibleEscapeCharacter = asts[index]
            if (possibleEscapeCharacter is Unparsed && possibleEscapeCharacter.char == '\\') {
                if (index + 1 < asts.size) {
                    val charToEscape = asts[index + 1]
                    // TODO check whether it's escaping a special character, or referring to a character group
                    if (charToEscape is Unparsed) {
                        resultingList.add(Literal(charToEscape.char))
                        index += 2
                    }
                } else {
                    error("Escape cannot be the last character")
                }
            } else {
                resultingList.add(possibleEscapeCharacter)
                index++
            }
        }

        return resultingList
    }

    private fun characterSetPass(asts: List<AST>): List<AST> {
        return bracketsPairing(asts, '[', ']', ::CharacterSet)
    }

    private fun groupingPass(asts: List<AST>): List<AST> {
        return bracketsPairing(asts, '(', ')') { Grouping(runPasses(it)) }
    }

    private fun repeatPass(asts: List<AST>): List<AST> {

        asts[0].let {
            if (it is Unparsed && it.char in listOf('*', '+', '?')) {
                error("Dangling metacharacter")
            }
        }

        val newAsts = mutableListOf<AST>()
        var index = 0
        while (index < asts.size - 1) {

            val currentAST = asts[index]

            val lookaheadAst = asts[index + 1]
            if (lookaheadAst is Unparsed) {
                when (lookaheadAst.char) {
                    '*' -> {
                        newAsts.add(Repeat.Star(currentAST))
                        index += 2
                    }
                    '+' -> {
                        newAsts.add(Repeat.Plus(currentAST))
                        index += 2
                    }
                    '?' -> {
                        newAsts.add(Repeat.QuestionMark(currentAST))
                        index += 2
                    }
                    else -> {
                        newAsts.add(currentAST)
                        index += 1
                    }
                }
            } else {
                newAsts.add(currentAST)
                index += 1
            }
        }

        asts.getOrNull(index)?.let {
            newAsts.add(it) // Add only if valid index
        }

        return newAsts
    }

    private fun rangeRepeatPass(asts: List<AST>): List<AST> {

        // TODO check for dangling metacharacters

        var index = 0

        val newAsts = mutableListOf<AST>()

        fun AST.isEqual(char: Char) = (this as? Unparsed)?.char == char

        fun AST.isNum() = (this as? Unparsed)?.char in '0'..'9'

        while (index < asts.size - 1) {

            val currentAST = asts[index]

            var innerIndex = index + 1

            if (!asts[innerIndex].isEqual('{')) {
                newAsts.add(currentAST)
                index++
                continue
            }

            innerIndex++

            var firstNum = 0
            var matchedAtLeastOnce = false
            while (innerIndex < asts.size && asts[innerIndex].isNum()) {
                matchedAtLeastOnce = true
                firstNum *= 10
                firstNum += (asts[innerIndex] as? Unparsed)?.char?.toString()?.toInt()!!
                innerIndex++
            }

            if (!matchedAtLeastOnce) {
                newAsts.add(currentAST)
                index++
                continue
            }

            if (innerIndex >= asts.size || !asts[innerIndex].isEqual(',')) {
                newAsts.add(currentAST)
                index++
                continue
            }

            innerIndex++
            var secondNum = 0
            matchedAtLeastOnce = false
            @Suppress("DuplicatedCode")
            while (innerIndex < asts.size && asts[innerIndex].isNum()) {
                matchedAtLeastOnce = true
                secondNum *= 10
                secondNum += (asts[innerIndex] as? Unparsed)?.char?.toString()?.toInt()!!
                innerIndex++
            }

            if (!matchedAtLeastOnce) {
                newAsts.add(currentAST)
                index++
                continue
            }

            if (innerIndex >= asts.size || !asts[innerIndex].isEqual('}')) {
                newAsts.add(currentAST)
                index++
                continue
            }

            // All checks passed, this is a valid range quantifier
            newAsts.add(Repeat.Range(currentAST, firstNum..secondNum))
            index = innerIndex + 1
        }

        if (index < asts.size) {
            newAsts.add(asts[index])
        }

        return newAsts
    }

    private fun concatenationPass(asts: List<AST>): List<AST> {

        if (asts.size == 1) {
            return asts
        }

        val newAsts = mutableListOf<AST>()

        // These will be parsed in later passes, but for now they must be ignored
        val charactersToAvoid = listOf('^', '$', '|')

        val accumulatedASTs = mutableListOf<AST>()
        asts.forEach { ast ->
            if (ast is SingleCharacter && ast.char in charactersToAvoid) {
                // Merge previous asts into a Concatenation, and then add the avoided symbol
                if (accumulatedASTs.isNotEmpty()) { // TODO if only one member, don't create concatenation
                    if (accumulatedASTs.size == 1) {
                        newAsts.add(accumulatedASTs.first())
                    } else {
                        newAsts.add(Concatenation(accumulatedASTs.toList() /* Create copy */))
                    }
                    accumulatedASTs.clear()
                }
                newAsts.add(ast)
            } else {
                accumulatedASTs.add(ast)
            }
        }

        if (accumulatedASTs.isNotEmpty()) {
            // If there are any asts in the "buffer", empty the buffer into resulting asts
            if (accumulatedASTs.size == 1) {
                newAsts.add(accumulatedASTs.first())
            } else {
                newAsts.add(Concatenation(accumulatedASTs))
            }
        }

        return newAsts
    }

    private fun alternationPass(asts: List<AST>): AST {

        val firstAST = asts.first()

        if (firstAST is Unparsed && firstAST.char == '|') {
            error("Alternation cannot be the first character")
        }

        val lastAST = asts.last()

        if (lastAST is Unparsed && lastAST.char == '|') {
            error("Alternation cannot be the last character")
        }

        // Splits into a list of lists, and each nested list should be a singleton
        val split = asts.splitBy { it is Unparsed && it.char == '|' }

        if (split.size == 1) {
            /*
            No alternation symbols in expression
            If there are no alternating symbols, then some other AST should be the root, and the only element in the
            list should be that exact root. Otherwise, something went wrong with the passes.
            */

            if (asts.size != 1) {
                error("Something went wrong")
            }

            return asts[0]
        }

        if (split.any { it.size > 1 }) {
            /*
            Something went wrong with the regex passes. Since this is the last pass, the only things that
            should occur is that there are no alternation metacharacters (case covered above), or that there are
            single ASTs between alternation symbols, but, if there are any lists between alternations that aren't
            singletons, something went wrong.
            */
            error("Something went wrong")
        }

        val splitAsts = split.flatten() // Since those are singletons, flatten to the list of their elements

        // Singleton list
        return Alternation(splitAsts)
    }

    private fun bracketsPairing(
        asts: List<AST>,
        openChar: Char,
        closedChar: Char,
        factory: (List<AST>) -> AST
    ): List<AST> {
        // Detecting character sets
        val openBracketIndexList = mutableListOf<Int>()
        val closedBracketIndexList = mutableListOf<Int>()

        // Find all the brackets
        asts.forEachIndexed { i, ast ->
            if (ast is Unparsed) {
                when (ast.char) {
                    openChar -> openBracketIndexList.add(i)
                    closedChar -> closedBracketIndexList.add(i)
                }
            }
        }

        if (openBracketIndexList.size != closedBracketIndexList.size) {
            error("Unmatched brackets")
        }

        val bracketPairs = (openBracketIndexList zip closedBracketIndexList).toMutableList()

        // Construct the new list, taking into consideration both the unchanged characters, and the new character sets
        val newAsts = mutableListOf<AST>()
        var index = 0
        while (index < asts.size) {
            val indices = bracketPairs.find { (open, closed) -> index in (open..closed) }
            if (indices != null) {
                val characterClassContents = asts.subList(indices.first + 1, indices.second)
                bracketPairs.remove(indices)
                newAsts.add(factory(characterClassContents))
                index = indices.second + 1
            } else {
                newAsts.add(asts[index])
                index++
            }
        }

        return newAsts
    }


}