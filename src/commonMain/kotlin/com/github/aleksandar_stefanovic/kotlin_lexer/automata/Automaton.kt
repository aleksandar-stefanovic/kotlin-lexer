package com.github.aleksandar_stefanovic.kotlin_lexer.automata

/**
 * Nondeterministic automaton with exactly one start state, and exactly one end state,
 * suitable for applying Thompson's Construction.
 */
class Automaton(val startState: Int, val endState: Int, val edges: Set<Edge>) {

    // Null char indicates ε-edge
    data class Edge(val from: Int, val to: Int, val symbol: Char?) {
        override fun toString() = "($from -${symbol ?: 'ε'}-> $to)"
    }

    // Set of states that can be reached from [states] by ε-edges
    private fun epsilonClosure(states: Set<Int>): Set<Int> {
        val stack = states.toMutableList()
        val epsilonClosure = states.toMutableSet()
        while (stack.isNotEmpty()) {
            val last = stack.removeLast()
            edges.filter { (from, _, char) -> char == null && from == last }
                .map { (_, to, _) -> to }
                .forEach {
                    if (it !in epsilonClosure) {
                        epsilonClosure.add(it)
                        stack.add(it)
                    }
                }
        }

        return epsilonClosure
    }


    // Powerset construction
    fun toDFA(): DFA {

        // Temporary class to indicate state transitions of the resulting DFA
        data class DfaEdge(val from: Set<Int>, val to: Set<Int>, val symbol: Char)

        val dfaStates = mutableSetOf<Set<Int>>()
        val dfaEdges = mutableSetOf<DfaEdge>()
        // States of the resulting DFA, which are denoted by a set of states of NFA
        // Initially has the ε-closure of the start state
        val unmarkedStates: MutableList<Set<Int>> = mutableListOf(epsilonClosure(setOf(startState)))
        while (unmarkedStates.isNotEmpty()) {

            // Move from unmarked to marked
            val dfaState: Set<Int> = unmarkedStates.removeLast()
            dfaStates.add(dfaState)

            // All the states that can be reached from [dfaState] by non-ε symbols
            edges.filter { (from, _, symbol) -> from in dfaState && symbol != null }
                .map { (_, to, symbol) -> Pair(symbol!!, to) } // checked for nulls in previous step
                .groupBy(
                    keySelector = { (symbol, _) -> symbol },
                    valueTransform = { (_, state) -> state }
                )
                .map { (symbol, list) -> Pair(symbol, list.flatMap { epsilonClosure(setOf(it)) }.toSet()) }
                .forEach { (symbol, newDfaState) ->
                    if (newDfaState !in dfaStates && newDfaState !in unmarkedStates) {
                        unmarkedStates.add(newDfaState)
                    }
                    dfaEdges.add(DfaEdge(dfaState, newDfaState, symbol))
                }
        }

        val stateMapping = dfaStates.associateWith { DFA.getUniqueIndex() }

        val dfaStartState = stateMapping[epsilonClosure(setOf(startState))]!!

        val dfaEndStates = stateMapping
            .filter { (stateSet, _) -> this.endState in stateSet }
            .map { (_, state) -> state }
            .toSet()

        val indexedDfaEdges = dfaEdges.map { (fromSet, toSet, symbol) ->
            DFA.Edge(stateMapping[fromSet]!!, stateMapping[toSet]!!, symbol)
        }.toSet()

        return DFA(dfaStartState, dfaEndStates, indexedDfaEdges)
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

    override fun toString(): String {
        return """start state: $startState
          |final state: $endState
          |edges:
          |${edges.joinToString("\n")}
        """.trimMargin()
    }

    companion object {
        // FIXME this causes issues with Kotlin/Native
        // A counter that ensures that the index of every automaton is unique
        @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
        var currentIndex: Int = 0

        fun getUniqueIndexPair(): Pair<Int, Int> {
            return Pair(currentIndex++, currentIndex++)
        }
    }
}