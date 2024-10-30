package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

public interface JSObject : JsAny {

    public val values: List<Any?>

    public val entries: List<List<Any?>>

    public operator fun set(property: Any?, value: Any?)

    public fun delete(property: Any?)
}

internal const val PROTOTYPE = "prototype"
internal const val PROTO = "__proto__"

internal fun JSObject.setPrototype(prototype : Any?) {
    this[PROTOTYPE] = prototype
}

internal fun JSObject.setProto(proto : Any?) {
    this[PROTO] = proto
}

internal class ObjectMap(
    val backedMap : MutableMap<Any?, Any?> = mutableMapOf(),
) : MutableMap<Any?, Any?> by backedMap {

    override fun get(key: Any?): Any? {
        return backedMap[mapKey(key)]
    }

    override fun put(key: Any?, value: Any?): Any? {
        return backedMap.put(mapKey(key), value)
    }

    override fun containsKey(key: Any?): Boolean {
        return backedMap.containsKey(mapKey(key))
    }

    private fun mapKey(key: Any?) : Any? {
        return when (key) {
            is JsWrapper<*> -> key.value
            else -> key
        }
    }
}

internal open class JSObjectImpl(
    open val name : String = "",
    map : MutableMap<Any?, Any?> = mutableMapOf()
) : JSObject {

    internal val map = ObjectMap(map)

    override val keys: List<String>
        get() = map.keys.map { it.toString() }

    override val values: List<Any?> get() = map.values.toList()

    override val entries: List<List<Any?>>
        get() = map.entries.map { listOf(it.key, it.value) }

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return map[PROTO]
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        if (property in map) {
            return map[property]
        }
        return super.get(property, runtime)
    }

    override fun delete(property: Any?) {
        map.remove(property)
    }

    override fun set(property: Any?, value: Any?) {
        map[property] = value
    }

    override suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean =
        property in map

    override fun toString(): String {
        return if (name.isNotBlank()){
            "[object $name]"
        } else {
            "[object]"
        }
    }
}


public sealed interface ObjectScope {

    public infix fun String.eq(value: Any?)

    public fun String.func(
        vararg args: FunctionParam,
        body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
    )

    public fun String.func(
        vararg args: String,
        params: (String) -> FunctionParam = { FunctionParam(it) },
        body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
    ) {
        func(
            args = args.map(params).toTypedArray(),
            body = body
        )
    }
}

@PublishedApi
internal class ObjectScopeImpl(
    name: String,
    val o : JSObject = JSObjectImpl(name)
) : ObjectScope {

    override fun String.func(
        vararg args: FunctionParam,
        body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
    ) {
        this eq JSFunction(
            this,
            parameters = args.toList(),
            body = { ctx ->
                with(ctx) {
                    body(args.map { ctx.get(it.name) })
                }
            }
        )
    }

    override fun String.eq(value: Any?) {
        o.set(this, value)
    }
}

public fun Object(name: String, contents : Map<Any?, Any?>) : JSObject {
    return JSObjectImpl(name, contents.toMutableMap())
}

public inline fun Object(name: String = "", builder : ObjectScope.() -> Unit) : JSObject {
    return ObjectScopeImpl(name).also(builder).o
}


internal fun JSObject.init(scope: ObjectScope.() -> Unit) {
    ObjectScopeImpl("", this).apply(scope)
}

internal interface SuspendReadOnlyProperty<out V> {
    suspend fun getValue(): V
}


internal fun JSObject.noArgsFunc(
    name: String,
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction = func(
    name = name,
    args = emptyArray<FunctionParam>(),
    body = body
)

internal fun JSObject.func(
    name: String,
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction = func(
    name = name,
    args = args.map(params).toTypedArray(),
    body = body
)


internal fun JSObject.func(
    name: String,
    vararg args: FunctionParam,
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
): JSFunction {
    return JSFunction(
        name = name,
        parameters = args.toList(),
        body = {
            with(it) {
                body(args.map { get(it.name) })
            }
        }
    ).also {
        this[name] = it
    }
}


internal fun String.func(
    vararg args: FunctionParam,
    body: ScriptRuntime.(args: List<Any?>) -> Any?
) = JSFunction(
    trimStart('_'),
    parameters = args.toList(),
    body = {
        with(it) {
            body(args.map { get(it.name) })
        }
    }
)

internal fun JSObject.func(
    name: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction {

    return name.func(
        params = params,
        body = body
    ).also {
        this[name] = it
    }
}

internal fun String.func(
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction = func(
    args = args.map(params).toTypedArray(),
    body = body
)