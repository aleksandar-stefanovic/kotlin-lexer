package com.github.aleksandar_stefanovic.kotlin_lexer.regex

sealed class AST {

    abstract class SingleCharacter(val char: Char) : AST() {
        override fun toString() = "'$char'"
    }

    class Unparsed(char: Char) : SingleCharacter(char)

    class Literal(literal: Char) : SingleCharacter(literal)

    class CharacterSet(asts: List<AST> ) : AST() {

        // TODO There could be a smarter way to store ranges, to conserve memory.
        private val chars: MutableList<Char> = mutableListOf()
        private val negated: Boolean

        init {
            val characters = asts.map { it as SingleCharacter }.map { it.char }
            negated = characters[0] == '^'
            var index = if (negated) 1 else 0
            while (index < asts.size) {
                val lookaheadAST = characters.getOrNull(index + 1)

                if (lookaheadAST == '-' && index + 2 < asts.size) {
                    val from = characters[index]
                    val to = characters[index + 2]
                    chars.addAll(from..to)
                    index += 3
                } else {
                    chars.add(characters[index])
                    index++
                }
            }
        }

        override fun toString() =
            chars.joinToString(prefix = "[${if (negated) "^" else ""}", postfix = "]")
    }

    class Grouping(val asts: List<AST>) : AST() {
        override fun toString() = asts.joinToString(prefix = "(", separator = "", postfix = ")")
    }

    class Repeat(val ast: AST, val from: Int, val to: Int) : AST() {
        override fun toString(): String {
            val quantifier = when (from..to) {
                0..Int.MAX_VALUE -> "*"
                1..Int.MAX_VALUE -> "+"
                0..1 -> "?"
                else -> "{$from,$to}"
            }

            return ast.toString() + quantifier
        }
    }

    class Concatenation(val asts: List<AST>) : AST() {
        override fun toString() = asts.joinToString(separator = "")
    }

    class Alternation(val asts: List<AST>) : AST() {
        override fun toString() = asts.joinToString(separator = "|")
    }
}