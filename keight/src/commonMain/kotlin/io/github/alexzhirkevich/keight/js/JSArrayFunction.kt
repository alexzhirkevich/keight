package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Uninitialized
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.OpEqualsImpl
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.requireThisRef
import io.github.alexzhirkevich.keight.thisRef
import kotlin.math.abs
import kotlin.math.min

internal const val LENGTH = "length"

internal class JSArrayFunction : JSFunction(
    name = "Array",
    properties = mutableMapOf(
        "isArray".js to "isArray".func("object") {
            findJsRoot().Array.isInstance(it.getOrNull(0), this).js
        },
        "from".js to "from".func("source"){
            val source = it[0]
            buildList {
                source?.arrayForEachIndexed(this@func){ _, it ->
                    add(it)
                }
            }.js
        },
        "of".js to "of".func("source".vararg()){
            (it[0] as List<JsAny>).js
        }
    ),
    prototype = Object {
        LENGTH.js eq JsNumberWrapper(0)
        at()
        concat()
        copyWithin()
        //todo: entries
        every()
        //todo: fill
        filter()
        find()
        findIndex()
        findLast()
        findLastIndex()
        flat()
        flatMap()
        forEach()
        includes()
        indexOf()
        join()
        //todo: keys
        lastIndexOf()
        map()
        pop()
        push()
        reduce()
        reduceRight()
        reverse()
        shift()
        slice()
        some()
        sort()
        splice()
        //todo: toLocaleString
        toReversed()
        toSorted()
        toSpliced()
        unshift()
        values()
        //todo: with

        Constants.toString.js.func {
            buildString {
                thisRef?.arrayForEachIndexed(this@func){ _, it ->
                    append(toString(it))
                    append(',')
                }
                if (isNotEmpty()) {
                    deleteAt(lastIndex)
                }
            }.js
        }

        JsSymbol.iterator.func {
            thisRef<JsObject>().arrayIterator(this)
        }
    },
) {
    override suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
        return obj is List<*>
    }

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        if (args.isEmpty()) {
            return emptyList<Undefined>().js
        }

        if (args.size == 1) {
            val size = runtime.toNumber(args[0])
            when {
                size is Long || size is Double && size.isFinite() && abs(size - size.toInt()) < Float.MIN_VALUE ->
                    return List(size.toInt()) { Undefined }.js

                size is Double -> throw RangeError("Invalid array length")
            }
        }
        return args.js
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return invoke(args, runtime)
    }
}

private suspend fun JsAny.arrayIterator(runtime: ScriptRuntime): JsAny {
    return if (this is Iterable<*>){
        this as Iterable<JsAny?>
        this.iterator().js
    } else {
        val length = length(runtime)
        runtime.helperIterator {
            if (it < length){
                IteratorEntry(get(it.js, this@helperIterator))
            } else {
                IteratorDone()
            }
        }
    }
}

private suspend fun JsAny.length(runtime: ScriptRuntime): Int {
    return runtime.toNumber(get(LENGTH.js, runtime)).toInt()
}

internal suspend inline fun JsAny.arrayForEachIndexed(
    runtime: ScriptRuntime,
    startFrom : Int = 0,
    block : (Int, JsAny?) -> Unit
) {
    when (this) {
        is List<*> -> {
            for (i in startFrom until size) {
                block(i, get(i) as JsAny)
            }
        }
        else -> {
            val length = length(runtime)
            for (i in startFrom until length) {
                block(i, get(i.js, runtime))
            }
        }
    }
}

internal suspend inline fun JsAny.arrayForEachIndexedReversed(
    runtime: ScriptRuntime,
    block : (Int, JsAny?) -> Unit
) {
    when (this) {
        is List<*> -> {
            for (i in lastIndex downTo 0) {
                block(i, get(i) as JsAny)
            }
        }
        else -> {
            val length = length(runtime)
            for (i in length -1  downTo 0) {
                block(i, get(i.js, runtime))
            }
        }
    }
}

