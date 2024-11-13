package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

internal class JSObjectFunction : JSFunction(
    name = "Object",
    prototype = JSObjectImpl(
        properties = mapOf(
            "valueOf" to JSFunction("valueOf", body = Expression { it.thisRef })
        )
    ).apply {
        this.set(PROTO, null)
    },
    properties = listOf(
        "assign".func("target", "source") {
            val target = it[0] as JSObject
            val source = it[1] as JSObject

            source.keys.forEach { k ->
                target.set(k, source.get(k, this), this)
            }
        },
        "create".func( "object") { args ->
            val obj = args.getOrNull(0) as? JsAny ?: return@func JSObjectImpl()
            Object {
                obj.keys.fastForEach {
                    it eq obj.get(it, this@func)
                }
            }
        },
        "entries".func( "object") {
            (it.firstOrNull() as? JSObject)?.entries ?: emptyList<String>()
        },
        "fromEntries".func("object") { args ->
            val entries = (args.getOrNull(0) as? List<List<*>>).orEmpty()
            JSObjectImpl(properties = entries.associate { it[0] to it[1] })
        },

        "keys".func("object") {
            (it.firstOrNull() as? JsAny)?.keys ?: emptySet<String>()
        },
        "values".func( "object") {
            (it.firstOrNull() as? JSObject)?.values ?: emptyList<String>()
        },

        "groupBy".func( "objects", "callback") { args ->
            val obj = args.getOrNull(0) as? Iterable<JsAny>
                ?: throw TypeError("${args.getOrNull(0)} is not iterable")

            val callback = args.getOrNull(1)?.callableOrNull()
                ?: throw TypeError("${args.getOrNull(1)} is not function")

            JSObjectImpl(
                properties = obj
                    .groupBy { callback.invoke(it.listOf(), this) }
                    .toMutableMap()
            )
        },

        "defineProperty".func( "object", "property", "descriptor") { args ->
            val obj = (args.getOrNull(0) as? JSObject)
                ?: throw TypeError("${args.getOrNull(0)} is not an object")

            val name = args.getOrElse(1) {
                return@func Unit
            }.let { JSStringFunction.toString(it, this) }

            val prop = args.getOrNull(2) as? JSObject ?: return@func Unit

            obj.define(this, name, prop)
            obj
        },

        "defineProperties".func( "object", "descriptors") { args ->
            val obj = (args.getOrNull(0) as? JSObject)
                ?: throw TypeError("${args.getOrNull(0)} is not an object")

            val props = args.getOrNull(1) as? JSObject ?: return@func Unit

            props.keys.forEach { key ->
                val prop = props.get(key, this) as? JsAny
                    ?: TypeError("$key property of $obj is not an object")

                obj.define(this, key, prop)
            }
            obj
        },

        "getOwnPropertyDescriptor".func( "object", "property") { args ->
            val obj = args.getOrNull(0) as? JSObject
                ?: throw TypeError("${args.getOrNull(0)} is not a object")

            val name = args.getOrElse(1) { return@func Unit }

            obj.descriptor(name)?.asObject() ?: Unit
        },

        "getOwnPropertyDescriptors".func( "object") { args ->
            val obj = args.getOrNull(0) as? JSObject
                ?: throw TypeError("${args.getOrNull(0)} is not a object")

            JSObjectImpl(
                properties = obj.keys.mapNotNull { k ->
                    obj.descriptor(k)?.asObject()?.let { k to it }
                }.toMap()
            )
        },

        "getOwnPropertyNames".func( "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JsAny) { "$obj is not an object" }
            obj.keys
        },

        "getPrototypeOf".func( "object") {
            val obj = it.getOrElse(0) {
                throw TypeError("Cannot convert undefined or null to object")
            }

            (obj as? JsAny)?.get(PROTO, this) ?: Unit
        },

        "preventExtensions".func( "object") {
            TODO()
        },

        "isExtensible".func( "object") {
            TODO()
        },

        "seal".func( "object") {
            TODO()
        },

        "isSealed".func( "object") {
            TODO()
        },

        "freeze".func( "object") {
            TODO()
        },

        "isFrozen".func( "object") {
            TODO()
        }
    ).associateBy { it.name }.toMutableMap()
) {

    override suspend fun isInstance(obj: Any?, runtime: ScriptRuntime): Boolean {
        return obj is JSObject || super.isInstance(obj, runtime)
    }

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()) {
            JSObjectImpl()
        } else {
            JSObjectImpl()
        }
    }


    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        return invoke(args, runtime)
    }
}

private suspend fun JSObject.define(runtime: ScriptRuntime, name : String, property : JsAny) {
    set(
        property = name,
        value = if (property.contains(name, runtime)) {
            property.get("value", runtime)
        } else {
            get(name, runtime)
        },
        runtime = runtime,
        enumerable = if (property.contains("enumerable", runtime)) {
            !runtime.isFalse(property.get("enumerable", runtime))
        } else null,
        configurable = if (property.contains("configurable", runtime)) {
            !runtime.isFalse(property.get("configurable", runtime))
        } else null,
        writable = if (property.contains("writable", runtime)) {
            !runtime.isFalse(property.get("writable", runtime))
        } else null,
    )
}