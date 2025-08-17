package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck


internal val PROTOTYPE = "prototype".js
internal val PROTO = "__proto__".js
internal val CONSTRUCTOR = "constructor".js

internal fun JsObjectImpl.setPrototype(prototype : Any?) {
    (prototype as JsObjectImpl).defineOwnProperty(
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


internal suspend fun JsObject.setPrototype(prototype : JsAny?, runtime: ScriptRuntime) {
    setProperty(
        property = PROTOTYPE,
        value = JsPropertyAccessor.Value(prototype),
        runtime = runtime,
        configurable = false,
        enumerable = false
    )
}

internal suspend fun JsObject.setProto(runtime: ScriptRuntime, proto : JsAny?) {
    if (isExtensible) {
        setProperty(
            property = PROTO,
            value = JsPropertyAccessor.Value(proto),
            runtime = runtime,
            enumerable = false,
            configurable = false
        )
    }
}

internal fun JsObjectImpl.setProto(proto : JsAny?) {
    if (isExtensible) {
        defineOwnProperty(
            property = PROTO,
            value = JsPropertyAccessor.Value(proto),
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
        return key
    }
}

public open class JsObjectImpl(
    override val name : String = "Object",
    properties : Map<JsAny?, JsAny?> = mutableMapOf()
) : JsObject, Named {

    override var isExtensible: Boolean = true
        internal set

    internal val map = ObjectMap(
        properties.mapValues {
            JSPropertyImpl(
                it.value as? JsPropertyAccessor? ?: JsPropertyAccessor.Value(it.value)
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
                (!excludeSymbols || it.key !is JsSymbol) &&
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
                        v is JsSymbol -> -2L
                        else -> -3L
                    }
                }
            }
    }

    override suspend fun values(runtime: ScriptRuntime): List<JsAny?> {
        return map
            .filter { it.value.enumerable != false }
            .values
            .map { it.value.get(runtime) }
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
        return runtime.findJsRoot().Object.get(PROTOTYPE, runtime)
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when {
            property in map -> map[property]?.value?.get(runtime)
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
            "Cannot assign to read only property '$property' of object $this".js
        }

        runtime.typeCheck(isExtensible || contains(property, runtime)) {
            "Cannot add property $property, object is not extensible".js
        }

        if (!isExtensible && property == PROTO){
            return
        }

//        if (!isWritableProperty(property, runtime)){
//            return
//        }

        val current = map[property]

        if (current != null) {
            if (current.writable != false) {
                current.value.set(value, runtime)
            }
        } else {
            map[property] = JSPropertyImpl(
                value = value as? JsPropertyAccessor ?: JsPropertyAccessor.Value(value),
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
        val v = value as? JsPropertyAccessor ?: JsPropertyAccessor.Value(value)
        map[property] = JSPropertyImpl(
            value = v,
            writable = null,
            enumerable = null,
            configurable = null
        )
    }

    override suspend fun setProperty(
        property: JsAny?,
        value: JsPropertyAccessor,
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
        val v = value as? JsPropertyAccessor? ?: JsPropertyAccessor.Value(value)

        map[property] = JSPropertyImpl(
            value = v,
            writable = writable,
            enumerable = enumerable,
            configurable = configurable
        )
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


    override suspend fun ownPropertyDescriptor(property: JsAny?): JsProperty? {
        return map[property]
    }

    override suspend fun ownPropertyDescriptors(): Map<JsAny?, JsProperty> {
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

private suspend fun JsObject.isWritableProperty(property: JsAny?, runtime: ScriptRuntime) : Boolean{
    return ownPropertyDescriptor(property)?.writable == false &&
            (proto(runtime) as? JsObject)?.ownPropertyDescriptor(property)?.writable != false
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
    val o : JsObjectImpl = JsObjectImpl(name)
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

public fun Object(name: String, contents : Map<JsAny?, JsAny?>) : JsObject {
    return JsObjectImpl(name, contents)
}

public inline fun Object(name: String = "Object", builder : ObjectScope.() -> Unit) : JsObject {
    return ObjectScopeImpl(name).also(builder).o
}




internal fun JsObjectImpl.func(
    name: String,
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: suspend ScriptRuntime.(args: List<Any?>) -> JsAny?
) : JSFunction = func(
    name = name,
    args = args.map(params).toTypedArray(),
    body = body
)


internal fun JsObjectImpl.func(
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
        setOverwrite(name.js, it)
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

internal fun JsObjectImpl.func(
    name: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: ScriptRuntime.(args: List<JsAny?>) -> JsAny?
) : JSFunction {
    return name.func(
        params = params,
        body = body
    ).also {
        setOverwrite(name.js, it,)
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