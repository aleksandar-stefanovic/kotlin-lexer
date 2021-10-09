package com.github.aleksandar_stefanovic.kotlin_lexer.automata

class DFA(val startState: Int, val endStates: Set<Int>, val edges: Set<Edge>) {

    // No ε-edges
    data class Edge(val from: Int, val to: Int, val char: Char)

    // Nullable int because it is not a complete automaton
    private fun traverse(state: Int?, symbol: Char): Int? {
        return edges.find { (from, _, char) -> from == state && char == symbol }?.to
    }

    // Nullable int because it is not a complete automaton, and so a missing edge results in a 'null' state
    fun matchInput(input: String): Boolean {
        val endState = input.fold<Int?>(startState) { currentState, symbol -> traverse(currentState, symbol) }
        return endState in endStates
    }

    companion object {
        // FIXME this causes issues with Kotlin/Native
        // A counter that ensures that the index of every automaton is unique
        @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
        private var currentIndex: Int = 0

        fun getUniqueIndex(): Int {
            return currentIndex++
        }
    }
}