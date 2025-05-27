package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck


public interface JSObject : JsAny {

    public val isExtensible : Boolean  get() = true

    public fun preventExtensions() {}

    public suspend fun values(runtime: ScriptRuntime): List<JsAny?> = emptyList()

    public suspend fun entries(runtime: ScriptRuntime): List<List<JsAny?>> = emptyList()

    public suspend fun set(
        property: JsAny?,
        value: JsAny?,
        runtime: ScriptRuntime,
    )

    public suspend fun defineOwnProperty(
        property: JsAny?,
        value: JSPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean? = null,
        configurable: Boolean? = null,
        writable: Boolean? = null,
    ) {
        set(property, value.get(runtime), runtime)
    }

    public fun ownPropertyDescriptor(property: JsAny?) : JSProperty? = null
    public fun ownPropertyDescriptors() : Map<JsAny?, JSProperty> = emptyMap()

    public suspend fun propertyIsEnumerable(name : JsAny?, runtime: ScriptRuntime) : Boolean {
        return true
    }

    public suspend fun hasOwnProperty(name : JsAny?, runtime: ScriptRuntime) : Boolean {
        return (proto(runtime) as? JSObject?)?.hasOwnProperty(name, runtime) == true
    }
}

internal val PROTOTYPE = "prototype".js()
internal val PROTO = "__proto__".js()
internal val CONSTRUCTOR = "constructor".js()

internal fun JSObjectImpl.setPrototype(prototype : Any?) {
    (prototype as JSObjectImpl).defineOwnProperty(
        property = CONSTRUCTOR,
        value = this,
        enumerable = false,
        configurable = false,
        writable = false
    )
    defineOwnProperty(
        property = PROTOTYPE,
        value = prototype,
        configurable = false,
        enumerable = false
    )
}


internal suspend fun JSObject.setPrototype(prototype : JsAny?, runtime: ScriptRuntime) {
    defineOwnProperty(
        property = PROTOTYPE,
        value = JSPropertyAccessor.Value(prototype),
        runtime = runtime,
        configurable = false,
        enumerable = false
    )
}

