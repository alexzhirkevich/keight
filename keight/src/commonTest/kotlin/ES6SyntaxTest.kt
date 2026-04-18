import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ES6+ syntax features:
 * - for...of loops
 * - Template string interpolation
 * - Property/Method shorthand
 * - Computed property names
 * - Array spread operator
 */
class ES6SyntaxTest {

    // ========== for...of Loop Tests ==========

    @Test
    fun forOfWithArray() = runtimeTest {
        val code = """
            let sum = 0
            for (let x of [1, 2, 3]) {
                sum += x
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    fun forOfWithSet() = runtimeTest {
        val code = """
            let sum = 0
            const s = new Set([10, 20, 30])
            for (let x of s) {
                sum += x
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(60L)
    }

    @Test
    fun forOfWithMap() = runtimeTest {
        val code = """
            let result = []
            const m = new Map([['a', 1], ['b', 2]])
            for (let [k, v] of m) {
                result.push(k + v)
            }
            result.join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("a1b2")
    }

    @Test
    fun forOfWithString() = runtimeTest {
        val code = """
            let chars = []
            for (let c of 'abc') {
                chars.push(c)
            }
            chars.join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("abc")
    }

    @Test
    fun forOfBreak() = runtimeTest {
        val code = """
            let sum = 0
            for (let x of [1, 2, 3, 4, 5]) {
                if (x > 3) break
                sum += x
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    fun forOfContinue() = runtimeTest {
        val code = """
            let sum = 0
            for (let x of [1, 2, 3, 4, 5]) {
                if (x % 2 === 0) continue
                sum += x
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(9L)
    }

    @Test
    fun forOfWithVar() = runtimeTest {
        val code = """
            var result = []
            for (var x of [1, 2, 3]) {
                result.push(x * 2)
            }
            result.join(',')
        """.trimIndent()
        code.eval(it).assertEqualsTo("2,4,6")
    }

    @Test
    fun forOfWithConst() = runtimeTest {
        val code = """
            let result = []
            for (const x of [1, 2, 3]) {
                result.push(x + 10)
            }
            result.join('-')
        """.trimIndent()
        code.eval(it).assertEqualsTo("11-12-13")
    }

    @Test
    fun forOfDestructuring() = runtimeTest {
        val code = """
            let sum = 0
            for (const [a, b] of [[1, 2], [3, 4]]) {
                sum += a + b
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    // ========== Template String Tests ==========

    @Test
    fun templateLiteralSimple() = runtimeTest {
        // Note: In Kotlin, ${"$"} expands to literal "$", so this tests "${$}{x}" which outputs "$x" literal
        // To test variable interpolation, we use ${x} directly
        val code = """
            const x = 10
            const y = 20
            `x = ${'$'}{x}, y = ${'$'}{y}`
        """.trimIndent()
        code.eval(it).assertEqualsTo("x = 10, y = 20")
    }

    @Test
    fun templateLiteralDollarSign() = runtimeTest {
        // Test that ${"$"} outputs a literal "$" (Kotlin ${"$"} -> JS "$")
        val code = """`Price: ${"$"}100`"""
        code.eval(it).assertEqualsTo("Price: $100")
    }

    @Test
    fun templateLiteralExpression() = runtimeTest {
        val code = """
            const a = 5
            const b = 3
            `Sum: ${'$'}{a + b}, Product: ${'$'}{a * b}`
        """.trimIndent()
        code.eval(it).assertEqualsTo("Sum: 8, Product: 15")
    }

    @Test
    fun templateLiteralNested() = runtimeTest {
        val code = """
            const arr = [1, 2, 3]
            `Sum: ${'$'}{arr.reduce((a, b) => a + b, 0)}`
        """.trimIndent()
        code.eval(it).assertEqualsTo("Sum: 6")
    }

    @Test
    fun templateLiteralFunction() = runtimeTest {
        val code = """
            const double = (x) => x * 2
            `Result: ${'$'}{double(21)}`
        """.trimIndent()
        code.eval(it).assertEqualsTo("Result: 42")
    }

    // ========== Property Shorthand Tests ==========

    @Test
    fun propertyShorthand() = runtimeTest {
        val code = """
            const x = 10, y = 20
            const obj = { x, y }
            obj.x + obj.y
        """.trimIndent()
        code.eval(it).assertEqualsTo(30L)
    }

    @Test
    fun methodShorthand() = runtimeTest {
        val code = """
            const obj = {
                value: 42,
                getValue() {
                    return this.value
                }
            }
            obj.getValue()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun methodShorthandWithParams() = runtimeTest {
        val code = """
            const calc = {
                value: 10,
                add(n) {
                    return this.value + n
                }
            }
            calc.add(5)
        """.trimIndent()
        code.eval(it).assertEqualsTo(15L)
    }

    @Test
    fun asyncMethod() = runtimeTest {
        val code = """
            const obj = {
                async getValue() {
                    return 42
                }
            }
            await obj.getValue()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    // ========== Computed Property Name Tests ==========

    @Test
    fun computedPropertyName() = runtimeTest {
        val code = """
            const key = 'dynamic'
            const obj = {
                [key]: 'value'
            }
            obj.dynamic
        """.trimIndent()
        code.eval(it).assertEqualsTo("value")
    }

    @Test
    fun computedPropertyNameStringLiteral() = runtimeTest {
        val code = """
            const obj = {
                ['test']: 100
            }
            obj.test
        """.trimIndent()
        code.eval(it).assertEqualsTo(100L)
    }

    @Test
    fun computedPropertyNameExpression() = runtimeTest {
        // Note: ['prefix' + 'Value'] uses string literal 'prefix', not variable prefix
        // So this creates key "prefixValue", not "getValue"
        // Using variable prefix instead: [prefix + 'Value']
        val code = """
            const prefix = 'get'
            const obj = {
                [prefix + 'Value']: 100
            }
            obj.getValue
        """.trimIndent()
        code.eval(it).assertEqualsTo(100L)
    }

    @Test
    fun computedPropertyWithMethod() = runtimeTest {
        // Test computed property with method - need to use 'this' to access object properties
        val code = """
            const methodName = 'getValue'
            const obj = {
                _value: 42,
                [methodName]() {
                    return this._value
                }
            }
            obj.getValue()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    // ========== Array Spread Tests ==========

    @Test
    fun spreadInArray() = runtimeTest {
        // Original test format with semicolons
        val code = """
            const a = [1, 2];
            const b = [3, 4];
            [...a, ...b]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(1L, 2L, 3L, 4L))
    }

    @Test
    fun spreadInFunctionCall() = runtimeTest {
        val code = """
            const arr = [1, 2, 3];
            Math.max(...arr)
        """.trimIndent()
        // Math.max returns a number (Double), not Long
        code.eval(it).assertEqualsTo(3.0)
    }

    @Test
    fun spreadWithValues() = runtimeTest {
        val code = """
            const first = [1, 2];
            const second = [3];
            const third = [4, 5, 6];
            [...first, ...second, ...third]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(1L, 2L, 3L, 4L, 5L, 6L))
    }

    // ========== Object Spread Tests ==========

    @Test
    fun objectSpread() = runtimeTest {
        val code = """
            const a = { x: 1 }
            const b = { y: 2 }
            const c = { ...a, ...b }
            c.x + c.y
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun objectSpreadOverride() = runtimeTest {
        val code = """
            const base = { x: 1, y: 2 }
            const extended = { ...base, y: 99, z: 3 }
            base.x + extended.y + extended.z
        """.trimIndent()
        code.eval(it).assertEqualsTo(103L)
    }

    // ========== Combined Tests ==========

    @Test
    fun combinedForOfWithSpread() = runtimeTest {
        val code = """
            const arr1 = [1, 2]
            const arr2 = [3, 4]
            let sum = 0
            for (let x of [...arr1, ...arr2]) {
                sum += x
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    fun templateWithObjectProperty() = runtimeTest {
        val code = """
            const user = { name: 'John', age: 30 }
            `Name: ${'$'}{user.name}, Age: ${'$'}{user.age}`
        """.trimIndent()
        code.eval(it).assertEqualsTo("Name: John, Age: 30")
    }

    @Test
    fun forOfWithObjectEntries() = runtimeTest {
        val code = """
            const obj = { a: 1, b: 2 }
            let sum = 0
            for (let v of Object.values(obj)) {
                sum += v
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    // ========== Extended Spread Tests ==========

    @Test
    fun spreadInFunctionCallMultiple() = runtimeTest {
        val code = """
            const arr1 = [1, 2];
            const arr2 = [3, 4];
            Math.min(...arr1, ...arr2)
        """.trimIndent()
        code.eval(it).assertEqualsTo(1.0)
    }

    @Test
    fun spreadWithMixedArgs() = runtimeTest {
        val code = """
            const arr = [2, 3, 4];
            Math.max(1, ...arr, 5)
        """.trimIndent()
        code.eval(it).assertEqualsTo(5.0)
    }

    @Test
    fun spreadEmptyArray() = runtimeTest {
        val code = """
            const empty = [];
            const a = [1, 2];
            [...empty, ...a, ...empty]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(1L, 2L))
    }

    @Test
    fun spreadNestedArray() = runtimeTest {
        val code = """
            const arr = [[1, 2], [3, 4]];
            [...arr[0], ...arr[1]]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(1L, 2L, 3L, 4L))
    }

    @Test
    fun spreadInObjectLiteral() = runtimeTest {
        val code = """
            const x = { a: 1 };
            const y = { b: 2 };
            const z = { ...x, ...y };
            z.a + z.b
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    // Note: String spread (e.g., [...'Hi']) has limited support
    // Current implementation treats string as single element

    @Test
    fun spreadInArrayWithNonSpread() = runtimeTest {
        val code = """
            const a = [1, 2];
            const b = 3;
            [0, ...a, b, 4]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(0L, 1L, 2L, 3L, 4L))
    }

    @Test
    fun spreadFunctionResult() = runtimeTest {
        val code = """
            const getArray = () => [1, 2, 3];
            [...getArray()]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(1L, 2L, 3L))
    }

    @Test
    fun spreadConcatArrays() = runtimeTest {
        val code = """
            const concat = (a, b, c) => [...a, ...b, ...c];
            concat([1], [2], [3])
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(1L, 2L, 3L))
    }

    @Test
    fun spreadInNewExpression() = runtimeTest {
        val code = """
            const arr = [1, 2, 3];
            new Set(...arr.map(x => x * 2))
        """.trimIndent()
        // This tests spread with method chain
        val result = code.eval(it)
        assertTrue(result is Set<*>)
    }

    @Test
    fun spreadVariableDeclaration() = runtimeTest {
        val code = """
            const original = [1, 2, 3];
            const copy = [...original];
            copy[0] = 99;
            original[0]
        """.trimIndent()
        code.eval(it).assertEqualsTo(1L)
    }

    @Test
    fun spreadWithNegativeNumbers() = runtimeTest {
        val code = """
            const neg = [-1, -2];
            const pos = [1, 2];
            [...neg, 0, ...pos]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(-1L, -2L, 0L, 1L, 2L))
    }

    @Test
    fun spreadWithFloats() = runtimeTest {
        val code = """
            const floats = [1.5, 2.5];
            Math.max(...floats)
        """.trimIndent()
        code.eval(it).assertEqualsTo(2.5)
    }

    @Test
    fun spreadInConditional() = runtimeTest {
        val code = """
            const shouldSpread = true;
            const arr = [1, 2];
            shouldSpread ? [...arr, 3] : [3]
        """.trimIndent()
        code.eval(it).assertEqualsTo(listOf(1L, 2L, 3L))
    }

    // ========== Extended Inheritance Tests ==========

    @Test
    fun superPropertyAccess() = runtimeTest {
        val code = """
            class Parent {
                greet() {
                    return 'Hello'
                }
            }
            class Child extends Parent {
                greet() {
                    return super.greet() + ', World!'
                }
            }
            new Child().greet()
        """.trimIndent()
        code.eval(it).assertEqualsTo("Hello, World!")
    }

    @Test
    fun superConstructorCall() = runtimeTest {
        val code = """
            class Base {
                constructor(x) {
                    this.x = x
                }
            }
            class Derived extends Base {
                constructor(x, y) {
                    super(x)
                    this.y = y
                }
            }
            const d = new Derived(1, 2)
            d.x + d.y
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun superMultipleLevels() = runtimeTest {
        val code = """
            class A {
                value() { return 'A' }
            }
            class B extends A {
                value() { return super.value() + 'B' }
            }
            class C extends B {
                value() { return super.value() + 'C' }
            }
            new C().value()
        """.trimIndent()
        code.eval(it).assertEqualsTo("ABC")
    }

    @Test
    fun superInGetter() = runtimeTest {
        val code = """
            class Parent {
                get val() { return 10 }
            }
            class Child extends Parent {
                get val() { return super.val * 2 }
            }
            new Child().val
        """.trimIndent()
        code.eval(it).assertEqualsTo(20L)
    }

    @Test
    fun superInSetter() = runtimeTest {
        val code = """
            class Parent {
                constructor() { this.val = 0 }
            }
            class Child extends Parent {
                set val(v) { this._val = v * 2 }
            }
            const c = new Child()
            c.val = 5
            c._val
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    fun superPropertyAccessMethodChain() = runtimeTest {
        val code = """
            class Base {
                add(x) { return x + 1 }
            }
            class Derived extends Base {
                add(x) { return super.add(x) + 1 }
            }
            new Derived().add(1)
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun superWithArguments() = runtimeTest {
        val code = """
            class Math {
                sum(a, b, c) { return a + b + c }
            }
            class Advanced extends Math {
                sum(a, b, c) { return super.sum(a, b, c) * 2 }
            }
            new Advanced().sum(1, 2, 3)
        """.trimIndent()
        code.eval(it).assertEqualsTo(12L)
    }

    @Test
    fun superAccessProperty() = runtimeTest {
        val code = """
            class Parent {
                baseValue = 42
            }
            class Child extends Parent {
                getResult() {
                    return super.baseValue
                }
            }
            new Child().getResult()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun superStaticAccess() = runtimeTest {
        // Note: super cannot be used in static methods in JavaScript
        // This test verifies static inheritance works correctly
        val code = """
            class Parent {
                static base() { return 'parent' }
            }
            class Child extends Parent {
                static derived() {
                    return Parent.base() + '-child'
                }
            }
            Child.derived()
        """.trimIndent()
        code.eval(it).assertEqualsTo("parent-child")
    }

    @Test
    fun superWithoutArgs() = runtimeTest {
        val code = """
            class Base {
                constructor() {
                    this.init = true
                }
            }
            class Derived extends Base {
                constructor() {
                    super()
                }
            }
            new Derived().init
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun superInMethodCall() = runtimeTest {
        // Test super method call
        val code = """
            class Base {
                addOne(x) { return x + 1 }
            }
            class Derived extends Base {
                addTwo(x) {
                    return super.addOne(super.addOne(x))
                }
            }
            new Derived().addTwo(0)
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun superAccessNestedProperty() = runtimeTest {
        val code = """
            class Base {
                config = { key: 'value' }
            }
            class Derived extends Base {
                getConfig() {
                    return super.config.key
                }
            }
            new Derived().getConfig()
        """.trimIndent()
        code.eval(it).assertEqualsTo("value")
    }

    @Test
    fun superSuperChaining() = runtimeTest {
        val code = """
            class Grandparent {
                identify() { return 'gp' }
            }
            class Parent extends Grandparent {
                identify() { return 'p-' + super.identify() }
            }
            class Child extends Parent {
                identify() { return 'c-' + super.identify() }
            }
            new Child().identify()
        """.trimIndent()
        code.eval(it).assertEqualsTo("c-p-gp")
    }
}
