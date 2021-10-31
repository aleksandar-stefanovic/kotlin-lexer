package com.github.aleksandar_stefanovic.kotlin_lexer.automata

class DFA(
    val startState: Int,
    val endStates: Map<Int, List<Any>>, // Map that contains final states as keys, and associated tokens as values
    val edges: Set<Edge>
) {

    // No ε-edges
    data class Edge(val from: Int, val to: Int, val char: Char)

    // Returns single longest lexeme it can find in input
    // TODO needs to match the whole input, and return a sequence of lexemes
    fun matchInput(input: String): Pair<String?, List<Any>?> {

        val chars = input.iterator()
        var currentState: Int? = startState
        var position = 0

        while (chars.hasNext()) {
            val char = chars.nextChar()
            currentState = edges.find { (from, _, symbol) -> from == currentState && char == symbol }?.to
            if (currentState != null) {
                position++
            } else {
                break
            }
        }

        return if (currentState in endStates.keys) {
            Pair(input.substring(0, position), endStates[currentState]!!)
        } else {
            Pair(null, null)
        }
    }

    override fun toString(): String {
        return """DFA(startState=$startState, endStates=$endStates, 
            |edges=${edges.joinToString("\n")})""".trimMargin()
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