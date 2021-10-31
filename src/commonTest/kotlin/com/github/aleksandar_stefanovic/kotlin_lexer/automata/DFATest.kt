package com.github.aleksandar_stefanovic.kotlin_lexer.automata

import kotlin.test.Test
import kotlin.test.assertEquals

internal class DFATest {

    @Test
    fun matchTrivialTest() {
        val dfa = DFA(
            0,
            mapOf(1 to listOf("Token_A", "Token_B")), // If it finished in state 1, it matched either 'a' or 'b'
            setOf(
                DFA.Edge(0, 1, 'a'),
                DFA.Edge(0, 1, 'b')
            )
        )

        val (matchedString, tokens) = dfa.matchInput("a")
        assertEquals("a", matchedString)
        assertEquals(listOf("Token_A", "Token_B"), tokens)
    }

    @Test
    fun matchTest1() {
        val dfa = DFA(
            0,
            mapOf(
                2 to listOf("Token B"),
                3 to listOf("Token C")
            ),
            setOf(
                DFA.Edge(0, 1, 'a'),
                DFA.Edge(1, 2, 'b'),
                DFA.Edge(1, 3, 'c'),
            )
        )

        val (match1, tokens1) = dfa.matchInput("ab")
        val (match2, tokens2) = dfa.matchInput("ac")

        assertEquals("ab", match1)
        assertEquals(listOf("Token B"), tokens1)
        assertEquals("ac", match2)
        assertEquals(listOf("Token C"), tokens2)

    }
}