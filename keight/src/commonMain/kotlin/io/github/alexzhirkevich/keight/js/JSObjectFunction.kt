package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

internal class JSObjectFunction : JSFunction(name = "Object") {

    init {
        func("assign", "target", "source") {
            val target = it[0] as JSObject
            val source = it[1] as JSObject

            source.keys.forEach { k ->
                target[k] = source.get(k, this)
            }
        }
        func("create", "object", "source") {
            TODO()
        }

        func("entries", "object") {
            (it.firstOrNull() as? JSObject)?.entries ?: emptyList<String>()
        }

        noArgsFunc("fromEntries") {
            TODO()
        }

        func("keys", "object") {
            (it.firstOrNull() as? JsAny)?.keys ?: emptySet<String>()
        }

        func("values", "object") {
            (it.firstOrNull() as? JSObject)?.values ?: emptyList<String>()
        }

        func("groupBy", "object", "callback") {
            TODO()
        }

        func("defineProperty", "object", "property", "descriptor") {
            TODO()
        }

        func("defineProperties", "object", "descriptors") {
            TODO()
        }

        func("getOwnPropertyDescriptor", "object", "property") {
            TODO()
        }

        func("getOwnPropertyDescriptors", "object") {
            TODO()
        }

        func("getOwnPropertyNames", "object") {
            TODO()
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
                    this.get("this")
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