/**
 * 23.1.3.17 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
 *
 * This method compares searchElement to the elements of the array, in ascending order, using
 * the IsStrictlyEqual algorithm, and if found at one or more indices, returns the smallest
 * such index; otherwise, it returns -1𝔽.
 *
 * 1. Let O be ? ToObject(this value).
 * 2. Let len be ? LengthOfArrayLike(O).
 * 3. If len = 0, return -1𝔽.
 * 4. Let n be ? ToIntegerOrInfinity(fromIndex).
 * 5. Assert: If fromIndex is undefined, then n is 0.
 * 6. If n = +∞, return -1𝔽.
 * 7. Else if n = -∞, set n to 0.
 * 8. If n ≥ 0, then
 *    a. Let k be n.
 * 9. Else,
 *    a. Let k be len + n.
 *    b. If k < 0, set k to 0.
 * 10. Repeat, while k < len,
 *     a. Let Pk be ! ToString(𝔽(k)).
 *     b. Let kPresent be ? HasProperty(O, Pk).
 *     c. If kPresent is true, then
 *        i. Let elementK be ? Get(O, Pk).
 *        ii. If IsStrictlyEqual(searchElement, elementK) is true, return 𝔽(k).
 *     d. Set k to k + 1.
 * 11. Return -1𝔽.
 * */
private fun ObjectScope.indexOf(){
    "indexOf".js.func(
        FunctionParam("search"),
        "position" defaults OpConstant(0.js)
    ) { args ->
        val search = args[0]

        val arr = thisRef<JsObject>()
        if (
            (search is JsNumberWrapper || search is JsNumberObject)
            && toNumber(search).toDouble().isNaN()) {
            return@func (-1).js
        }

        var position = toNumber(
            args.argOrElse(1){ 0.js }
        ).toInt()

        val size = arr.length(this)

        if (size == 0 || position >= size){
            return@func (-1).js
        }

        position = when {
            position < -size -> 0
            position < 0 -> position + size
            else -> position
        }

        for (i in position until size) {
            if (
                arr.hasOwnProperty(i.js, this)
                && OpEqualsImpl(arr.get(i.js, this), search, true, this)
            ) {
                return@func i.js
            }
        }

        (-1).js
    }
}

/**
 * 23.1.3.20 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
 *
 *This method compares searchElement to the elements of the array in descending order using
 * the IsStrictlyEqual algorithm, and if found at one or more indices, returns the largest
 * such index; otherwise, it returns -1𝔽.
 *
 * 1. Let O be ? ToObject(this value).
 * 2. Let len be ? LengthOfArrayLike(O).
 * 3. If len = 0, return -1𝔽.
 * 4. If fromIndex is present, let n be ? ToIntegerOrInfinity(fromIndex); else let n be len - 1.
 * 5. If n = -∞, return -1𝔽.
 * 6. If n ≥ 0, then
 *    a. Let k be min(n, len - 1).
 * 7. Else,
 *    a. Let k be len + n.
 * 8. Repeat, while k ≥ 0,
 *    a. Let Pk be ! ToString(𝔽(k)).
 *    b. Let kPresent be ? HasProperty(O, Pk).
 *    c. If kPresent is true, then
 *       i. Let elementK be ? Get(O, Pk).
 *       ii. If IsStrictlyEqual(searchElement, elementK) is true, return 𝔽(k).
 *    d. Set k to k - 1.
 * 9. Return -1𝔽.
 * */
private fun ObjectScope.lastIndexOf(){
    "lastIndexOf".js.func(
        FunctionParam("search"),
        "position" defaults OpArgOmitted
    ) { args ->
        val arr = thisRef<JsObject>()
        val search = args[0]

        if (
            (search is JsNumberWrapper || search is JsNumberObject)
            && toNumber(search).toDouble().isNaN()) {
            return@func (-1).js
        }

        val size = arr.length(this)

        var position = toNumber(
            args.argOrElse(1) { (size-1).js }
        ).toInt()


        if (position < -size) {
            return@func (-1).js
        }

        position = when {
            position < -size -> return@func (-1).js
            position >= size -> size-1
            position < 0 -> position + size
            else -> position
        }

        for (i in position downTo 0) {
            if (
                arr.hasOwnProperty(i.js, this)
                && OpEqualsImpl(arr.get(i.js, this), search, true, this)
            ) {
                return@func i.js
            }
        }

        (-1).js
    }
}

