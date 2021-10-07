package com.github.aleksandar_stefanovic.kotlin_lexer.regex

import com.github.aleksandar_stefanovic.kotlin_lexer.automata.Automaton
import com.github.aleksandar_stefanovic.kotlin_lexer.automata.Automaton.Edge

sealed class AST {
    /**
     * Use Thompson's construction to convert regex [AST] to an [Automaton]
     */
    abstract fun toAutomaton(): Automaton
}

abstract class SingleCharacter(val char: Char) : AST() {
    override fun toString() = "'$char'"

    override fun toAutomaton(): Automaton {
        val (start, end) = Automaton.getUniqueIndexPair()
        return Automaton(
            start,
            end,
            setOf(Edge(start, end, char))
        )
    }
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

    override fun toAutomaton(): Automaton {
        val (start, end) = Automaton.getUniqueIndexPair()
        return Automaton(
            start,
            end,
            chars.map { char -> Edge(start, end, char) }.toSet()
        )
    }

    override fun toString() =
        chars.joinToString(prefix = "[${if (negated) "^" else ""}", postfix = "]")
}

class Grouping(val asts: List<AST>) : AST() { // TODO grouping can have only one child

    override fun toAutomaton() = asts[0].toAutomaton()

    override fun toString() = asts.joinToString(prefix = "(", separator = "", postfix = ")")
}

sealed class Repeat(protected val ast: AST) : AST() {

    class Star(ast: AST) : Repeat(ast) {
        override fun toAutomaton(): Automaton {
            val (start, end) = Automaton.getUniqueIndexPair()
            val automaton = ast.toAutomaton()
            return Automaton(
                start,
                end,
                automaton.edges
                        + Edge(start, automaton.startState, null)
                        + Edge(automaton.endState, end, null)
                        + Edge(start, end, null)
                        + Edge(automaton.endState, automaton.startState, null)
            )
        }
    }

    class Plus(ast: AST): Repeat(ast) {
        override fun toAutomaton(): Automaton {
            val (start, end) = Automaton.getUniqueIndexPair()
            val automaton = ast.toAutomaton()
            return Automaton(
                start,
                end,
                automaton.edges
                        + Edge(start, automaton.startState, null)
                        + Edge(automaton.endState, end, null)
                        + Edge(automaton.endState, automaton.startState, null)
            )
        }
    }

    class QuestionMark(ast: AST) : Repeat(ast) {
        override fun toAutomaton(): Automaton {
            val (start, end) = Automaton.getUniqueIndexPair()
            val automaton = ast.toAutomaton()
            return Automaton(
                start,
                end,
                automaton.edges
                        + Edge(start, automaton.startState, null)
                        + Edge(automaton.endState, end, null)
                        + Edge(automaton.endState, automaton.startState, null)
            )
        }
    }

    class Range(ast: AST, val range: IntRange) : Repeat(ast) {
        override fun toAutomaton(): Automaton {
            val automaton = ast.toAutomaton()
            val automata = (0 until range.last).map { automaton.clone() }

            // Edges between required parts (in expr. E{2,5}, there are 2 required parts, and 3 optional parts)
            val requiredEdges = (0 until range.first) // TODO test if this works for ranges with exact repeat number, i.e. a{5,5}
                .map { index -> Edge(automata[index].endState, automata[index + 1].startState, null) }

            // Edges after the required part, which indicate that further occurrences aren't required
            val epsilonEdges = range
                .map { it - 1 } // Move to correct indices
                .map { index ->
                    Edge(automata[index].endState, automata.last().endState, null)
                }

            val edges = automata.map { it.edges }.reduce { e1, e2 -> e1 union e2 }
                .union(requiredEdges)
                .union(epsilonEdges)

            return Automaton(
                automata.first().startState,
                automata.last().endState,
                edges
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

class Concatenation(private val asts: List<AST>) : AST() {

    override fun toAutomaton() =
        asts.map { it.toAutomaton() }
            .reduce { a1, a2 ->
                Automaton(
                    startState = a1.startState,
                    endState = a2.endState,
                    edges = a1.edges union a2.edges + Edge(a1.endState, a2.startState, null)
                )
            }

    override fun toString() = asts.joinToString(separator = "")
}

class Alternation(private val asts: List<AST>) : AST() {

    override fun toAutomaton(): Automaton {
        val (start, end) = Automaton.getUniqueIndexPair()
        val automata = asts.map { it.toAutomaton() }
        val edgesFromStart = automata.map { Edge(start, it.startState, null) }
        val edgesToEnd = automata.map { Edge(it.endState, end, null) }

        val edges = automata.map { it.edges }.reduce { s1, s2 -> s1 union s2 } union edgesFromStart union edgesToEnd

        return Automaton(
            start,
            end,
            edges
        )
    }

    override fun toString() = asts.joinToString(separator = "|")
}
