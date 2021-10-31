package com.github.aleksandar_stefanovic.kotlin_lexer.util

/**
 * Splits the iterable into multiple lists, by splitting the iterable on the elements for which the [predicate] returns
 * true.
 */
fun <T> Iterable<T>.splitBy(predicate: (T) -> Boolean): List<List<T>> {
    val buffer = mutableListOf<T>()
    val lists: MutableList<List<T>> = mutableListOf()

    this.forEach {
        if (predicate(it)) {
            val copy = buffer.toList() // copies the list
            lists.add(copy)
            buffer.clear()
        } else {
            buffer.add(it)
        }
    }
    // Add the buffer as the last element in the list
    lists.add(buffer)

    return lists
}

infix fun <K, V> Map<K, V>.merge(other: Map<K, V>): Map<K, List<V>> {
    val entryList = this.entries.toList() + other.entries.toList() // Contains duplicate keys, needs to be grouped

    return entryList.groupBy (
        keySelector = { (key, _) -> key },
        valueTransform = { (_, value) -> value}
    )
}