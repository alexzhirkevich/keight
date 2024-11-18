package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.fastMapTo
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError


public interface JSObject : JsAny {

    public val isExtensible : Boolean  get() = true

    public fun preventExtensions() {}

    public suspend fun values(runtime: ScriptRuntime) :List<Any?> = emptyList()

    public suspend fun entries(runtime: ScriptRuntime) : List<List<Any?>> = emptyList()

    public suspend fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime,
    )

    public suspend fun defineOwnProperty(
        property: Any?,
        value: JSPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean? = null,
        configurable: Boolean? = null,
        writable: Boolean? = null,
    ) {
        set(property, value.get(runtime), runtime)
    }

    public fun descriptor(property: Any?) : JSPropertyDescriptor?
}

internal const val PROTOTYPE = "prototype"
internal const val PROTO = "__proto__"

internal fun JSObjectImpl.setPrototype(prototype : Any?) {
    defineOwnProperty(
        property = PROTOTYPE,
        value = prototype,
        configurable = false,
        enumerable = false
    )
}


internal suspend fun JSObject.setPrototype(prototype : Any?, runtime: ScriptRuntime) {
    defineOwnProperty(
        property = PROTOTYPE,
        value = JSPropertyAccessor.Value(prototype),
        runtime = runtime,
        configurable = false,
        enumerable = false
    )
}

internal suspend fun JSObject.setProto(runtime: ScriptRuntime, proto : Any?) {
    if (isExtensible) {
        defineOwnProperty(
            property = PROTO,
            value = JSPropertyAccessor.Value(proto),
            runtime = runtime,
            enumerable = false,
            configurable = false
        )
    }
}

internal fun JSObjectImpl.setProto(proto : Any?) {
    if (isExtensible) {
        defineOwnProperty(PROTO, JSPropertyAccessor.Value(proto), enumerable = false, configurable = false)
    }
}


internal suspend fun JSObject.getOrElse(property: Any?, runtime: ScriptRuntime, orElse : () -> Any?) : Any? {
    return if (contains(property, runtime)){
        get(property, runtime)
    } else {
        orElse()
    }
}

internal class ObjectMap<V>(
    val backedMap : MutableMap<Any?, V> = mutableMapOf(),
) : MutableMap<Any?, V> by backedMap {

    override fun get(key: Any?): V? {
        return backedMap[mapKey(key)]
    }

    override fun put(key: Any?, value: V): V? {
        return backedMap.put(mapKey(key), value)
    }

    override fun containsKey(key: Any?): Boolean {
        return backedMap.containsKey(mapKey(key))
    }

    private tailrec fun mapKey(key: Any?) : Any? {
        return when (key) {
            is Wrapper<*> -> mapKey(key.value)
            else -> key
        }
    }
}

public open class JSObjectImpl(
    public open val name : String = "Object",
    properties : Map<Any?, Any?> = mutableMapOf()
) : JSObject {

    override var isExtensible : Boolean = true
        internal set

    private val map = ObjectMap(
        properties.mapValues {
            JSProperty(
                it.value as? JSPropertyAccessor? ?: JSPropertyAccessor.Value(it.value)
            )
        }.toMutableMap()
    )

    protected val properties : Map<Any?, Any?>
        get() = map.backedMap

    override suspend fun keys(runtime: ScriptRuntime): List<String> {
        return map.filter { it.value.enumerable != false }.keys.map { it.toString() }
    }


    override suspend fun values(runtime: ScriptRuntime): List<Any?> {
        return map.filter { it.value.enumerable != false }.values.map { it.value?.get(runtime) }
    }

    override suspend fun entries(runtime: ScriptRuntime): List<List<Any?>> {
        return map.entries
            .mapNotNull {
                if (it.value.enumerable == false) null else listOf(
                    it.key,
                    it.value.value.get(runtime)
                )
            }
    }

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return if (PROTO in map) {
            get(PROTO, runtime)
        } else {
            (runtime.findRoot() as JSRuntime).Object.get(PROTOTYPE,runtime)
        }
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when {
            property in map -> map[property]?.value?.get(runtime)?.get(runtime)
            else -> super.get(property, runtime)
        }
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

    override suspend fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime,
    ) {
        runtime.typeCheck(map[property]?.writable != false || !runtime.isStrict) {
            "Cannot assign to read only property '$property' of object $this"
        }

        runtime.typeCheck(isExtensible || contains(property, runtime)) {
            "Cannot add property $property, object is not extensible"
        }

        val current = map[property]

        if (current != null) {
            if (current.writable != false) {
                current.value.set(value, runtime)
            }
        } else {
            map[property] = JSProperty(
                value = value as? JSPropertyAccessor ?: JSPropertyAccessor.Value(value),
                writable = null,
                enumerable = null,
                configurable = null
            )
        }
    }

    internal fun set(
        property: Any?,
        value: Any?,
    ) {
        val v = value as? JSPropertyAccessor ?: JSPropertyAccessor.Value(value)
        map[property] = JSProperty(
            value = v,
            writable = null,
            enumerable = null,
            configurable = null
        )
    }

    override suspend fun defineOwnProperty(
        property: Any?,
        value: JSPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {

        if (map[property]?.writable == false && runtime.isStrict){
            throw TypeError("Cannot assign to read only property '$property' of object $this")
        }

        defineOwnProperty(
            property = property,
            value = value,
            enumerable = enumerable,
            configurable = configurable,
            writable = writable
        )
    }
    internal fun defineOwnProperty(
        property: Any?,
        value: Any?,
        enumerable: Boolean? = null,
        configurable: Boolean? = null,
        writable: Boolean? = null
    ) {
        val v = value as? JSPropertyAccessor? ?: JSPropertyAccessor.Value(value)

        if (property in map) {
            if (map[property]?.writable != false ) {
                map[property]?.value = v
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
                value = v,
                writable = writable,
                enumerable = enumerable,
                configurable = configurable
            )
        }
    }


    override suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean =
        property in map || super.contains(property, runtime)

    override fun descriptor(property: Any?): JSPropertyDescriptor? {
        return map[property]
    }

    override fun preventExtensions() {
        isExtensible = false
    }

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
        o.defineOwnProperty(
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

public inline fun Object(name: String = "Object", builder : ObjectScope.() -> Unit) : JSObject {
    return ObjectScopeImpl(name).also(builder).o
}


//internal fun JSObjectImpl.init(scope: ObjectScope.() -> Unit) : JSObjectImpl {
//    ObjectScopeImpl("", this).apply(scope)
//    return this
//}


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