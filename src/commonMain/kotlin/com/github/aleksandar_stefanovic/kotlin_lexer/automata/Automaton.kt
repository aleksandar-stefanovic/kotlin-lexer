package com.github.aleksandar_stefanovic.kotlin_lexer.automata

class Automaton(
    val states: IntRange,
    val edges: Set<Edge>,
    val startState: Int = states.first(),
    val endStates: Map<Int, List<String>> // TODO should this be just a string, or a token with an optional value?
) {
    data class Edge(val from: Int, val to: Int, val symbol: Char) {
        override fun toString() = "($from, $to, $symbol)"
    }

    fun toDFA() {
        TODO()
    }

    fun acceptString(str: String): String? {

        var currentState = startState
        var lastFinalState: Int? = null

        for (char in str) {
            if (currentState in endStates) {
                lastFinalState = currentState
            }

            val edgeToNext = edges.find { it.from == currentState && it.symbol == char }

            if (edgeToNext != null) {
                currentState = edgeToNext.to
            } else {
                break
            }
        }

        // Due to rule priority, it should return the token with the max priority
        return endStates[lastFinalState]?.maxOrNull() // FIXME: max, first or last?
    }
}