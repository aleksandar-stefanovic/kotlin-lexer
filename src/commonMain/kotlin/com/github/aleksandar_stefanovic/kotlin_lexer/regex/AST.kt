package com.github.aleksandar_stefanovic.kotlin_lexer.regex

import com.github.aleksandar_stefanovic.kotlin_lexer.automata.Automaton
import com.github.aleksandar_stefanovic.kotlin_lexer.automata.Automaton.Edge

sealed class AST {
    /**
     * Optional token that will be forwarded into the automaton.
     */
    var token: Any? = null

    fun toAutomaton(): Automaton {
        val (start, end) = Automaton.getUniqueIndexPair()
        return Automaton(
            start,
            setOf(end),
            automatonEdges(start, end),
            if (token != null) mapOf(end to token!!) else emptyMap()
        )
    }

    /** Since Thompson's Construction always adds additional start and final states, classes that implement [AST]
     *  only need to define edges between those states and any internal states they define. This way, instead of
     *  creating a whole automaton, implementing classes have a single responsibility of defining edges, and a lot
     *  of code repetition is avoided.
     */
    protected abstract fun automatonEdges(start: Int, end: Int): Set<Edge>
}

abstract class SingleCharacter(val char: Char) : AST() {

    override fun automatonEdges(start: Int, end: Int) = setOf(Edge(start, end, char))

    override fun toString() = "'$char'"
}

class Unparsed(char: Char) : SingleCharacter(char)

class Literal(literal: Char) : SingleCharacter(literal)

class CharacterSet(asts: List<AST>) : AST() {

    // TODO There could be a smarter way to store ranges, to conserve memory.
    private val chars: MutableList<Char> = mutableListOf()
    private val negated: Boolean // FIXME negation doesn't work

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

    override fun automatonEdges(start: Int, end: Int): Set<Edge> {
        return chars.map { char -> Edge(start, end, char) }.toSet()
    }

    override fun toString() =
        chars.joinToString(prefix = "[${if (negated) "^" else ""}", postfix = "]")
}

class Grouping(val ast: AST) : AST() {

    override fun automatonEdges(start: Int, end: Int): Set<Edge> {
        val automaton = ast.toAutomaton()
        return automaton.edges + Edge(start, automaton.startState, null) + Edge(automaton.endStates.first(), end, null)
    }

    override fun toString() = "($ast)"
}

sealed class Repeat(val ast: AST) : AST() {

    class Star(ast: AST) : Repeat(ast) {

        override fun automatonEdges(start: Int, end: Int): Set<Edge> {
            val automaton = ast.toAutomaton()
            return (automaton.edges
                + Edge(start, automaton.startState, null)
                + Edge(automaton.endStates.first(), end, null)
                + Edge(start, end, null)
                + Edge(automaton.endStates.first(), automaton.startState, null))
        }
    }

    class Plus(ast: AST): Repeat(ast) {

        override fun automatonEdges(start: Int, end: Int): Set<Edge> {
            val automaton = ast.toAutomaton()
            return (automaton.edges
                    + Edge(start, automaton.startState, null)
                    + Edge(automaton.endStates.first(), end, null)
                    + Edge(automaton.endStates.first(), automaton.startState, null))
        }
    }

    class QuestionMark(ast: AST) : Repeat(ast) {

        override fun automatonEdges(start: Int, end: Int): Set<Edge> {
            val automaton = ast.toAutomaton()
            return (automaton.edges
                    + Edge(start, automaton.startState, null)
                    + Edge(automaton.endStates.first(), end, null)
                    + Edge(automaton.endStates.first(), automaton.startState, null))
        }
    }

    class Range(ast: AST, val range: IntRange) : Repeat(ast) {

        override fun automatonEdges(start: Int, end: Int): Set<Edge> {
            val automaton = ast.toAutomaton()
            val automata = (0 until range.last).map { automaton.clone() }

            // Edges between required parts (in expr. E{2,5}, there are 2 required parts, and 3 optional parts)
            val requiredEdges =
                (0 until range.first) // TODO test if this works for ranges with exact repeat number, i.e. a{5,5}
                    .map { index -> Edge(automata[index].endStates.first(), automata[index + 1].startState, null) }

            // Edges after the required part, which indicate that further occurrences aren't required
            val epsilonEdges = (range.first until range.last)
                .map { it - 1 } // Move to correct indices
                .map { index ->
                    Edge(automata[index].endStates.first(), automata.last().endStates.first(), null)
                }

            return (automata.map { it.edges }.reduce { e1, e2 -> e1 union e2 }
                .union(requiredEdges)
                .union(epsilonEdges)
                    + Edge(start, automata.first().startState, null)
                    + Edge(automata.last().endStates.first(), end, null)
            )
        }
    }

    override fun toString(): String {
        val quantifier = when (this) {
            is Star -> "*"
            is Plus -> "+"
            is QuestionMark -> "?"
            is Range -> "{${range.first},${range.last}}"
        }

        return ast.toString() + quantifier
    }
}

class Concatenation(val asts: List<AST>) : AST() {

    override fun automatonEdges(start: Int, end: Int): Set<Edge> {
        val automaton = asts.map { it.toAutomaton() }
            .reduce { a1, a2 ->
                Automaton(
                    startState = a1.startState,
                    endStates = a2.endStates,
                    edges = a1.edges union a2.edges union a1.endStates.map { Edge(it, a2.startState, null) },
                    emptyMap()
                )
            }

        return automaton.edges + Edge(start, automaton.startState, null) union automaton.endStates.map { Edge(it, end, null) }
    }

    override fun toString() = asts.joinToString(separator = "")
}

class Alternation(val asts: List<AST>) : AST() {

    override fun automatonEdges(start: Int, end: Int): Set<Edge> {
        val automata = asts.map { it.toAutomaton() }
        val edgesFromStart = automata.map { Edge(start, it.startState, null) }
        val edgesToEnd = automata.map { Edge(it.endStates.first(), end, null) }

        return automata.map { it.edges }.reduce { s1, s2 -> s1.union(s2) } union edgesFromStart union edgesToEnd
    }

    override fun toString() = asts.joinToString(separator = "|")
}
