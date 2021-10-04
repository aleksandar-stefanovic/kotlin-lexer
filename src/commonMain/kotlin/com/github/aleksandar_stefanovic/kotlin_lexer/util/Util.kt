package util

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

fun <T> Iterable<T>.splitBy(separator: T): List<List<T>> {
    return this.splitBy { it == separator }
}