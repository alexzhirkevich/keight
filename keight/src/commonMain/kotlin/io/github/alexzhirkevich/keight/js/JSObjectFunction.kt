package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

internal class JSObjectFunction : JSFunction(name = "Object") {

    init {
        func("assign", "target", "source") {
            val target = it[0] as JSObject
            val source = it[1] as JSObject

            source.keys.forEach { k ->
                target.set(k, source.get(k, this))
            }
        }
        func("create", "object") { args ->
            val obj = args.getOrNull(0) as? JsAny ?: return@func JSObjectImpl()

            Object {
                obj.keys.fastForEach {
                    it eq obj.get(it, this@func)
                }
            }
        }

        func("entries", "object") {
            (it.firstOrNull() as? JSObject)?.entries ?: emptyList<String>()
        }

        func("fromEntries", "object") { args ->
            val entries = (args.getOrNull(0) as? List<List<*>>).orEmpty()
            JSObjectImpl(properties = entries.associate { it[0] to it[1] })
        }

        func("keys", "object") {
            (it.firstOrNull() as? JsAny)?.keys ?: emptySet<String>()
        }

        func("values", "object") {
            (it.firstOrNull() as? JSObject)?.values ?: emptyList<String>()
        }

        func("groupBy", "objects", "callback") { args ->
            val obj = args.getOrNull(0) as? Iterable<JsAny>
                ?: throw TypeError("${args.getOrNull(0)} is not iterable")

            val callback = args.getOrNull(1)?.callableOrNull()
                ?: throw TypeError("${args.getOrNull(1)} is not function")

            JSObjectImpl(
                properties = obj
                    .groupBy { callback.invoke(it.listOf(), this) }
                    .toMutableMap()
            )
        }

        func("defineProperty", "object", "property", "descriptor") { args ->
            val obj = (args.getOrNull(0) as? JSObject)
                ?: throw TypeError("${args.getOrNull(0)} is not an object")

            val name = args.getOrElse(1) {
                return@func Unit
            }.let { JSStringFunction.toString(it, this) }

            val prop = args.getOrNull(2) as? JSObject ?: return@func Unit

            obj.define(this, name, prop)
            obj
        }

        func("defineProperties", "object", "descriptors") { args ->
            val obj = (args.getOrNull(0) as? JSObject)
                ?: throw TypeError("${args.getOrNull(0)} is not an object")

            val props = args.getOrNull(1) as? JSObject ?: return@func Unit

            props.keys.forEach { key ->
                val prop = props.get(key, this) as? JsAny
                    ?: TypeError("$key property of $obj is not an object")

                obj.define(this, key, prop)
            }
            obj
        }

        func("getOwnPropertyDescriptor", "object", "property") { args ->
            val obj = args.getOrNull(0) as? JSObject
                ?: throw TypeError("${args.getOrNull(0)} is not a object")

            val name = args.getOrElse(1) { return@func Unit }

            obj.descriptor(name)?.asObject() ?: Unit
        }

        func("getOwnPropertyDescriptors", "object") { args ->
            val obj = args.getOrNull(0) as? JSObject
                ?: throw TypeError("${args.getOrNull(0)} is not a object")

            JSObjectImpl(
                properties = obj.keys.mapNotNull { k ->
                    obj.descriptor(k)?.asObject()?.let { k to it }
                }.toMap()
            )
        }

        func("getOwnPropertyNames", "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JsAny) { "$obj is not an object"}
            obj.keys
        }

        func("getPrototypeOf", "object") {
            val obj = it.getOrElse(0) {
                throw TypeError("Cannot convert undefined or null to object")
            }

            (obj as? JsAny)?.get(PROTO, this) ?: Unit
        }

        func("preventExtensions", "object") {
            TODO()
        }

        func("isExtensible", "object") {
            TODO()
        }

        func("seal", "object") {
            TODO()
        }

        func("isSealed", "object") {
            TODO()
        }

        func("freeze", "object") {
            TODO()
        }

        func("isFrozen", "object") {
            TODO()
        }

        setPrototype(
            Object("") {
                noArgsFunc("valueOf") {
                    get("this")
                }
            }.apply {
                setProto(null)
            }
        )
    }

    override suspend fun isInstance(obj: Any?, runtime: ScriptRuntime): Boolean {
        return obj is JSObject || super.isInstance(obj, runtime)
    }

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()){
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
        writable = if (property.contains("writable", runtime)) {
            !runtime.isFalse(property.get("writable", runtime))
        } else null,
        enumerable = if (property.contains("enumerable", runtime)) {
            !runtime.isFalse(property.get("enumerable", runtime))
        } else null,
        configurable = if (property.contains("configurable", runtime)) {
            !runtime.isFalse(property.get("configurable", runtime))
        } else null
    )
}