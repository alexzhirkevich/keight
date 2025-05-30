package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.runBlocking
import javax.script.Bindings

internal class KeightBindings(
    private val runtime: ScriptRuntime
) : Bindings {

    override fun clear() {
        runtime.reset()
    }

    override fun put(key: String?, value: Any?): Any? {
        runBlocking { runtime.set(key?.js(), fromKotlin(value), VariableType.Local) }
        return value
    }

    override fun putAll(from: Map<out String, Any>) {
        from.forEach { (k, v) -> put(k, v) }
    }

    override fun remove(k: String?): Any? {
        return runBlocking { runtime.delete(k?.js()) }
    }

    override fun containsKey(k: String?): Boolean {
        return runBlocking { runtime.contains(k?.js()) }
    }

    override fun containsValue(value: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(p0: String?): Any? {
        return runBlocking { runtime.get(p0?.js()) }
    }

    override fun isEmpty(): Boolean {
        return false
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() = TODO("Not yet implemented")

    override val keys: MutableSet<String>
        get() = TODO("Not yet implemented")

    override val values: MutableCollection<Any>
        get() = TODO("Not yet implemented")

    override val size: Int
        get() = TODO("Not yet implemented")
}