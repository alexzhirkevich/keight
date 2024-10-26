package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

public interface JSObject : JsAny {

    public val keys: Set<String>

    public val values: List<Any?>

    public val entries: List<List<Any?>>

    public operator fun set(variable: Any?, value: Any?)
}

private const val PROTOTYPE = "prototype"

internal val JSObject.prototype : Any? get() = get(PROTOTYPE)

internal fun JSObject.setPrototype(prototype : Any?) {
    this[PROTOTYPE] = prototype
}

private class ObjectMap(
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

    private val map = ObjectMap(map)

    override val keys: Set<String>
        get() = map.keys.map { it.toString() }.toSet()

    override val values: List<Any?> get() = map.values.toList()

    override val entries: List<List<Any?>>
        get() = map.entries.map { listOf(it.key, it.value) }

    override fun get(variable: Any?): Any? {
        return if (contains(variable))
            map[variable]
        else
            super.get(variable)
    }
    override fun set(variable: Any?, value: Any?) {
        map[variable] = value
    }

    override fun contains(variable: Any?): Boolean = variable in map

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
        body: ScriptRuntime.(args: List<Any?>) -> Any?
    )

    public fun String.func(
        vararg args: String,
        params: (String) -> FunctionParam = { FunctionParam(it) },
        body: ScriptRuntime.(args: List<Any?>) -> Any?
    ) {
        func(
            args = args.map(params).toTypedArray(),
            body = body
        )
    }
}

private class ObjectScopeImpl(
    name: String,
    val o : JSObject = JSObjectImpl(name)
) : ObjectScope {

    override fun String.func(
        vararg args: FunctionParam,
        body: ScriptRuntime.(args: List<Any?>) -> Any?
    ) {
        this eq JSFunction(
            this,
            parameters = args.toList(),
            body = { ctx ->
                with(ctx) {
                    body(args.map { ctx[it.name] })
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

public fun  Object(name: String, builder : ObjectScope.() -> Unit) : JSObject {
    return ObjectScopeImpl(name).also(builder).o
}


internal fun JSObject.init(scope: ObjectScope.() -> Unit) {
    ObjectScopeImpl("", this).apply(scope)
}

internal fun func(
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: ScriptRuntime.(args: List<Any?>) -> Any?
) : PropertyDelegateProvider<JSObject, ReadOnlyProperty<JSObject, JSFunction>> =  func(
    args = args.map(params).toTypedArray(),
    body = body
)

internal fun func(
    vararg args: FunctionParam,
    body: ScriptRuntime.(args: List<Any?>) -> Any?
): PropertyDelegateProvider<JSObject, ReadOnlyProperty<JSObject, JSFunction>> =  PropertyDelegateProvider { obj, prop ->
    val name = prop.name.trimStart('_')
    obj[name] = JSFunction(
        name = name,
        parameters = args.toList(),
        body = {
            with(it) {
                body(args.map { get(it.name) })
            }
        }
    )

    ReadOnlyProperty { thisRef, property ->
        thisRef[property.name] as JSFunction
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


internal fun  String.func(
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: ScriptRuntime.(args: List<Any?>) -> Any?
) : JSFunction = func(
    args = args.map(params).toTypedArray(),
    body = body
)