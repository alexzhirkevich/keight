package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.es.abstract.esToPropertyKey
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
            constructObject(it.get("obj".js))
        }
    },
    prototype = JsObjectImpl(
        properties = listOf(
            Constants.toString.func { thisRef.toString().js },
            "isPrototypeOf".func("object") {
                (thisRef?.isPrototypeOf(it[0],this) == true).js
            },
            "hasOwnProperty".func("name" defaults OpArgOmitted) { args ->
                val name = args.getOrElse(0) { return@func false.js }
                thisRef<JsObject>().hasOwnProperty(name, this).js
            },
            "propertyIsEnumerable".func("v" defaults OpArgOmitted) {
                val name = it.argOrElse(0) { return@func false.js }

                thisRef<JsObject>().propertyIsEnumerable(name, this).js
            }
        ).associateBy { it.name.js }
    ).apply {
        setProto(null)
    },
    properties = listOf(
        "assign".func("target", "source") {
            val target = it[0] as JsObject
            val source = it[1] as JsObject

            source.keys(this).forEach { k ->
                target.set(k, source.get(k, this), this)
            }
            Undefined
        },
        "create".func( "object") { args ->
            val obj = args.getOrNull(0) as? JsAny ?: return@func JsObjectImpl()
            Object {
                obj.keys(this@func).fastForEach {
                    it eq obj.get(it, this@func)
                }
            }
            Undefined
        },
        "entries".func( "object") {
            (it.firstOrNull() as? JsObject)?.entries(this)
                ?.map { it.js }?.js
                ?: emptyList<JsAny?>().js
        },
        "fromEntries".func("object") { args ->
            val entries = (args.getOrNull(0) as? List<List<JsAny?>>).orEmpty()
            JsObjectImpl(properties = entries.associate { it[0] to it[1] })
        },

        "keys".func("object") {
            it.firstOrNull()?.keys(this)
                ?.fastMap { toString(it).js }?.js
                ?: emptyList<JsAny?>().js
        },
        "values".func( "object") {
            (it.firstOrNull() as? JsObject)?.values(this)?.js
                ?: emptyList<JsAny?>().js
        },

        "groupBy".func( "objects", "callback") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is Iterable<*>) {
                "$obj is not iterable".js
            }

            obj as Iterable<JsAny?>

            val callback = args.getOrNull(1)?.callableOrNull()
                ?: typeError { "${args.getOrNull(1)} is not function".js }

            val props =  obj
                .groupBy { callback.invoke(it.listOf(), this) }
                .mapValues { it.value.js }
                .toMutableMap()

            JsObjectImpl(properties = props)
        },

        "defineProperty".func( "object", "property", "descriptor") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JsObject) {
                "$obj is not a object".js
            }

            val key = args.getOrElse(1) {
                return@func Undefined
            }?.esToPropertyKey(this)


            val desc = args.getOrNull(2) ?: return@func Undefined

            obj.defineOwnPropertyOrThrow(key, desc, this)
            obj
        },

        "defineProperties".func( "object", "descriptors") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JsObject) {
                "$obj is not a object".js
            }

            val props = args.getOrNull(1) as? JsObject ?: return@func Undefined

            props.keys(this).forEach { key ->
                val prop = props.get(key, this)

                typeCheck(prop is JsAny){
                    "$key property of $obj is not an object".js
                }

                obj.define(this, key, prop)
            }
            obj
        },

        "getOwnPropertyDescriptor".func( "object", "property") { args ->
            val obj = args.getOrNull(0)

            typeCheck(obj is JsObject) {
                "$obj is not a object".js
            }

            val name = args.getOrElse(1) { return@func Undefined }

            obj.ownPropertyDescriptor(name)?.descriptor() ?: Undefined
        },

        "getOwnPropertyDescriptors".func( "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JsObject) {
                "$obj is not a object".js
            }

            JsObjectImpl(
                properties = obj.ownPropertyDescriptors().mapValues { it.value.descriptor() }
            )
        },

        "getOwnPropertyNames".func( "object") { args ->
            val obj = args.getOrNull(0)
            typeCheck(obj is JsAny) { "$obj is not an object".js }
            obj.keys(this).fastMap { it.toString().js }.js
        },

        "getPrototypeOf".func( "object") {
            val obj = it.getOrNull(0)

            typeCheck(obj is JsAny) { "$obj is not an object".js }

            obj.get(PROTO, this) ?: Undefined
        },

        "preventExtensions".func( "object") {
            val obj = it.getOrNull(0)
            typeCheck(obj is JsObject) { "$obj is not an object".js }
            obj.preventExtensions()
            obj
        },

        "isExtensible".func( "object") {
            val t = thisRef
            if (t !is JsObject){
                return@func false.js
            }
            t.isExtensible.js
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
                return@func true.js
            }

            val ka = a?.toKotlin(this)
            val kb = b?.toKotlin(this)

            (ka is Double && ka.isNaN() && kb is Double && kb.isNaN()).js
        }
    ).associateBy { it.name.js }.toMutableMap()
) {

    override suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
        return obj is JsObject || super.isInstance(obj, runtime)
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

private suspend fun ScriptRuntime.constructObject(param : Any?) : JsObjectImpl {
    return when (param) {
        is Number -> JsNumberObject(JsNumberWrapper(param))
        is Wrapper<*> -> constructObject(param)
        is JsObject -> JsObjectImpl(properties = param.entries(this).associate { it[0] to it[1] })
        else -> JsObjectImpl()
    }
}

private suspend fun JsObject. define(runtime: ScriptRuntime, name : JsAny?, property : JsAny) {
    val p = when {
        property.contains(Constants.get.js, runtime) ||
                property.contains(Constants.set.js, runtime) ->
            JsPropertyAccessor.BackedField(
                property.get(Constants.get.js, runtime)?.callableOrNull(),
                property.get(Constants.set.js, runtime)?.callableOrNull()
            )

        else -> JsPropertyAccessor.Value(property.get(Constants.value.js, runtime))
    }

    val writable = when {
        property.contains(Constants.writable.js, runtime) ->
            !runtime.isFalse(property.get(Constants.writable.js, runtime))

        property.contains("set".js, runtime) -> null

        else -> false
    }

    setProperty(
        property = name,
        value = p,
        runtime = runtime,
        enumerable = if (property.contains(Constants.enumerable.js, runtime)) {
            !runtime.isFalse(property.get(Constants.enumerable.js, runtime))
        } else false,
        configurable = if (property.contains(Constants.configurable.js, runtime)) {
            !runtime.isFalse(property.get(Constants.configurable.js, runtime))
        } else false,
        writable = writable,
    )
}
