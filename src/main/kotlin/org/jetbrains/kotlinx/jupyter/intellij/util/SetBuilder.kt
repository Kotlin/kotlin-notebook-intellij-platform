package org.jetbrains.kotlinx.jupyter.intellij.util

interface SetBuilder<T> {
    fun add(element: T)

    fun build(): Set<T>
}

class FilteringSetBuilder<T>(
    val filter: (T) -> Boolean = { true },
    val modifier: (T) -> T = { it },
) : SetBuilder<T> {
    private val builder = mutableSetOf<T>()

    override fun add(element: T) {
        if (filter(element)) builder.add(modifier(element))
    }

    override fun build() = builder.toSet()
}
