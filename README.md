# kotlin-lexer
Exploration of lexer development. Currently in an experimental state.

### Goal:
Create a Kotlin Multiplatform lexer, with an ability to load lexical rules externally, or create them with a DSL.

### Non-goals:
- Create a lexer generator
- Abuse Kotlin syntax to make it look like some other language or tool.

### Progress:

- [x] Recognize and parse regular expressions into ASTs (partially done, needs to cover more edge cases and needs more testing)
- [x] Use Thompson's Construction to parse given regular expressions into finite automata (needs testing)
- [x] Use Powerset construction to turn NFAs into DFAs
- [ ] Use minimization to optimize automata
- [ ] Combine automata into a single automaton which recognizes lexemes
- [ ] Add optional values to matched lexemes
- [ ] Create a simple DSL for defining rules
- [ ] Find a way to load external lexical rules.
- [ ] Use coroutines to parallelize execution where it makes sense

### Build

To build this project, import it into IntelliJIDEA, and it will hopefully work (please submit issues if they occur)

### Contributing

See [CONTRIBUTING.md](https://github.com/aleksandar-stefanovic/kotlin-lexer/blob/master/CONTRIBUTING.md)
