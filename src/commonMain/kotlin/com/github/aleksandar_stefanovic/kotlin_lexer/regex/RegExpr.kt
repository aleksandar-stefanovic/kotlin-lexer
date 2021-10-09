package com.github.aleksandar_stefanovic.kotlin_lexer.regex

import util.splitBy

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
class RegExpr(/* @Language("RegExp") */ private val expression: String) {

    init {
        if (expression.isEmpty()) {
            error("Expression must not be empty")
        }
    }

    fun compile(): AST {
        return expression.map(::Unparsed)
            .run { escapePass(this) }
            .run { characterSetPass(this) }
            .run { groupingPass(this) }
            .run { repeatPass(this) }
            .run { rangeRepeatPass(this) }
            .run { concatenationPass(this) }
            .run { alternationPass(this) }
    }

    private fun escapePass(asts: List<Unparsed>): List<SingleCharacter> {
        val noSpecialCharsList = mutableListOf<SingleCharacter>()
        var index = 0
        while (index < asts.size) {
            val ast = asts[index]
            if (ast.char == '\\') {
                if (index + 1 < asts.size) {
                    // TODO check whether it's escaping a special character, or referring to a character group
                    noSpecialCharsList.add(Literal(asts[index + 1].char))
                    index += 2
                } else {
                    error("Escape cannot be the last character")
                }
            } else {
                noSpecialCharsList.add(ast)
                index++
            }
        }

        return noSpecialCharsList
    }

    private fun characterSetPass(asts: List<SingleCharacter>): List<AST> {
        return bracketsPairing(asts, '[', ']', ::CharacterSet)
    }

    private fun groupingPass(asts: List<AST>): List<AST> {
        return bracketsPairing(asts, '(', ')', ::Grouping)
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

            val currentAST = asts[index].let {
                if (it is Grouping) {
                    Grouping(repeatPass(it.asts))
                } else {
                    it
                }
            }

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

            val currentAST = asts[index].let {
                if (it is Grouping) {
                    Grouping(rangeRepeatPass(it.asts))
                } else {
                    it
                }
            }

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
            if (asts[0] is Grouping) {
                return listOf(Grouping(concatenationPass((asts[0] as Grouping).asts)))
            }
            return asts
        }

        val newAsts = mutableListOf<AST>()

        // These will be parsed in later passes, but for now they must be ignored
        val charactersToAvoid = listOf('^', '$', '|')

        val accumulatedASTs = mutableListOf<AST>()
        asts.forEach { ast ->
            if (ast is SingleCharacter && ast.char in charactersToAvoid) {
                // Merge previous asts into a Concatenation, and then add the avoided symbol
                if (accumulatedASTs.isNotEmpty()) {
                    newAsts.add(Concatenation(accumulatedASTs))
                    accumulatedASTs.clear()
                }
                newAsts.add(ast)
            } else if (ast is Grouping) {
                accumulatedASTs.add(Grouping(concatenationPass(ast.asts)))
            } else {
                accumulatedASTs.add(ast)
            }
        }

        if (accumulatedASTs.isNotEmpty()) {
            newAsts.add(Concatenation(accumulatedASTs))
        }

        return newAsts
    }

    private fun alternationPass(asts: List<AST>): AST {

        // Apply to groupings
        val newAsts = asts.map {
            if (it is Grouping) {
                Grouping(listOf(alternationPass(it.asts)))
            } else it
        }

        // Splits into a list of lists, and each nested list should be a singleton
        val split = newAsts.splitBy { it is Unparsed && it.char == '|' }

        if (split.size == 1) {
            /*
            No alternation symbols in expression
            If there are no alternating symbols, then some other AST should be the root, and the only element in the
            list should be that exact root. Otherwise, something went wrong with the passes.
            */

            if (newAsts.size != 1) {
                error("Something went wrong")
            }

            return newAsts[0]
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

        val splitAsts = split.map { it.first() }


        if (split.first().isEmpty()) {
            error("Alternation cannot be the first character")
        }

        if (split.last().isEmpty()) {
            error("Alternation cannot be the last character")
        }



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