private fun ObjectScope.forEach(){
    "forEach".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ) { args ->
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }
        thisRef?.arrayForEachIndexed(this@func) { idx, it ->
            if (it !is Uninitialized) {
                callable.call(thisArg, listOf(it, idx.js, arr), this@func)
            }
        }
        Undefined
    }
}
private fun ObjectScope.map() {
    "map".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ) { args ->
        val arr = thisRef
        val length = thisRef?.length(this) ?: 0
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        buildList(length) {
            thisRef?.arrayForEachIndexed(this@func) { idx, it ->
                add(
                    if (it is Uninitialized) {
                        it
                    } else {
                        callable.call(thisArg, listOf(it, idx.js, arr), this@func)
                    }
                )
            }
        }.js
    }
}

private fun ObjectScope.filter() {
    "filter".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ) { args ->
        val arr = thisRef
        val length = thisRef?.length(this) ?: 0
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        buildList(length) {
            thisRef?.arrayForEachIndexed(this@func) { idx, it ->
                if (
                    it !is Uninitialized &&
                    !isFalse(callable.call(thisArg, listOf(it, idx.js, arr), this@func))
                ) {
                    add(it)
                }
            }
        }.js
    }
}

private fun ObjectScope.reduce() {
    "reduce".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ) { args ->
        val v = if (args.size > 1) {
            listOf(args[1]) + thisRef<Iterable<JsAny?>>()
        } else {
            thisRef<Iterable<JsAny?>>()
        }
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        v.reduceIndexed { idx, acc, any ->
            callable.call(thisArg, listOf(acc, any, idx.js, arr), this)
        }
    }
}

