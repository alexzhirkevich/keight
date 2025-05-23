package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.expressions.OpEqualsImpl
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.fastMap
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
            constructObject(it.get("obj".js()))
        }
    },
    prototype = JSObjectImpl(
        properties = listOf(
            "toString".func { thisRef.toString().js() },
            "isPrototypeOf".func("object") {
                ((thisRef as? JsAny)?.isPrototypeOf(it[0],this) == true).js()
            },
            "hasOwnProperty".func("name" defaults OpArgOmitted) { args ->
                val name = args.getOrElse(0) { return@func false.js() }
                thisRef<JSObject>().hasOwnProperty(name, this).js()
            },
            "propertyIsEnumerable".func("v" defaults OpArgOmitted) {
                val name = it.argOrElse(0) { return@func false.js() }

                thisRef<JSObject>().propertyIsEnumerable(name, this).js()
            }
        ).associateBy { it.name.js() }
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
            Undefined
        },
        "create".func( "object") { args ->
            val obj = args.getOrNull(0) as? JsAny ?: return@func JSObjectImpl()
            Object {
                obj.keys(this@func).fastForEach {
                    it eq obj.get(it, this@func)
                }
            }
            Undefined
        },
        "entries".func( "object") {
            (it.firstOrNull() as? JSObject)?.entries(this)
                ?.map { it.js() }?.js()
                ?: emptyList<JsAny?>().js()
        },
        "fromEntries".func("object") { args ->
            val entries = (args.getOrNull(0) as? List<List<JsAny?>>).orEmpty()
            JSObjectImpl(properties = entries.associate { it[0] to it[1] })
        },

        "keys".func("object") {
            it.firstOrNull()?.keys(this)
                ?.fastMap { JSStringFunction.toString(it, this).js() }?.js()
                ?: emptyList<JsAny?>().js()
        },
        "values".func( "object") {
            (it.firstOrNull() as? JSObject)?.values(this)?.js()
                ?: emptyList<JsAny?>().js()
        },

        "groupBy".func( "objects", "callback") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is Iterable<*>) {
                "$obj is not iterable".js()
            }

            obj as Iterable<JsAny?>

            val callback = args.getOrNull(1)?.callableOrNull()
                ?: typeError { "${args.getOrNull(1)} is not function".js() }

            val props =  obj
                .groupBy { callback.invoke(it.listOf(), this) }
                .mapValues { it.value.js() }
                .toMutableMap()

            JSObjectImpl(properties = props)
        },

        "defineProperty".func( "object", "property", "descriptor") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JSObject) {
                "$obj is not a object".js()
            }

            val name = args.getOrElse(1) {
                return@func Undefined
            }

            val prop = args.getOrNull(2) as? JSObject ?: return@func Undefined

            obj.define(this, name, prop)
            obj
        },

        "defineProperties".func( "object", "descriptors") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JSObject) {
                "$obj is not a object".js()
            }

            val props = args.getOrNull(1) as? JSObject ?: return@func Undefined

            props.keys(this).forEach { key ->
                val prop = props.get(key, this)

                typeCheck(prop is JsAny){
                    "$key property of $obj is not an object".js()
                }

                obj.define(this, key, prop)
            }
            obj
        },

        "getOwnPropertyDescriptor".func( "object", "property") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JSObject) {
                "$obj is not a object".js()
            }

            val name = args.getOrElse(1) { return@func Undefined }

            obj.ownPropertyDescriptor(name)?.descriptor() ?: Undefined
        },

        "getOwnPropertyDescriptors".func( "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JSObject) {
                "$obj is not a object".js()
            }

            JSObjectImpl(
                properties = obj.ownPropertyDescriptors().mapValues { it.value.descriptor() }
            )
        },

        "getOwnPropertyNames".func( "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JsAny) { "$obj is not an object".js() }
            obj.keys(this).fastMap { it.toString().js() }.js()
        },

        "getPrototypeOf".func( "object") {
            val obj = it.getOrNull(0)

            typeCheck(obj is JsAny) { "$obj is not an object".js() }

            obj.get(PROTO, this) ?: Undefined
        },

        "preventExtensions".func( "object") {
            val obj = it.getOrNull(0)
            typeCheck(obj is JSObject) { "$obj is not an object".js() }
            obj.preventExtensions()
            obj
        },

        "isExtensible".func( "object") {
            val t = thisRef
            if (t !is JSObject){
                return@func false.js()
            }
            t.isExtensible.js()
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
        },
        "is".func("a","b") { args ->
            val a = args[0]
            val b = args[1]

            if (OpEqualsImpl(a,b, true, this)){
                return@func true.js()
            }

            val ka = a?.toKotlin(this)
            val kb = b?.toKotlin(this)

            (ka is Double && ka.isNaN() && kb is Double && kb.isNaN()).js()
        }
    ).associateBy { it.name.js() }.toMutableMap()
) {

    override suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
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

private suspend fun JSObject.define(runtime: ScriptRuntime, name : JsAny?, property : JsAny) {
    val p = when {
        property.contains("value".js(), runtime) ->
            JSPropertyAccessor.Value(property.get("value".js(), runtime))

        property.contains("get".js(), runtime) -> JSPropertyAccessor.BackedField(
            property.get("get".js(), runtime)?.callableOrNull(),
            property.get("set".js(), runtime)?.callableOrNull()
        )

        else -> JSPropertyAccessor.Value(get(name, runtime))
    }

    defineOwnProperty(
        property = name,
        value = p,
        runtime = runtime,
        enumerable = if (property.contains("enumerable".js(), runtime)) {
            !runtime.isFalse(property.get("enumerable".js(), runtime))
        } else null,
        configurable = if (property.contains("configurable".js(), runtime)) {
            !runtime.isFalse(property.get("configurable".js(), runtime))
        } else null,
        writable = if (property.contains("writable".js(), runtime)) {
            !runtime.isFalse(property.get("writable".js(), runtime))
        } else null,
    )
}