internal suspend fun JSObject.setProto(runtime: ScriptRuntime, proto : JsAny?) {
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

internal fun JSObjectImpl.setProto(proto : JsAny?) {
    if (isExtensible) {
        defineOwnProperty(
            property = PROTO,
            value = JSPropertyAccessor.Value(proto),
            enumerable = false,
            configurable = false
        )
    }
}

internal class ObjectMap<V>(
    val backedMap : MutableMap<JsAny?, V> = mutableMapOf(),
) : MutableMap<JsAny?, V> by backedMap {

    override fun remove(key: JsAny?): V? {
        return backedMap.remove(mapKey(key))
    }

    override fun get(key: JsAny?): V? {
        return backedMap[mapKey(key)]
    }

    override fun put(key: JsAny?, value: V): V? {
        return backedMap.put(mapKey(key), value)
    }

    override fun containsKey(key: JsAny?): Boolean {
        return backedMap.containsKey(mapKey(key))
    }

    private fun mapKey(key: JsAny?) : JsAny? {
        return when (key) {
//            is Wrapper<*> -> key.unwrap()
            else -> key
        }
    }
}

public open class JSObjectImpl(
    override val name : String = "Object",
    properties : Map<JsAny?, JsAny?> = mutableMapOf()
) : JSObject, Named {

    override var isExtensible: Boolean = true
        internal set

    internal val map = ObjectMap(
        properties.mapValues {
            JSPropertyImpl(
                it.value as? JSPropertyAccessor? ?: JSPropertyAccessor.Value(it.value)
            )
        }.toMutableMap()
    )

    protected val properties: Map<JsAny?, Any?>
        get() = map.backedMap

    public override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> {
        return map
            .filter {
                (!excludeSymbols || it.key !is JSSymbol) &&
                        (!excludeNonEnumerables || it.value.enumerable != false)
            }
            .keys
            .groupBy {
                val v = it?.toKotlin(runtime)
                v is Long && v > 0
            }
            .let {
                mapOf(
                    true to it[true].orEmpty(),
                    false to it[false].orEmpty()
                )
            }
            .flatMap {
                it.value.sortedByDescending { v ->
                    when {
                        v is JsNumberWrapper && v.value.toLong() > 0 -> v.value.toLong()
                        v is JsNumberObject && v.value.toLong() > 0 -> v.value.toLong()
                        v is JsStringWrapper || v is JsStringObject -> -1L
                        v is JSSymbol -> -2L
                        else -> -3L
                    }
                }
            }
    }

    override suspend fun values(runtime: ScriptRuntime): List<JsAny?> {
        return map
            .filter { it.value.enumerable != false }
            .values.map { it.value.get(runtime) }
    }

    override suspend fun entries(runtime: ScriptRuntime): List<List<JsAny?>> {
        return map.entries
            .mapNotNull {
                if (it.value.enumerable == false) null else listOf(
                    it.key,
                    it.value.value.get(runtime)
                )
            }
    }

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return if (PROTO in map) {
            get(PROTO, runtime)
        } else {
           fallbackProto(runtime)
        }
    }

    internal open suspend fun fallbackProto(runtime: ScriptRuntime) : JsAny? {
        return (runtime.findRoot() as JSRuntime).Object.get(PROTOTYPE, runtime)
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when {
            property in map -> map[property]?.value?.get(runtime)?.get(runtime)
            else -> super.get(property, runtime)
        }
    }


    override suspend fun delete(property: JsAny?, runtime: ScriptRuntime): Boolean {
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
        property: JsAny?,
        value: JsAny?,
        runtime: ScriptRuntime,
    ) {

        runtime.typeCheck(map[property]?.writable != false || !runtime.isStrict) {
            "Cannot assign to read only property '$property' of object $this".js()
        }

        runtime.typeCheck(isExtensible || contains(property, runtime)) {
            "Cannot add property $property, object is not extensible".js()
        }

        if (!isExtensible && property == PROTO){
            return
        }

        val current = map[property]

        if (current != null) {
            if (current.writable != false) {
                current.value.set(value, runtime)
            }
        } else {
            map[property] = JSPropertyImpl(
                value = value as? JSPropertyAccessor ?: JSPropertyAccessor.Value(value),
                writable = null,
                enumerable = null,
                configurable = null
            )
        }
    }

    internal fun setOverwrite(
        property: JsAny?,
        value: JsAny?,
    ) {
        val v = value as? JSPropertyAccessor ?: JSPropertyAccessor.Value(value)
        map[property] = JSPropertyImpl(
            value = v,
            writable = null,
            enumerable = null,
            configurable = null
        )
    }

    override suspend fun defineOwnProperty(
        property: JsAny?,
        value: JSPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {

        if (map[property]?.writable == false && runtime.isStrict) {
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
        property: JsAny?,
        value: JsAny?,
        enumerable: Boolean? = null,
        configurable: Boolean? = null,
        writable: Boolean? = null
    ) {
        val v = value as? JSPropertyAccessor? ?: JSPropertyAccessor.Value(value)

        if (property in map) {
            map[property]?.value = v
            if (configurable != null) {
                map[property]?.configurable = configurable
            }

//            if (map[property]?.configurable != false) {
                if (writable != null) {
                    map[property]?.writable = writable
                }
                if (enumerable != null) {
                    map[property]?.enumerable = enumerable
                }
//            }
        } else {
            map[property] = JSPropertyImpl(
                value = v,
                writable = writable,
                enumerable = enumerable,
                configurable = configurable
            )
        }
    }

    override suspend fun hasOwnProperty(name: JsAny?, runtime: ScriptRuntime): Boolean {
        return name in map
    }

    override suspend fun propertyIsEnumerable(name: JsAny?, runtime: ScriptRuntime): Boolean {
        val p = map[name] ?: return false
        return p.enumerable != false
    }

    override suspend fun contains(property: JsAny?, runtime: ScriptRuntime): Boolean {
        val prop = map[property] ?: return super.contains(property, runtime)
        return prop.enumerable != false
    }


    override fun ownPropertyDescriptor(property: JsAny?): JSProperty? {
        return map[property]
    }

    override fun ownPropertyDescriptors(): Map<JsAny?, JSProperty> {
        return map
    }

    override fun preventExtensions() {
        isExtensible = false
    }

    override fun toString(): String {
        return if (name.isNotBlank()) {
            "[object $name]"
        } else {
            "[object]"
        }
    }
}

public sealed interface ObjectScope {

    public infix fun JsAny?.eq(value: JsAny?)

    public fun JsAny?.eq(value: JsAny?, writable: Boolean? = null, configurable: Boolean? = false, enumerable: Boolean? = null)

    public fun JsAny?.func(
        vararg args: FunctionParam,
        body: suspend ScriptRuntime.(args: List<JsAny?>) -> JsAny?
    )

    public fun JsAny?.func(
        vararg args: String,
        params: (String) -> FunctionParam = { FunctionParam(it) },
        body: suspend ScriptRuntime.(args: List<JsAny?>) -> JsAny?
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

    override fun JsAny?.eq(
        value: JsAny?,
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
    override fun JsAny?.func(
        vararg args: FunctionParam,
        body: suspend ScriptRuntime.(args: List<JsAny?>) -> JsAny?
    ) {

        this eq JSFunction(
            this.toString(),
            parameters = args.toList(),
            body = Expression { ctx ->
                with(ctx) {
                    body(args.map { it.get(ctx) })
                }
            }
        )
    }

    override fun JsAny?.eq(value: JsAny?) {
        o.setOverwrite(this, value,)
    }
}

public fun Object(name: String, contents : Map<JsAny?, JsAny?>) : JSObject {
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
    body: suspend ScriptRuntime.(args: List<Any?>) -> JsAny?
) : JSFunction = func(
    name = name,
    args = emptyArray<FunctionParam>(),
    body = body
)

internal fun JSObjectImpl.func(
    name: String,
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: suspend ScriptRuntime.(args: List<Any?>) -> JsAny?
) : JSFunction = func(
    name = name,
    args = args.map(params).toTypedArray(),
    body = body
)


internal fun JSObjectImpl.func(
    name: String,
    vararg args: FunctionParam,
    body: suspend ScriptRuntime.(args: List<Any?>) -> JsAny?
): JSFunction {
    return JSFunction(
        name = name,
        parameters = args.toList(),
        body = Expression { r ->
            with(r) {
                body(args.map { it.get(r) })
            }
        }
    ).also {
        setOverwrite(name.js(), it)
    }
}


internal fun String.func(
    vararg args: FunctionParam,
    body: suspend ScriptRuntime.(args: List<JsAny?>) -> JsAny?
) = JSFunction(
    name = this,
    parameters = args,
    body = body
)

internal fun JSObjectImpl.func(
    name: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: ScriptRuntime.(args: List<JsAny?>) -> JsAny?
) : JSFunction {
    return name.func(
        params = params,
        body = body
    ).also {
        setOverwrite(name.js(), it,)
    }
}

internal fun String.func(
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: suspend ScriptRuntime.(args: List<JsAny?>) -> JsAny?
) : JSFunction = func(
    args = args.map(params).toTypedArray(),
    body = body
)