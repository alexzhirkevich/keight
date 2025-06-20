package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.thisRef


internal class JSSetFunction : JSFunction(
    name = "Set",
    prototype = Object {
        "size".js eq 0.js
        "add".js.func("value") {
            if (it.isNotEmpty()) {
                thisRef<MutableCollection<JsAny?>>().add(it[0])
            }
            Undefined
        }
        "clear".js.func() {
            thisRef<MutableCollection<JsAny?>>().clear()
            Undefined
        }
        "delete".js.func("value" defaults OpConstant(Undefined)) {
            val v = it[0]
            val s = thisRef<MutableCollection<JsAny?>>()
            val exists = s.contains(v)
            s.remove(v)
            exists.js
        }
        "has".js.func("value" defaults OpConstant(Undefined)) {
            thisRef<Iterable<JsAny?>>().contains(it[0]).js
        }
        "difference".js.func("other" defaults OpConstant(Undefined)) {
            thisRef<Iterable<JsAny?>>().subtract(it[0] as Iterable<JsAny?>).js
        }
        "symmetricDifference".js.func("other" defaults OpConstant(Undefined)) {
            val s = thisRef<Iterable<JsAny?>>()
            val other = (it[0] as Iterable<JsAny?>).toSet()

            buildSet {
                s.forEach { v -> if (v !in other) add(v) }
                other.forEach { v -> if (v !in s) add(v) }
            }.js
        }
        "union".js.func("other" defaults OpConstant(Undefined)) {
            val s = thisRef<Iterable<JsAny?>>()
            val other = it[0] as Iterable<JsAny?>
            s.union(other).js
        }
        "intersection".js.func("other" defaults OpConstant(Undefined)) {
            val s = thisRef<Iterable<JsAny?>>()
            val other = it[0] as Iterable<JsAny?>
            s.intersect(other).js
        }
        "isDisjointFrom".js.func("other" defaults OpConstant(Undefined)) {
            val s = thisRef<Iterable<JsAny?>>()
            val other = it[0] as Iterable<JsAny?>
            other.all { v -> v !in s }.js
        }
        "isSubsetOf".js.func("other" defaults OpConstant(Undefined)) {
            val s = thisRef<Iterable<JsAny?>>()
            val other = it[0] as Iterable<JsAny?>
            other.all { v -> v in s }.js
        }
        "isSupersetOf".js.func("other" defaults OpConstant(Undefined)) {
            val s = thisRef<Iterable<JsAny?>>()
            val other = it[0] as Iterable<JsAny?>
            s.all { v -> v in other }.js
        }
        "entries".js.func {
            val iter = thisRef<Iterable<JsAny?>>().iterator()
            var i = 0
            sequence<JsAny?> {
                while (iter.hasNext()) {
                    yield(listOf((++i).js, iter.next()).js)
                }
            }.iterator().js
        }
        "keys".js.func {
            thisRef<Iterable<JsAny?>>().iterator().js
        }
        "values".js.func {
            thisRef<Iterable<JsAny?>>().iterator().js
        }
        "forEach".js.func {
            val callback = it[0].callableOrThrow(this)
            val s = thisRef
            thisRef<Iterable<JsAny?>>().forEachIndexed { idx, v ->
                callback.invoke(listOf(v, (idx + 1).js, s), this)
            }
            Undefined
        }
    }
) {

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        runtime.typeError { "Constructor Set requires 'new'".js }
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        if (args.isEmpty()){
            return mutableSetOf<JsAny>().js
        }
        val x = args[0]

        return when {
            x is Iterable<*> -> (x as Iterable<JsAny?>).toSet().js
//            x is JsWrapper<*> && x.value is Iterable<*> -> (x.value as Iterable<*>).toSet()
            else -> runtime.typeError { "$x is not iterable".js }
        }
    }
}