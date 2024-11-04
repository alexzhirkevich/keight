package io.github.alexzhirkevich.keight.js

public interface JSPropertyDescriptor {
    public val value : Any?
    public val writable : Boolean?
    public val enumerable : Boolean?
    public val configurable : Boolean?
}

internal class JSProperty(
    override var value : Any?,
    override var writable : Boolean? = null,
    override var enumerable : Boolean? = null,
    override var configurable : Boolean? = null,
) : JSPropertyDescriptor

internal fun JSPropertyDescriptor.asObject() : JSObject {
    return Object {
        "value" eq value
        "writable" eq (writable ?: true)
        "configurable" eq (configurable ?: true)
        "enumerable" eq (enumerable ?: true)
    }
}