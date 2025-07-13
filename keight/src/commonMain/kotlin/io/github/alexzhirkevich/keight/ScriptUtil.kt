package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSBooleanWrapper
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsSymbol
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.isNumber
import io.github.alexzhirkevich.keight.js.isString
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.js.sameValueZero
import io.github.alexzhirkevich.keight.js.unsafeToNumber
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal fun Delegate(
    a : Expression,
    b : Expression,
    op : suspend ScriptRuntime.(JsAny?, JsAny?) -> JsAny?
) = Expression { op(it, a(it), b(it)) }

internal fun Delegate(
    a : Expression,
    op : suspend ScriptRuntime.(JsAny?) -> JsAny?
) = Expression {
    op(it, a(it))
}

internal object Uninitialized : JsAny by Undefined

internal suspend fun JsAny?.sameValue(other : JsAny?) : Boolean {
    if (!sameType(other)){
        return false
    }

    if (this?.isNumber() == true){
        checkNotNull(other)
        unsafeToNumber().sameValueZero(other.unsafeToNumber())
    }

    return sameValueNotNumber(other)
}

internal suspend fun JsAny?.sameValueNotNumber(other : JsAny?) : Boolean {

    check(sameValue(other))

    if (this == null || this is Undefined){
        return true
    }

    if (isString()){
        return this == other
    }

    if (this is JSBooleanWrapper){
        return this == other
    }

    return this === other
}


internal suspend fun JsAny?.sameType(other : JsAny?) : Boolean {

    return when {
        this is Undefined && other is Undefined -> true
        this == null && other == null -> true
        this is JSBooleanWrapper && other is JSBooleanWrapper -> true
        this?.isNumber() == true && other?.isNumber() == true -> true
        this is JsSymbol && other is JsSymbol -> true
        this?.isString() == true && other?.isString() == true -> true
        this is JsObject && other is JsObject -> true

        else -> false
    }
}



internal fun JsAny.callableOrNull() : Callable? = this as? Callable
internal suspend fun JsAny?.callableOrThrow(runtime: ScriptRuntime) : Callable =
    this?.callableOrNull() ?: runtime.typeError("$this is not a function".js)

internal fun <T : Any?> checkNotEmpty(value : T?) : T {
    check(value != null && value != Unit){
        val type = if (value == null) "null" else "undefined"
        "TypeError: Cannot read properties of $type"
    }
    return value
}

internal fun Any.valueAtIndexOrUnit(index : Int) : JsAny {
    return when (this) {
        is Map<*, *> -> {
            get(index) as JsAny? ?: Undefined
        }

        is List<*> -> getOrElse(index) { Undefined } as JsAny
        is Array<*> -> getOrElse(index) { Undefined } as JsAny
        is CharSequence -> getOrNull(index)?.toString()?.js ?: Undefined
        else -> Undefined
    }
}

internal suspend fun Any.valueAtIndexOrUnit(index : JsAny?, numberIndex : Int, runtime: ScriptRuntime) : JsAny? {
    val indexNorm = when (index){
        is CharSequence -> index.toString().js
        else -> index
    }
    return when (this) {
        is Map<*, *> -> {
            if (indexNorm in this) {
                get(indexNorm) as JsAny
            } else {
                Undefined
            }
        }

        is List<*> -> getOrElse(numberIndex) { Undefined } as JsAny?
        is Array<*> -> getOrElse(numberIndex) { Undefined } as JsAny?
        is CharSequence -> getOrNull(numberIndex)?.toString()?.js ?: Undefined
        is JsAny -> get(indexNorm, runtime)
        else -> Undefined
    }!!
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEachReversed(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices.reversed()) {
        val item = get(index)
        action(item)
    }
}


@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEachIndexed(action: (Int, T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(index, item)
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastAll(predicate: (T) -> Boolean): Boolean {
    contract { callsInPlace(predicate) }
    fastForEach { if (!predicate(it)) return false }
    return true
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastAny(predicate: (T) -> Boolean): Boolean {
    contract { callsInPlace(predicate) }
    fastForEach { if (predicate(it)) return true }
    return false
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFirstOrNull(predicate: (T) -> Boolean): T? {
    contract { callsInPlace(predicate) }
    fastForEach { if (predicate(it)) return it }
    return null
}


@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastSumBy(selector: (T) -> Double): Double {
    contract { callsInPlace(selector) }
    var sum = 0.0
    fastForEach { element ->
        sum += selector(element)
    }
    return sum
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastMap(transform: (T) -> R): MutableList<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach {
        target += transform(it)
    }
    return target
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastMapTo(destination : MutableList<R>, transform: (T) -> R): List<R> {
    contract { callsInPlace(transform) }
    fastForEachIndexed { i, it ->
        destination[i] = transform(it)
    }
    return destination
}

public inline fun <T, R, C : MutableList<in R>> Array<out T>.fastMapTo(destination: C, transform: (T) -> R): C {
    for (i in indices){
        destination[i] = transform(get(i))
    }
    return destination
}

// TODO: should be fastMaxByOrNull to match stdlib
/**
 * Returns the first element yielding the largest value of the given function or `null` if there
 * are no elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R : Comparable<R>> List<T>.fastMaxBy(selector: (T) -> R): T? {
    contract { callsInPlace(selector) }
    if (isEmpty()) return null
    var maxElem = get(0)
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = get(i)
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastLastOrNull(predicate: (T) -> Boolean): T? {
    contract { callsInPlace(predicate) }
    for (index in indices.reversed()) {
        val item = get(index)
        if (predicate(item)) return item
    }
    return null
}

/**
 * Returns a list containing only elements matching the given [predicate].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFilter(predicate: (T) -> Boolean): MutableList<T> {
    contract { callsInPlace(predicate) }
    val target = ArrayList<T>(size)
    fastForEach {
        if (predicate(it)) target += (it)
    }
    return target
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
internal inline fun <T, R> List<T>.fastMapIndexed(
    transform: (index: Int, T) -> R
): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEachIndexed { index, e ->
        target += transform(index, e)
    }
    return target
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
internal inline fun <T, R> List<T>.fastMapIndexedNotNull(
    transform: (index: Int, T) -> R?
): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEachIndexed { index, e ->
        transform(index, e)?.let { target += it }
    }
    return target
}

/**
 * Returns the largest value among all values produced by selector function applied to each element
 * in the collection or null if there are no elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R : Comparable<R>> List<T>.fastMaxOfOrNull(selector: (T) -> R): R? {
    contract { callsInPlace(selector) }
    if (isEmpty()) return null
    var maxValue = selector(get(0))
    for (i in 1..lastIndex) {
        val v = selector(get(i))
        if (v > maxValue) maxValue = v
    }
    return maxValue
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastMapNotNull(transform: (T) -> R?): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach { e ->
        transform(e)?.let { target += it }
    }
    return target
}


/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked
 * on each element of original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastFlatMap(transform: (T) -> Iterable<R>): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach { e ->
        val list = transform(e)
        target.addAll(list)
    }
    return target
}

/**
 * Returns the first value that [predicate] returns `true` for
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 *
 * @throws [NoSuchElementException] if no such element is found
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFirst(predicate: (T) -> Boolean): T {
    contract { callsInPlace(predicate) }
    fastForEach { if (predicate(it)) return it }
    throw NoSuchElementException("Collection contains no element matching the predicate.")
}

/**
 * Copied from Appendable.kt
 */
private fun <T> Appendable.appendElement(element: T, transform: ((T) -> CharSequence)?) {
    when {
        transform != null -> append(transform(element))
        element is CharSequence? -> append(element)
        element is Char -> append(element)
        else -> append(element.toString())
    }
}
