package com.github.aleksandar_stefanovic.kotlin_lexer.automata

import com.github.aleksandar_stefanovic.kotlin_lexer.regex.AST

/**
 * Use Thompson's construction to convert regex [AST] to an [Automaton]
 */
object ThompsonConstruction {

    fun convert(ast: AST) {
        val automaton: Automaton = when (ast) {
            is AST.CharacterSet -> convert(ast)
            is AST.Alternation -> convert(ast)
            is AST.Concatenation -> convert(ast)
            is AST.Grouping -> convert(ast)
            is AST.Repeat -> convert(ast)
            is AST.SingleCharacter -> convert(ast)
        }
    }

    fun convert(ast: AST.CharacterSet): Automaton {
        TODO()
    }

    fun convert(ast: AST.Alternation): Automaton {
        TODO()
    }

    fun convert(ast: AST.Concatenation): Automaton {
        TODO()
    }

    fun convert(ast: AST.Grouping): Automaton {
        TODO()
    }

    fun convert(ast: AST.Repeat): Automaton {
        TODO()
    }

    fun convert(ast: AST.SingleCharacter): Automaton {
        TODO()
    }


}