package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.thisRef

internal class JSMapFunction : JSFunction(
    name = "Map",
    prototype = Object {
        "size".js eq 0.js
        "set".js.func(
            FunctionParam("key"),
            "value" defaults OpConstant(Undefined)
        ) { args ->
            thisRef<MutableMap<JsAny?,JsAny?>>()[args[0]] = args[1]
            Undefined
        }
        "get".js.func("key" defaults OpConstant(Undefined)) {
            val key = it.getOrElse(0) { Undefined }
            thisRef<Map<JsAny?,JsAny?>>()[key]
        }
        "clear".js.func {
            thisRef<MutableMap<JsAny?,JsAny?>>().clear()
            Undefined
        }
        "delete".js.func("key" defaults OpConstant(Undefined)) {
            val k = it[0]
            val map = thisRef<MutableMap<JsAny?, JsAny?>>()
            val exists = map.containsKey(k)
            map.remove(k)
            exists.js
        }
        "has".js.func("key") {
            thisRef<Map<JsAny?,JsAny?>>()
                .containsKey(it.getOrElse(0) { Undefined }).js
        }
        "entries".js.func {
            thisRef<Map<JsAny?,JsAny?>>().entries
                .map { listOf(it.key, it.value).js }
                .iterator().js
        }
        "keys".js.func {
            thisRef<Map<JsAny?,JsAny?>>().keys.iterator().js
        }
        "values".js.func {
            thisRef<Map<JsAny?,JsAny?>>().values.iterator().js
        }
        "forEach".js.func("callback") {
            val callback = it[0].callableOrThrow(this)
            val map = thisRef<Map<JsAny?,JsAny?>>()
            val m = thisRef
            map.forEach { (k,v) ->
                callback.invoke(listOf(v, k, m), this)
            }
            Undefined
        }
        JsSymbol.iterator.func {
            thisRef<Map<JsAny?,JsAny?>>().entries
                .map { listOf(it.key, it.value).js }
                .iterator().js
        }
    }
) {
    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return if (args.isEmpty()) {
            emptyMap<JsAny?, JsAny?>().js
        } else {
            val entries = args[0]

            val mapEntries = when {
                entries is Map<*, *> -> {
                    return LinkedHashMap(entries as Map<JsAny?, JsAny?>).js
                }
                entries is Iterable<*> -> {
                    entries as Iterable<JsAny?>
                    entries.mapNotNull {
                        it?.pairOfFirstEntries(runtime)
                    }
                }

                entries?.isIterator(runtime) == true -> buildList<Pair<JsAny?,JsAny?>> {
                    entries.forEach(runtime) {
                        it?.pairOfFirstEntries(runtime)?.let(::add)
                    }
                }

                else -> runtime.typeError { "Failed to cast $entries to array-like".js }
            }

            mapEntries.toMap().js
        }
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return invoke(args, runtime)
    }
}

private suspend fun JsAny.pairOfFirstEntries(runtime : ScriptRuntime): Pair<JsAny?, JsAny?> {
    return when {
        this is Iterable<*> -> {
            this as Iterable<JsAny?>
            val iter = iterator()
            iter.next() to iter.next()
        }

        isIterator(runtime) -> {
            next(runtime).value(runtime) to next(runtime).value(runtime)
        }

        else -> runtime.typeError { "Failed to cast $this to array-like".js }
    }
}