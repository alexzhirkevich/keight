package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot

public interface JSObject : JsAny {

    public val values: List<Any?>

    public val entries: List<List<Any?>>

    public fun set(
        property: Any?,
        value: Any?,
        writable: Boolean? = null,
        enumerable: Boolean? = null,
        configurable: Boolean? = null
    )

    public fun descriptor(property: Any?) : JSPropertyDescriptor?
}

internal const val PROTOTYPE = "prototype"
internal const val PROTO = "__proto__"

internal fun JSObject.setPrototype(prototype : Any?) {
    set(PROTOTYPE, prototype)
}

internal fun JSObject.setProto(proto : Any?) {
    set(PROTO, proto)
}

internal suspend fun JSObject.getOrElse(property: Any?, runtime: ScriptRuntime, orElse : () -> Any?) : Any? {
    return if (contains(property, runtime)){
        get(property, runtime)
    } else {
        orElse()
    }
}

internal class ObjectMap(
    val backedMap : MutableMap<Any?, JSProperty> = mutableMapOf(),
) : MutableMap<Any?, JSProperty> by backedMap {

    override fun get(key: Any?): JSProperty? {
        return backedMap[mapKey(key)]
    }

    override fun put(key: Any?, value: JSProperty): JSProperty? {
        return backedMap.put(mapKey(key), value)
    }

    override fun containsKey(key: Any?): Boolean {
        return backedMap.containsKey(mapKey(key))
    }

    private tailrec fun mapKey(key: Any?) : Any? {
        return when (key) {
            is JsWrapper<*> -> mapKey(key.value)
            else -> key
        }
    }
}

public open class JSObjectImpl(
    public open val name : String = "",
    properties : Map<Any?, Any?> = mutableMapOf()
) : JSObject {

    private val map = ObjectMap(
        properties.mapValues { JSProperty(it.value) }.toMutableMap()
    )

    protected val properties : Map<Any?, Any?>
        get() = map.backedMap

    override val keys: List<String>
        get() = map.keys.map { it.toString() }

    override val values: List<Any?> get() = map.values.map { it.value }

    override val entries: List<List<Any?>>
        get() = map.entries.map { listOf(it.key, it.value.value) }

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return if (PROTO in map) {
            map[PROTO]?.value
        } else {
            (runtime.findRoot() as JSRuntime).Object.get(PROTOTYPE,runtime)
        }
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        if (property in map) {
            return map[property]?.value
        }
        return super.get(property, runtime)
    }

    override suspend fun delete(property: Any?, runtime: ScriptRuntime) {
        if (property in map) {
            map.remove(property)
        } else {
            super.delete(property, runtime)
        }
    }

    override fun set(
        property: Any?,
        value: Any?,
        writable: Boolean?,
        enumerable: Boolean?,
        configurable: Boolean?
    ) {
        if (property in map) {
            map[property]?.value = value
            if (writable != null) {
                map[property]?.writable = writable
            }
            if (enumerable != null) {
                map[property]?.writable = enumerable
            }
            if (configurable != null) {
                map[property]?.writable = configurable
            }
        } else {
            map[property] = JSProperty(
                value = value,
                writable = writable,
                enumerable = enumerable,
                configurable = configurable
            )
        }
    }

    override fun descriptor(property: Any?): JSPropertyDescriptor? {
        return map[property]
    }

    override suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean =
        property in map || super.contains(property, runtime)

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
    return JSObjectImpl(name, contents)
}

public inline fun Object(name: String = "", builder : ObjectScope.() -> Unit) : JSObject {
    return ObjectScopeImpl(name).also(builder).o
}


internal fun JSObject.init(scope: ObjectScope.() -> Unit) {
    ObjectScopeImpl("", this).apply(scope)
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
        set(name, it)
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
        set(name, it)
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