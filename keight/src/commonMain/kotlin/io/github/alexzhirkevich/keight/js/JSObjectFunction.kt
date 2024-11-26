package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.thisRef

internal class JSObjectFunction : JSFunction(
    name = "Object",
    parameters = listOf(
        FunctionParam("obj")
    ),
    body = Expression {
        with(it) {
            constructObject(it.get("obj"))
        }
    },
    prototype = JSObjectImpl(
        properties = listOf(
            "toString".func { thisRef.toString() },
            "isPrototypeOf".func("object") {
                (thisRef as? JsAny)?.isPrototypeOf(it[0],this) == true
            },
            "hasOwnProperty".func("name" defaults OpArgOmitted) { args ->
                val name = args.getOrElse(0) { return@func false }
                thisRef<JSObject>().hasOwnProperty(name, this)
            },
            "propertyIsEnumerable".func("v" defaults OpArgOmitted) {
                val name = it.argOrElse(0) { return@func false }

                thisRef<JSObject>().propertyIsEnumerable(name, this)
            }
        ).associateBy { it.name }
    ).apply {
        setProto(null)
    },
    properties = listOf(
        "assign".func("target", "source") {
            val target = it[0] as JSObject
            val source = it[1] as JSObject

            source.keys(this).forEach { k ->
                target.set(k, source.get(k, this), this)
            }
        },
        "create".func( "object") { args ->
            val obj = args.getOrNull(0) as? JsAny ?: return@func JSObjectImpl()
            Object {
                obj.keys(this@func).fastForEach {
                    it eq obj.get(it, this@func)
                }
            }
        },
        "entries".func( "object") {
            (it.firstOrNull() as? JSObject)?.entries(this) ?: emptyList<String>()
        },
        "fromEntries".func("object") { args ->
            val entries = (args.getOrNull(0) as? List<List<*>>).orEmpty()
            JSObjectImpl(properties = entries.associate { it[0] to it[1] })
        },

        "keys".func("object") {
            (it.firstOrNull() as? JsAny)?.keys(this) ?: emptySet<String>()
        },
        "values".func( "object") {
            (it.firstOrNull() as? JSObject)?.values(this) ?: emptyList<String>()
        },

        "groupBy".func( "objects", "callback") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is Iterable<*>) {
                "$obj is not iterable"
            }

            val callback = args.getOrNull(1)?.callableOrNull()
                ?: typeError { "${args.getOrNull(1)} is not function" }

            JSObjectImpl(
                properties = obj
                    .groupBy { callback.invoke(it.listOf(), this) }
                    .toMutableMap()
            )
        },

        "defineProperty".func( "object", "property", "descriptor") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JSObject) {
                "$obj is not a object"
            }

            val name = args.getOrElse(1) {
                return@func Unit
            }

            val prop = args.getOrNull(2) as? JSObject ?: return@func Unit

            obj.define(this, name, prop)
            obj
        },

        "defineProperties".func( "object", "descriptors") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JSObject) {
                "$obj is not a object"
            }

            val props = args.getOrNull(1) as? JSObject ?: return@func Unit

            props.keys(this).forEach { key ->
                val prop = props.get(key, this)

                typeCheck(prop is JsAny){
                    "$key property of $obj is not an object"
                }

                obj.define(this, key, prop)
            }
            obj
        },

        "getOwnPropertyDescriptor".func( "object", "property") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JSObject) {
                "$obj is not a object"
            }

            val name = args.getOrElse(1) { return@func Unit }

            obj.descriptor(name) ?: Unit
        },

        "getOwnPropertyDescriptors".func( "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JSObject) {
                "$obj is not a object"
            }

            JSObjectImpl(
                properties = obj.keys(this).mapNotNull { k ->
                    obj.descriptor(k)?.let { k to it }
                }.toMap()
            )
        },

        "getOwnPropertyNames".func( "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JsAny) { "$obj is not an object" }
            obj.keys(this)
        },

        "getPrototypeOf".func( "object") {
            val obj = it.getOrNull(0)

            typeCheck(obj is JsAny) { "$obj is not an object" }

            (obj as? JsAny)?.get(PROTO, this) ?: Unit
        },

        "preventExtensions".func( "object") {
            val obj = it.getOrNull(0)
            typeCheck(obj is JSObject) { "$obj is not an object" }
            obj.preventExtensions()
            obj
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

//    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
//        return if (args.isEmpty()) {
//            JSObjectImpl()
//        } else {
//            JSObjectImpl()
//        }
//    }
//
//
//    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
//        return invoke(args, runtime)
//    }
}

private suspend fun ScriptRuntime.constructObject(param : Any?) : JSObjectImpl {
    return when (param) {
        is Number -> JsNumberObject(JsNumberWrapper(param))
        is Wrapper<*> -> constructObject(param)
        is JSObject -> JSObjectImpl(properties = param.entries(this).associate { it[0] to it[1] })
        else -> JSObjectImpl()
    }
}

private suspend fun JSObject.define(runtime: ScriptRuntime, name : Any?, property : JsAny) {
    val p = when {
        property.contains("value", runtime) ->
            JSPropertyAccessor.Value(property.get("value", runtime))

        property.contains("get", runtime) -> JSPropertyAccessor.BackedField(
            property.get("get", runtime)?.callableOrNull(),
            property.get("set", runtime)?.callableOrNull()
        )

        else -> JSPropertyAccessor.Value(get(name, runtime))
    }

    defineOwnProperty(
        property = name,
        value = p,
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
