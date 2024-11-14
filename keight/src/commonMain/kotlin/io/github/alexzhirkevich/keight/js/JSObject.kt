package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastMapTo
import io.github.alexzhirkevich.keight.findRoot

public interface JSObject : JsAny {

    public val values: List<Any?>

    public val entries: List<List<Any?>>

    public fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime,
        enumerable: Boolean? = null,
        configurable: Boolean? = null,
        writable: Boolean? = null,
    )

    public fun descriptor(property: Any?) : JSPropertyDescriptor?
}

internal const val PROTOTYPE = "prototype"
internal const val PROTO = "__proto__"


internal fun JSObjectImpl.setPrototype(prototype : Any?) {
    set(PROTOTYPE, prototype, configurable = false, writable = false, enumerable = false)
}


internal fun JSObject.setPrototype(prototype : Any?, runtime: ScriptRuntime) {
    set(PROTOTYPE, prototype, runtime, configurable = false, writable = false, enumerable = false)
}

internal fun JSObject.setProto(proto : Any?, runtime: ScriptRuntime) {
    set(PROTO, proto, runtime)
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
        get() = map.filter { it.value.enumerable != false }.keys.map { it.toString() }

    override val values: List<Any?>
        get() = map.filter { it.value.enumerable != false }.values.map { it.value }

    override val entries: List<List<Any?>>
        get() = map.entries.mapNotNull { if (it.value.enumerable == false) null else listOf(it.key, it.value.value) }

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

    override suspend fun delete(property: Any?, runtime: ScriptRuntime): Boolean {
        return when {
            property !in map -> true
            map[property]?.configurable == false -> false
            else -> {
                map.remove(property)
                true
            }
        }
    }

    override fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
        set(
            property = property,
            value = value,
            enumerable = enumerable,
            configurable = configurable,
            writable = writable
        )
    }

    internal fun set(
        property: Any?,
        value: Any?,
        writable: Boolean? = null,
        configurable: Boolean? = null,
        enumerable: Boolean? = null,
    ) {
        if (property in map) {
            if (map[property]?.writable != false) {
                map[property]?.value = value
            }
            if (map[property]?.configurable != false) {
                if (writable != null) {
                    map[property]?.writable = writable
                }
                if (enumerable != null) {
                    map[property]?.enumerable = enumerable
                }
                if (configurable != null) {
                    map[property]?.configurable = configurable
                }
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

    public infix fun Any.eq(value: Any?)
    public  fun Any.eq(value: Any?, writable: Boolean? = null, configurable: Boolean? = false, enumerable: Boolean? = null)

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
    val o : JSObjectImpl = JSObjectImpl(name)
) : ObjectScope {

    override fun Any.eq(
        value: Any?,
        writable: Boolean?,
        configurable: Boolean?,
        enumerable: Boolean?
    ) {
        o.set(
            property = this,
            value = value,
            writable = writable,
            configurable = configurable,
            enumerable = enumerable
        )
    }
    override fun String.func(
        vararg args: FunctionParam,
        body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
    ) {
        val argsList = MutableList<Any?>(args.size) { }

        this eq JSFunction(
            this,
            parameters = args.toList(),
            body = Expression { ctx ->
                with(ctx) {
                    body(args.fastMapTo(argsList) { ctx.get(it.name) })
                }
            }
        )
    }

    override fun Any.eq(value: Any?) {
        o.set(this, value,)
    }
}

public fun Object(name: String, contents : Map<Any?, Any?>) : JSObject {
    return JSObjectImpl(name, contents)
}

public inline fun Object(name: String = "", builder : ObjectScope.() -> Unit) : JSObject {
    return ObjectScopeImpl(name).also(builder).o
}


internal fun JSObjectImpl.init(scope: ObjectScope.() -> Unit) : JSObjectImpl {
    ObjectScopeImpl("", this).apply(scope)
    return this
}


internal fun JSObjectImpl.noArgsFunc(
    name: String,
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction = func(
    name = name,
    args = emptyArray<FunctionParam>(),
    body = body
)

internal fun JSObjectImpl.func(
    name: String,
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction = func(
    name = name,
    args = args.map(params).toTypedArray(),
    body = body
)


internal fun JSObjectImpl.func(
    name: String,
    vararg args: FunctionParam,
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
): JSFunction {
    return JSFunction(
        name = name,
        parameters = args.toList(),
        body = Expression {
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
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
) = JSFunction(
    trimStart('_'),
    parameters = args.toList(),
    body = Expression {
        with(it) {
            body(args.map { get(it.name) })
        }
    }
)

internal fun JSObjectImpl.func(
    name: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction {

    return name.func(
        params = params,
        body = body
    ).also {
        set(name, it,)
    }
}

internal fun String.func(
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: suspend ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction = func(
    args = args.map(params).toTypedArray(),
    body = body
)