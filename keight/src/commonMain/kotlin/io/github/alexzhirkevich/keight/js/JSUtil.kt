package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.expressions.OpConstant
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

internal fun <T> T.listOf() : List<T> = SingleElementList(this)

@JvmInline
internal value class SingleElementList<T>(private val element : T) : List<T> {

    override val size: Int get() = 1

    override fun contains(element: T): Boolean = this.element == element

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { it == element }

    override fun get(index: Int): T {
        check(index == 0){
            "Index $index is out of range 0..0"
        }
        return element
    }

    override fun indexOf(element: T): Int = if (element == this.element) 0 else -1

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<T> = iterator { yield(element) }

    override fun lastIndexOf(element: T): Int = if (element == this.element) 0 else -1

    override fun listIterator(): ListIterator<T> = listOf(element).listIterator()

    override fun listIterator(index: Int): ListIterator<T> = listOf(element).listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        check(fromIndex == 0 && toIndex == 0){
            "range $fromIndex..$toIndex is out of range 0..0"
        }
        return SingleElementList(element)
    }
}

internal object ArgOmitted
internal val OpArgOmitted = OpConstant(ArgOmitted)

@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.argOrElse(index: Int, defaultValue: (Int) -> T): T {
    contract {
        callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
    }
    return if (index in indices) {
        get(index).let {
            if (it == ArgOmitted) defaultValue(index) else it
        }
    } else defaultValue(index)
}

@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.argOrNull(index: Int): T? = argOrElse(index) { null }