private fun ObjectScope.reduceRight() {
    "reduceRight".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ) { args ->
        val v = if (args.size > 1) {
            thisRef<List<JsAny?>>() + args[1]
        } else {
            thisRef<List<JsAny?>>()
        }
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        v.reduceRightIndexed { idx, acc, any ->
            callable.call(thisArg, listOf(acc, any, idx.js, arr), this)
        }
    }
}
private fun ObjectScope.some() {
    "some".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ) { args ->
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        arr?.arrayForEachIndexed(this@func) { idx, it ->
            if (
                it !is Uninitialized
                && !isFalse(callable.call(thisArg, listOf(it, idx.js, arr), this@func))
            ) {
                return@func true.js
            }
        }
        false.js
    }
}
private fun ObjectScope.every() {
    "every".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ) { args ->
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        arr?.arrayForEachIndexed(this@func) { idx, it ->
            if (
                it !is Uninitialized &&
                isFalse(callable.call(thisArg, listOf(it, idx.js, arr), this@func))
            ) {
                return@func false.js
            }
        }
        true.js
    }
}
private fun ObjectScope.find() {
    "find".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ){ args ->
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        arr?.arrayForEachIndexed(this@func) { idx, it ->
            if (
                it !is Uninitialized &&
                isFalse(callable.call(thisArg, listOf(it, idx.js, arr), this@func))
            ) {
                return@func it
            }
        }
        Undefined
    }
}
private fun ObjectScope.findLast() {
    "findLast".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ){ args ->
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        arr?.arrayForEachIndexedReversed(this@func) { idx, it ->
            if (
                it !is Uninitialized &&
                isFalse(callable.call(thisArg, listOf(it, idx.js, arr), this@func))
            ) {
                return@func it
            }
        }
        Undefined
    }
}
private fun ObjectScope.findIndex() {
    "findIndex".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ){ args ->
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        arr?.arrayForEachIndexed(this@func) { idx, it ->
            if (
                it !is Uninitialized &&
                isFalse(callable.call(thisArg, listOf(it, idx.js, arr), this@func))
            ) {
                return@func idx.js
            }
        }
        (-1).js
    }
}
private fun ObjectScope.findLastIndex() {
    "findLastIndex".js.func(
        FunctionParam("callback"),
        "thisArg" defaults OpArgOmitted
    ){ args ->
        val arr = thisRef
        val callable = args[0].callableOrThrow(this)
        val thisArg = args.getOrElse(1) { thisRef }

        arr?.arrayForEachIndexedReversed(this@func) { idx, it ->
            if (
                it !is Uninitialized &&
                isFalse(callable.call(thisArg, listOf(it, idx.js, arr), this@func))
            ) {
                return@func idx.js
            }
        }
        (-1).js
    }
}
private fun ObjectScope.reverse() {
    "reverse".js.func {
        thisRef<MutableList<*>>().reverse(); Undefined
    }
}
private fun ObjectScope.toReversed() {
    "toReversed".js.func {
        val arr = thisRef ?: return@func Undefined
        when (this) {
            is Iterable<*> -> reversed() as List<JsAny>
            else -> {
                val length = arr.length(this)
                buildList {
                    for (i in length - 1 downTo 0) {
                        arr.get(i.js, this@func)
                    }
                }
            }
        }.js
    }
}
private fun ObjectScope.sort() {
    "sort".js.func(
        "comparator" defaults OpArgOmitted
    ) {
        val list = thisRef<MutableList<JsAny?>>()
        val res = list.sorted(it.argOrNull(0), this)
        res.fastForEachIndexed { i, v -> list[i] = v }
        Undefined
    }
}
private fun ObjectScope.toSorted() {
    "toSorted".js.func(
        "comparator" defaults OpArgOmitted
    ) {
        thisRef<List<JsAny?>>()
            .sorted(it.argOrNull(0), this).js
    }
}
private fun ObjectScope.slice() {
    "slice".js.func(
        "start" defaults OpConstant(0.js),
        "end" defaults OpArgOmitted
    ) { args ->
        if (args.isEmpty()) {
            thisRef()
        } else {
            val start = toNumber(args[0]).toInt()
            val list = thisRef<List<JsAny?>>()
            val end = toNumber(args.argOrElse(1) { list.size.js })
                .toInt()
                .coerceIn(0, list.size)
            list.slice(start until end).js
        }
    }
}
private fun ObjectScope.splice() {

    "splice".js.func(
        "value" defaults OpConstant(mutableListOf<JsAny?>().js),
        "deleteCount" defaults OpArgOmitted,
        "items".vararg()
    ) { args ->
        val value = thisRef<MutableList<JsAny?>>()

        val pos = toNumber(args[0]).toInt()
        val remove = toNumber(args.argOrElse(1) { (value.size - pos).js }).toInt()
        val rest = args[2] as Collection<JsAny?>

        val deleted = buildList {
            repeat(remove) {
                add(value.removeAt(pos))
            }
        }

        value.addAll(pos, rest)
        deleted.js
    }
}
private fun ObjectScope.toSpliced() {
    "toSpliced".js.func(
        "value" defaults OpConstant(mutableListOf<JsAny?>().js),
        "deleteCount" defaults OpArgOmitted,
        "items".vararg()
    ) { args ->

        val value = thisRef<List<JsAny?>>().toMutableList()
        val pos = toNumber(args[0]).toInt()
        val remove = toNumber(args.argOrElse(1) { (value.size - pos).js }).toInt()
        val rest = args[2] as List<JsAny?>

        repeat(remove) {
            value.removeAt(pos)
        }

        value.addAll(pos, rest)
        value.js
    }
}
private fun ObjectScope.at() {
    "at".js.func("idx") {
        requireThisRef("Array.prototype.at").get(it[0], this)
    }
}
private fun ObjectScope.includes() {
    "includes".js.func(
        FunctionParam("item"),
        "fromIndex" defaults OpArgOmitted
    ) {
        val v = it[0]
        val fromIndex = it.argOrElse(1) { 0.js }.let { toNumber(it).toInt() }

        thisRef?.arrayForEachIndexed(this, fromIndex) { idx, it ->
            if (OpEqualsImpl(it, v, false, this)) {
                return@func true.js
            }
        }
        false.js
    }
}
private fun ObjectScope.join() {
    "join".js.func("separator") {
        val sep = toString(it.getOrElse(0) { ",".js })
        thisRef<Iterable<JsAny?>>().joinToString(separator = sep).js
    }
}
private fun ObjectScope.push() {
    "push".js.func("items".vararg()) {
        val arr = thisRef as? JsObject ?: return@func Undefined
        val initialLength = arr.length(this)
        var length = arr.length(this) - 1
        it[0]?.forEach(this) {
            arr.set(
                (++length).js, it, this
            )
        }
        (arr.length(this) - initialLength).js
    }
}
private fun ObjectScope.pop() {

    "pop".js.func {
        val value = thisRef<MutableList<JsAny?>>()
        if (value.isEmpty()) {
            Undefined
        } else {
            var res: JsAny?

            do {
                res = value.removeLast()
            } while (value.isNotEmpty() && res is Uninitialized)

            if (res is Uninitialized) Undefined else res
        }
    }
}
private fun ObjectScope.shift() {
    "shift".js.func {
        val value = thisRef<MutableList<JsAny?>>()
        if (value.isEmpty()) Undefined else value.removeLast()
    }
}
private fun ObjectScope.unshift() {
    "unshift".js.func("items".vararg()) {
        val value = thisRef<MutableList<JsAny?>>()
        val items = it[0] as Collection<JsAny?>
        value.addAll(0, items)
        items.size.js
    }
}
private fun ObjectScope.concat() {
    "concat".js.func("items".vararg()) {
        buildList {
            addAll(thisRef<Iterable<JsAny?>>())
            (it[0] as Iterable<JsAny?>).forEach {
                when (it) {
                    is Iterable<*> -> addAll(it as Iterable<JsAny?>)
                    else -> add(it)
                }
            }
        }.js
    }
}
private fun ObjectScope.copyWithin() {
    "copyWithin".js.func(
        FunctionParam("to"),
        FunctionParam("from"),
        "num" defaults OpArgOmitted
    ) {
        val to = toNumber(it[0]).toInt()
        val from = toNumber(it[1]).toInt()
        val value = thisRef<MutableList<JsAny?>>()
        val num = it.argOrNull(2)?.let { toNumber(it) }?.toInt() ?: value.size

        for (i in 0 until num.coerceAtMost(min(value.size - to, value.size - from))) {
            value[to + i] = value[from + i]
        }
        value.js
    }
}
private fun ObjectScope.flat() {
    "flat".js.func("depth" defaults OpArgOmitted) {
        val depth = it.argOrNull(0)
            ?.let { toNumber(it) }
            ?.toInt()
            ?.coerceAtLeast(0)
            ?: 1
        thisRef<JsObject>().flat(this, depth).js
    }
}
private fun ObjectScope.flatMap() {
    "flatMap".js.func("block") { args ->
        val arr = thisRef ?: return@func Undefined
        val length = arr.length(this)
        val callable = args[0].callableOrThrow(this)
        buildList(length) {
            thisRef?.arrayForEachIndexed(this@func) { idx, it ->
                callable.invoke(listOf(it, idx.js, arr), this@func)
                    ?.flat(this@func, 1, this)
            }
        }.js
    }
}
private fun ObjectScope.values() {
    "values".js.func {
        thisRef<JsObject>().arrayIterator(this)
    }

}
private suspend fun JsAny.flat(
    runtime: ScriptRuntime,
    depth : Int,
    collector : MutableList<JsAny?> = mutableListOf()
) : List<JsAny?> {
    arrayForEachIndexed(runtime) { idx, it ->
        if (depth == 0 || it !is List<*>) {
            collector.add(it)
        } else {
            it as List<JsAny?>
            it.flat(runtime, depth - 1, collector)
        }
    }
    return collector
}

