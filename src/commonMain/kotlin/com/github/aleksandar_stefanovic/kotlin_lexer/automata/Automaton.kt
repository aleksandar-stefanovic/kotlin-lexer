package com.github.aleksandar_stefanovic.kotlin_lexer.automata

/**
 * Nondeterministic automaton with exactly one start state, and exactly one end state,
 * suitable for applying Thompson's Construction.
 */
class Automaton(val startState: Int, val endState: Int, val edges: Set<Edge>) {

    // Null char indicates ε-edge
    data class Edge(val from: Int, val to: Int, val symbol: Char?) {
        override fun toString() = "($from, $to, $symbol)"
    }

    fun toDFA() {
        TODO()
    }

    fun clone(): Automaton { // TODO test
        // Each of the existing state indices gets assigned a new value, and those values are stored in a map
        val hashMap = HashMap<Int, Int>()

        fun getNewValue(old: Int): Int {
            return hashMap.getOrPut(old) {
                currentIndex++
            }
        }

        return Automaton(
            getNewValue(startState),
            getNewValue(endState),
            this.edges.map { (from, to, char) -> Edge(getNewValue(from), getNewValue(to), char) }.toSet()
        )

    }

    companion object {
        // FIXME this causes issues with Kotlin/Native
        // A counter that ensures that the index of every automaton is unique
        var currentIndex: Int = 0

        fun getUniqueIndexPair(): Pair<Int, Int> { // TODO test
            return Pair(currentIndex++, currentIndex++)
        }
    }
}