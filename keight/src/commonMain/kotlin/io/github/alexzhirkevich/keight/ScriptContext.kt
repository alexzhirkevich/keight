package io.github.alexzhirkevich.keight

import kotlin.jvm.JvmInline

public val ScriptContext.comparator : Comparator<Any?>
    get() = ScriptContextComparator(this)

@JvmInline
private value class ScriptContextComparator(
    private val context: ScriptContext
) : Comparator<Any?> {
    override fun compare(a: Any?, b: Any?): Int {
        return if (context.isComparable(a, b)){
            context.compare(a,b)
        } else {
            0
        }
    }

}

public interface ScriptContext {

    public fun isFalse(a: Any?): Boolean

    /**
     * Returns true if [a] and [b] can be compared and false when
     * the comparison result for such types is always false
     **/
    public fun isComparable(a: Any?, b: Any?): Boolean

    /**
     * Should return negative value if [a] < [b],
     * positive if [a] > b and 0 if [a] equals to [b]
     *
     * Engine can call this function only in case [isComparable] result is true
     *
     * @see [Comparator]
     * */
    public fun compare(a: Any?, b: Any?): Int

    public fun sum(a: Any?, b: Any?): Any?
    public fun sub(a: Any?, b: Any?): Any?
    public fun mul(a: Any?, b: Any?): Any?
    public fun div(a: Any?, b: Any?): Any?
    public fun mod(a: Any?, b: Any?): Any?

    public fun inc(a: Any?): Any?
    public fun dec(a: Any?): Any?

    public fun neg(a: Any?): Any?
    public fun pos(a: Any?): Any?

    public fun toNumber(a: Any?, strict: Boolean = false): Number

    public fun fromKotlin(a: Any?): Any?
    public fun toKotlin(a: Any?): Any?
}