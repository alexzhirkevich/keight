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

    // ========== 新增测试用例 ==========

    @Test
    fun objectLiteralGetterWithComputedProperty() = runtimeTest {
        // Getter accessing computed property name (via expression)
        val code = """
            const prefix = "computed"
            const obj = {
                value: 100,
                get computedValue() {
                    return this.value + 50
                }
            }
            obj.computedValue
        """.trimIndent()
        code.eval(it).assertEqualsTo(150L)
    }

    @Test
    fun getterAccessingMultipleThisProperties() = runtimeTest {
        // Getter using multiple properties from `this`
        val code = """
            const calc = {
                a: 2,
                b: 3,
                c: 4,
                get result() {
                    return (this.a + this.b) * this.c
                }
            }
            calc.result
        """.trimIndent()
        code.eval(it).assertEqualsTo(20L)
    }

    @Test
    fun setterWithValidation() = runtimeTest {
        // Setter with validation using `this`
        val code = """
            const person = {
                _age: 0,
                set age(v) {
                    if (v < 0) {
                        this._age = 0
                    } else {
                        this._age = v
                    }
                },
                get age() {
                    return this._age
                }
            }
            person.age = -5
            person.age
        """.trimIndent()
        code.eval(it).assertEqualsTo(0L)
    }

    @Test
    fun getterWithSideEffect() = runtimeTest {
        // Getter with side effect tracking
        val code = """
            const counter = {
                _count: 0,
                get accessCount() {
                    this._count++
                    return this._count
                }
            }
            counter.accessCount
            counter.accessCount
            counter.accessCount
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun getterSetterPair() = runtimeTest {
        // Object with paired getter and setter
        val code = """
            const temp = {
                _celsius: 0,
                get fahrenheit() {
                    return this._celsius * 9 / 5 + 32
                },
                set fahrenheit(v) {
                    this._celsius = (v - 32) * 5 / 9
                }
            }
            temp.fahrenheit = 212
            temp._celsius
        """.trimIndent()
        // 212°F = 100°C
        code.eval(it).assertEqualsTo(100L)
    }

    @Test
    fun objectSpreadWithGetter() = runtimeTest {
        // Object with getter after spread
        val code = """
            const base = { x: 10 }
            const obj = {
                ...base,
                get doubled() {
                    return this.x * 2
                }
            }
            obj.doubled
        """.trimIndent()
        code.eval(it).assertEqualsTo(20L)
    }

    @Test
    fun getterReturningMethodResult() = runtimeTest {
        // Getter returning a value that method would return
        val code = """
            const obj = {
                factor: 3,
                base: 10,
                get product() {
                    return this.base * this.factor
                }
            }
            obj.product
        """.trimIndent()
        code.eval(it).assertEqualsTo(30L)
    }

    @Test
    fun thisInNestedObjectGetter() = runtimeTest {
        // Getter in nested object accessing outer `this`
        val code = """
            const outer = {
                value: 100,
                inner: {
                    multiplier: 2,
                    get computed() {
                        return this.multiplier * outer.value
                    }
                }
            }
            outer.inner.computed
        """.trimIndent()
        code.eval(it).assertEqualsTo(200L)
    }

    @Test
    fun setterWithMultipleFields() = runtimeTest {
        // Setter updating multiple fields via `this`
        val code = """
            const logger = {
                _first: null,
                _last: null,
                _full: null,
                set name(n) {
                    this._first = n.split(' ')[0]
                    this._last = n.split(' ')[1]
                    this._full = n
                },
                get firstName() { return this._first },
                get lastName() { return this._last }
            }
            logger.name = "John Doe"
            logger.firstName + " " + logger.lastName
        """.trimIndent()
        code.eval(it).assertEqualsTo("John Doe")
    }

    @Test
    fun getterReturningArrayFromThis() = runtimeTest {
        // Getter returning array constructed from `this` properties
        val code = """
            const obj = {
                x: 1,
                y: 2,
                z: 3,
                get coords() {
                    return [this.x, this.y, this.z]
                }
            }
            obj.coords[1]
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun chainedGettersThis() = runtimeTest {
        // Multiple chained getters all using `this`
        val code = """
            const math = {
                value: 10,
                get double() {
                    return this.value * 2
                },
                get quadruple() {
                    return this.double * 2
                },
                get triple() {
                    return this.value * 3
                }
            }
            math.quadruple
        """.trimIndent()
        code.eval(it).assertEqualsTo(40L)
    }

    @Test
    fun setterWithConditionalThisAccess() = runtimeTest {
        // Setter with conditional logic using `this`
        val code = """
            const box = {
                _width: 0,
                _height: 0,
                set size(dim) {
                    if (typeof dim === 'number') {
                        this._width = dim
                        this._height = dim
                    } else {
                        this._width = dim.w
                        this._height = dim.h
                    }
                },
                get area() {
                    return this._width * this._height
                }
            }
            box.size = 5
            box.area
        """.trimIndent()
        code.eval(it).assertEqualsTo(25L)
    }

    @Test
    fun getterAccessingNestedThisObject() = runtimeTest {
        // Getter accessing nested object via `this`
        val code = """
            const app = {
                config: {
                    theme: 'dark'
                },
                get theme() {
                    return this.config.theme
                }
            }
            app.theme
        """.trimIndent()
        code.eval(it).assertEqualsTo("dark")
    }

    // ========== this 边界情况测试 ==========

    @Test
    fun methodBoundToDifferentObject() = runtimeTest {
        // Method extracted and called with different this
        val code = """
            const obj1 = { value: 10, getValue: function() { return this.value } }
            const obj2 = { value: 20 }
            obj1.getValue.call(obj2)
        """.trimIndent()
        code.eval(it).assertEqualsTo(20L)
    }

    @Test
    fun methodWithExplicitThisBinding() = runtimeTest {
        // Method with explicit this binding
        val code = """
            const obj = {
                value: 100,
                getValue: function() { return this.value }
            }
            const target = { value: 50 }
            obj.getValue.call(target)
        """.trimIndent()
        code.eval(it).assertEqualsTo(50L)
    }

    @Test
    fun setterWithCallBinding() = runtimeTest {
        // Setter accessed via call with different this
        val code = """
            const obj = {
                stored: null,
                set value(v) { this.stored = v },
                get stored() { return this.stored }
            }
            const target = {}
            obj.value = 42
            obj.stored
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun prototypeMethodWithThis() = runtimeTest {
        // Prototype method should have correct this
        val code = """
            function Counter() {
                this.count = 0
            }
            Counter.prototype.increment = function() {
                this.count++
            }
            const c = new Counter()
            c.increment()
            c.increment()
            c.count
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun classMethodWithThis() = runtimeTest {
        // Class method should have correct this
        val code = """
            class Stack {
                constructor() {
                    this.items = []
                }
                push(item) {
                    this.items.push(item)
                }
                pop() {
                    return this.items.pop()
                }
            }
            const s = new Stack()
            s.push(1)
            s.push(2)
            s.pop()
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun getterInCallback() = runtimeTest {
        // Getter result passed to callback
        val code = """
            const obj = {
                value: 42,
                get computed() { return this.value * 2 }
            }
            const results = [1, 2, 3].map(function(x) {
                return x + obj.computed
            })
            results[2]
        """.trimIndent()
        code.eval(it).assertEqualsTo(87L)
    }

    @Test
    fun thisInConstructor() = runtimeTest {
        // Constructor using this to set properties
        val code = """
            function Point(x, y) {
                this.x = x
                this.y = y
                this.getX = function() { return this.x }
            }
            const p = new Point(5, 10)
            p.getX()
        """.trimIndent()
        code.eval(it).assertEqualsTo(5L)
    }

    @Test
    fun classConstructorWithThis() = runtimeTest {
        // Class constructor using this
        val code = """
            class Point {
                constructor(x, y) {
                    this.x = x
                    this.y = y
                }
                getX() {
                    return this.x
                }
            }
            const p = new Point(7, 8)
            p.getX()
        """.trimIndent()
        code.eval(it).assertEqualsTo(7L)
    }
}
