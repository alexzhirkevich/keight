import io.github.alexzhirkevich.keight.js.Undefined
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertyThisBindingTest {

    /**
     * Tests for `this` binding in property getters and setters.
     *
     * Issue: JsPropertyAccessor.BackedField.get() calls getter.invoke(emptyList(), runtime)
     * without passing thisArg, causing `this` to point to the wrong object.
     *
     * Compare with: JSFunction.call(thisArg, args, runtime) which correctly uses runtime.withScope(thisArg)
     */

    @Test
    fun getterThisBinding() = runtimeTest {
        // Basic getter should have `this` pointing to the object
        val code = """
            const obj = {
                value: 42,
                get computed() {
                    return this.value
                }
            }
            obj.computed
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun getterThisBindingInClass() = runtimeTest {
        // Class getter should have `this` pointing to the instance
        val code = """
            class Test {
                constructor(val) {
                    this.val = val
                }
                get doubled() {
                    return this.val * 2
                }
            }
            const t = new Test(21)
            t.doubled
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun methodThisBinding() = runtimeTest {
        // Regular method call - `this` should be correct
        val code = """
            const obj = {
                value: 42,
                getValue: function() {
                    return this.value
                }
            }
            obj.getValue()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun getterThisFromDifferentReference() = runtimeTest {
        // Getter accessed via different reference
        val code = """
            const obj = {
                value: 100,
                get computed() {
                    return this.value
                }
            }
            const ref = obj
            ref.value = 200
            obj.computed
        """.trimIndent()
        // Should return 200 since ref and obj point to the same object
        code.eval(it).assertEqualsTo(200L)
    }

    @Test
    fun getterThisType() = runtimeTest {
        // Type of `this` in getter should be "object"
        val code = """
            const obj = {
                get typeOfThis() {
                    return typeof this
                }
            }
            obj.typeOfThis
        """.trimIndent()
        code.eval(it).assertEqualsTo("object")
    }

    @Test
    fun setterThisBinding() = runtimeTest {
        // Setter should have `this` pointing to the object
        val code = """
            const obj = {
                storedValue: null,
                set value(v) {
                    this.storedValue = v
                },
                get stored() {
                    return this.storedValue
                }
            }
            obj.value = 123
            obj.stored
        """.trimIndent()
        code.eval(it).assertEqualsTo(123L)
    }

    @Test
    fun nestedPropertyThisBinding() = runtimeTest {
        // Nested getter - `this` should point to inner object
        val code = """
            const outer = {
                x: 10,
                inner: {
                    y: 20,
                    get sum() {
                        return this.y + 100
                    }
                }
            }
            outer.inner.sum
        """.trimIndent()
        // `this.y` should be inner.y = 20, so result is 120
        code.eval(it).assertEqualsTo(120L)
    }

    @Test
    fun getterAccessingThisProperties() = runtimeTest {
        // Multiple properties accessed via `this`
        val code = """
            const obj = {
                a: 10,
                b: 20,
                get sum() {
                    return this.a + this.b
                },
                get product() {
                    return this.a * this.b
                }
            }
            obj.sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(30L)
    }

    @Test
    fun getterReturningThis() = runtimeTest {
        // Getter returning `this`
        val code = """
            const obj = {
                value: 42,
                get self() {
                    return this
                }
            }
            obj.self === obj
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun classGetterWithStaticContext() = runtimeTest {
        // Getter accessing instance property via `this`
        val code = """
            class Counter {
                constructor() {
                    this.count = 0
                }
                get value() {
                    return this.count
                }
                increment() {
                    this.count++
                }
            }
            const c = new Counter()
            c.increment()
            c.increment()
            c.value
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun getterChainingThis() = runtimeTest {
        // Chained access to `this` properties
        val code = """
            const obj = {
                name: "test",
                get greeting() {
                    return "Hello, " + this.name
                },
                get message() {
                    return this.greeting + "!"
                }
            }
            obj.message
        """.trimIndent()
        code.eval(it).assertEqualsTo("Hello, test!")
    }

    @Test
    fun getterWithArrowInMethod() = runtimeTest {
        // Arrow function capturing `this` from enclosing method
        val code = """
            const obj = {
                value: 42,
                getByArrow: function() {
                    const getter = () => this.value
                    return getter()
                }
            }
            obj.getByArrow()
        """.trimIndent()
        // Arrow function captures `this` from getByArrow, which points to obj
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun prototypeGetterThis() = runtimeTest {
        // Getter on prototype should have `this` pointing to instance
        val code = """
            function Person(name) {
                this.name = name
            }
            Person.prototype = {
                get greet() {
                    return "Hi, " + this.name
                }
            }
            const p = new Person("Alice")
            p.greet
        """.trimIndent()
        code.eval(it).assertEqualsTo("Hi, Alice")
    }

    @Test
    fun getterWithUndefinedProperty() = runtimeTest {
        // Accessing undefined property via `this` should return undefined
        val code = """
            const obj = {
                get missing() {
                    return this.nonexistent
                }
            }
            obj.missing
        """.trimIndent()
        code.eval(it).assertEqualsTo(Undefined)
    }

    @Test
    fun setterWithThisInClass() = runtimeTest {
        // Setter in class accessing `this`
        val code = """
            class Container {
                constructor() {
                    this.history = []
                }
                set data(v) {
                    this.history.push(v)
                }
                get last() {
                    return this.history[this.history.length - 1]
                }
            }
            const c = new Container()
            c.data = 10
            c.data = 20
            c.last
        """.trimIndent()
        code.eval(it).assertEqualsTo(20L)
    }

    @Test
    fun getterReturningThisProperty() = runtimeTest {
        // Getter returning a property of `this`
        val code = """
            const obj = {
                inner: {
                    value: 999
                },
                get innerValue() {
                    return this.inner.value
                }
            }
            obj.innerValue
        """.trimIndent()
        code.eval(it).assertEqualsTo(999L)
    }
}