private suspend fun List<JsAny?>.sorted(comparator : JsAny?, runtime: ScriptRuntime): List<JsAny?> {
    return if (comparator == null) {
        mergeSort { a, b ->
            runtime.toString(a).compareTo(runtime.toString(b)).toDouble()
        }
    } else {
        val comp = comparator.callableOrThrow(runtime)
        mergeSort { a, b ->
            runtime
                .toNumber(comp.invoke(listOf(a, b), runtime))
                .toDouble().let { if (it.isNaN()) 0.0 else it }
        }
    }
}

private suspend fun merge(
    left: List<JsAny?>,
    right: List<JsAny?>,
    compare: suspend (JsAny?, JsAny?) -> Double
): List<JsAny?> {
    var i = 0
    var j = 0

    return List(left.size + right.size) {
        when {
            i >= left.size -> right[j++]
            j >= right.size -> left[i++]
            compare(left[i], right[j]) <= 0 -> left[i++]
            else -> right[j++]
        }
    }
}

private suspend fun List<JsAny?>.mergeSort(compare: suspend (JsAny?, JsAny?) -> Double): List<JsAny?> {
    if (size <= 1) return this
    val mid = size / 2
    return merge(
        take(mid).mergeSort(compare),
        drop(mid).mergeSort(compare),
        compare
    )
}

