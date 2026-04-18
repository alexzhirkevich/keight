import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive ES6+ feature tests
 * Covers: Destructuring, Trailing commas, Symbol, Class features, etc.
 *
 * Tests marked with @Ignore indicate features that are NOT YET SUPPORTED by keight.
 * These tests serve as a TODO list for future implementation.
 */
class ES6ComprehensiveTest {

    // ========== Destructuring Assignment Tests ==========

    @Test
    fun arrayDestructuringBasic() = runtimeTest {
        val code = """
            const [a, b, c] = [1, 2, 3]
            a + b + c
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    @Ignore("Array destructuring with skip element (elision) not supported")
    fun arrayDestructuringSkip() = runtimeTest {
        val code = """
            const [first, , third] = [1, 2, 3]
            first + third
        """.trimIndent()
        code.eval(it).assertEqualsTo(4L)
    }

    @Test
    fun arrayDestructuringRest() = runtimeTest {
        val code = """
            const [head, ...tail] = [1, 2, 3, 4]
            tail.join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("234")
    }

    @Test
    fun arrayDestructuringDefault() = runtimeTest {
        val code = """
            const [a = 1, b = 2, c = 3] = [10, 20]
            a + b + c
        """.trimIndent()
        code.eval(it).assertEqualsTo(33L)
    }

    @Test
    fun arrayDestructuringNested() = runtimeTest {
        val code = """
            const [[a, b], [c]] = [[1, 2], [3]]
            a + b + c
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    fun objectDestructuringBasic() = runtimeTest {
        val code = """
            const { name, age } = { name: 'John', age: 30 }
            name + age
        """.trimIndent()
        code.eval(it).assertEqualsTo("John30")
    }

    @Test
    fun objectDestructuringRenamed() = runtimeTest {
        val code = """
            const { name: userName, age: userAge } = { name: 'John', age: 30 }
            userName + userAge
        """.trimIndent()
        code.eval(it).assertEqualsTo("John30")
    }

    @Test
    fun objectDestructuringDefault() = runtimeTest {
        val code = """
            const { name, age = 25 } = { name: 'John' }
            age
        """.trimIndent()
        code.eval(it).assertEqualsTo(25L)
    }

    @Test
    fun objectDestructuringNested() = runtimeTest {
        val code = """
            const { person: { name, address: { city } } } = {
                person: { name: 'John', address: { city: 'NYC' } }
            }
            city
        """.trimIndent()
        code.eval(it).assertEqualsTo("NYC")
    }

    @Test
    fun objectDestructuringMixed() = runtimeTest {
        val code = """
            const { a, b: [c, d] } = { a: 1, b: [2, 3] }
            a + c + d
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    fun destructuringFunctionParams() = runtimeTest {
        val code = """
            function test({ name, value = 10 }) {
                return name + value
            }
            test({ name: 'x', value: 5 })
        """.trimIndent()
        code.eval(it).assertEqualsTo("x5")
    }

    @Test
    fun destructuringFunctionParamsArray() = runtimeTest {
        val code = """
            function test([a, b, c = 3]) {
                return a + b + c
            }
            test([1, 2])
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    @Ignore("Object destructuring assignment expression (({ a, b } = obj)) not supported")
    fun destructuringAssignmentExpression() = runtimeTest {
        val code = """
            let a, b
            ({ a, b } = { a: 1, b: 2 })
            a + b
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    @Ignore("Destructuring with computed property key not supported")
    fun destructuringWithComputedProperty() = runtimeTest {
        val code = """
            const key = 'name'
            const { [key]: value } = { name: 'test' }
            value
        """.trimIndent()
        code.eval(it).assertEqualsTo("test")
    }

    @Test
    fun destructuringInLoop() = runtimeTest {
        val code = """
            const arr = [{a: 1, b: 2}, {a: 3, b: 4}]
            let sum = 0
            for (const {a, b} of arr) {
                sum += a + b
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    fun destructuringInForOf() = runtimeTest {
        val code = """
            const pairs = [['x', 1], ['y', 2]]
            let result = ''
            for (const [key, val] of pairs) {
                result += key + val
            }
            result
        """.trimIndent()
        code.eval(it).assertEqualsTo("x1y2")
    }

    @Test
    @Ignore("Object rest spread in destructuring not supported")
    fun destructuringRest() = runtimeTest {
        val code = """
            const { a, ...rest } = { a: 1, b: 2, c: 3 }
            rest.b + rest.c
        """.trimIndent()
        code.eval(it).assertEqualsTo(5L)
    }

    // ========== Trailing Commas Tests ==========

    @Test
    fun trailingCommaArray() = runtimeTest {
        val code = """
            const arr = [1, 2, 3,]
            arr.length
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun trailingCommaObject() = runtimeTest {
        val code = """
            const obj = {
                a: 1,
                b: 2,
            }
            obj.a + obj.b
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun trailingCommaFunctionParams() = runtimeTest {
        val code = """
            function test(a, b,) {
                return a + b
            }
            test(1, 2)
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun trailingCommaFunctionCall() = runtimeTest {
        val code = """
            function test(...args) {
                return args.join('')
            }
            test(1, 2, 3,)
        """.trimIndent()
        code.eval(it).assertEqualsTo("123")
    }

    // ========== Symbol Tests ==========

    @Test
    fun symbolBasic() = runtimeTest {
        val code = """
            const sym = Symbol('test')
            typeof sym
        """.trimIndent()
        code.eval(it).assertEqualsTo("symbol")
    }

    @Test
    @Ignore("Symbol comparison issue")
    fun symbolUnique() = runtimeTest {
        val code = """
            const sym1 = Symbol('test')
            const sym2 = Symbol('test')
            sym1 === sym2
        """.trimIndent()
        code.eval(it).assertEqualsTo(false)
    }

    @Test
    fun symbolAsPropertyKey() = runtimeTest {
        val code = """
            const sym = Symbol('description')
            const obj = { [sym]: 'value' }
            obj[sym]
        """.trimIndent()
        code.eval(it).assertEqualsTo("value")
    }

    @Test
    @Ignore("Symbol.for not properly implemented")
    fun symbolFor() = runtimeTest {
        val code = """
            const sym1 = Symbol.for('test')
            const sym2 = Symbol.for('test')
            sym1 === sym2
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    @Ignore("Symbol.keyFor not properly implemented")
    fun symbolKeyFor() = runtimeTest {
        val code = """
            const sym = Symbol.for('test')
            Symbol.keyFor(sym)
        """.trimIndent()
        code.eval(it).assertEqualsTo("test")
    }

    @Test
    @Ignore("Symbol.keyFor not properly implemented")
    fun symbolKeyForUnknown() = runtimeTest {
        val code = """
            const sym = Symbol('test')
            Symbol.keyFor(sym)
        """.trimIndent()
        code.eval(it).assertEqualsTo(Undefined)
    }

    @Test
    @Ignore("Generator functions (function*) not supported")
    fun wellKnownSymbols() = runtimeTest {
        val code = """
            const obj = {
                [Symbol.iterator]: function*() {
                    yield 1
                    yield 2
                    yield 3
                }
            }
            [...obj][2]
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun symbolToString() = runtimeTest {
        val code = """
            const sym = Symbol('test')
            sym.toString()
        """.trimIndent()
        code.eval(it).assertEqualsTo("Symbol(test)")
    }

    @Test
    @Ignore("Symbol.description property not implemented")
    fun symbolDescription() = runtimeTest {
        val code = """
            const sym = Symbol('test description')
            sym.description
        """.trimIndent()
        code.eval(it).assertEqualsTo("test description")
    }

    // ========== Class Features Tests ==========

    @Test
    fun classGetterSetter() = runtimeTest {
        val code = """
            class Counter {
                constructor() { this._count = 0 }
                get count() { return this._count }
                set count(v) { this._count = v }
            }
            const c = new Counter()
            c.count = 10
            c.count
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    @Ignore("Class computed property names not fully supported")
    fun classComputedPropertyNames() = runtimeTest {
        val code = """
            const propName = 'dynamic'
            class Test {
                [propName]() { return 'dynamic method' }
            }
            new Test().dynamic()
        """.trimIndent()
        code.eval(it).assertEqualsTo("dynamic method")
    }

    @Test
    fun classStaticMethod() = runtimeTest {
        val code = """
            class MathHelper {
                static square(x) { return x * x }
            }
            MathHelper.square(5)
        """.trimIndent()
        code.eval(it).assertEqualsTo(25L)
    }

    @Test
    fun classStaticProperty() = runtimeTest {
        val code = """
            class Config {
                static defaultName = 'Anonymous'
            }
            Config.defaultName
        """.trimIndent()
        code.eval(it).assertEqualsTo("Anonymous")
    }

    @Test
    fun classStaticInheritance() = runtimeTest {
        val code = """
            class Parent {
                static greeting = 'Hello'
            }
            class Child extends Parent {}
            Child.greeting
        """.trimIndent()
        code.eval(it).assertEqualsTo("Hello")
    }

    @Test
    fun classPrototypeInheritance() = runtimeTest {
        val code = """
            class Animal {
                speak() { return 'sound' }
            }
            class Dog extends Animal {
                speak() { return 'bark' }
            }
            const d = new Dog()
            d instanceof Animal
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    @Ignore("Class.toString() output differs from JS spec")
    fun classToString() = runtimeTest {
        val code = """
            class Test {}
            Test.toString()
        """.trimIndent()
        code.eval(it).assertEqualsTo("class Test {}")
    }

    @Test
    fun classExpression() = runtimeTest {
        val code = """
            const MyClass = class {
                getValue() { return 42 }
            }
            new MyClass().getValue()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    @Ignore("Named class expression class name not accessible in static method")
    fun classExpressionNamed() = runtimeTest {
        val code = """
            const MyClass = class NamedClass {
                static who() { return NamedClass.name }
            }
            MyClass.who()
        """.trimIndent()
        code.eval(it).assertEqualsTo("NamedClass")
    }

    // ========== Arrow Function Tests ==========

    @Test
    fun arrowFunctionThis() = runtimeTest {
        val code = """
            const obj = {
                value: 10,
                getValue: () => this.value
            }
            typeof obj.getValue()
        """.trimIndent()
        code.eval(it).assertEqualsTo("undefined")
    }

    @Test
    fun arrowFunctionArguments() = runtimeTest {
        val code = """
            const func = () => arguments
            typeof func()
        """.trimIndent()
        code.eval(it).assertEqualsTo("undefined")
    }

    @Test
    fun arrowFunctionNested() = runtimeTest {
        val code = """
            const outer = () => {
                const inner = (x) => x * 2
                return inner(5)
            }
            outer()
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    fun arrowFunctionObjectReturn() = runtimeTest {
        val code = """
            const func = () => ({ x: 1, y: 2 })
            func().x + func().y
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun arrowFunctionIIFE() = runtimeTest {
        val code = """
            const result = ((a, b) => a + b)(3, 4)
            result
        """.trimIndent()
        code.eval(it).assertEqualsTo(7L)
    }

    // ========== Default Parameters Tests ==========

    @Test
    fun defaultParametersExpression() = runtimeTest {
        val code = """
            let called = false
            function test(x = (called = true, 10)) {
                return x
            }
            test()
            called
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun defaultParametersReference() = runtimeTest {
        val code = """
            function test(a, b = a + 1) {
                return b
            }
            test(5)
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    fun defaultParametersUndefined() = runtimeTest {
        val code = """
            function test(a = 10) {
                return a
            }
            test(undefined)
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    @Ignore("Default parameters should use undefined, not null, to trigger default value")
    fun defaultParametersNull() = runtimeTest {
        val code = """
            function test(a = 10) {
                return a
            }
            test(null)
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    fun defaultParametersMixed() = runtimeTest {
        val code = """
            function test(a, b = 5, c = a * 2) {
                return a + b + c
            }
            test(2)
        """.trimIndent()
        code.eval(it).assertEqualsTo(11L)
    }

    // ========== Rest Parameters Tests ==========

    @Test
    fun restParametersBasic() = runtimeTest {
        val code = """
            function test(...args) {
                return args.length
            }
            test(1, 2, 3, 4, 5)
        """.trimIndent()
        code.eval(it).assertEqualsTo(5L)
    }

    @Test
    fun restParametersWithOther() = runtimeTest {
        val code = """
            function test(a, b, ...rest) {
                return rest.join('')
            }
            test(1, 2, 3, 4, 5)
        """.trimIndent()
        code.eval(it).assertEqualsTo("345")
    }

    @Test
    fun restParametersEmpty() = runtimeTest {
        val code = """
            function test(a, ...rest) {
                return rest.length
            }
            test(1)
        """.trimIndent()
        code.eval(it).assertEqualsTo(0L)
    }

    // ========== Block Scoped Tests ==========

    @Test
    @Ignore("Block-scoped let shadowing behavior differs from JS spec")
    fun blockScopedLet() = runtimeTest {
        val code = """
            {
                let x = 10
                {
                    let x = 20
                    x
                }
            }
        """.trimIndent()
        code.eval(it).assertEqualsTo(20L)
    }

    @Test
    @Ignore("Block-scoped const not properly isolated")
    fun blockScopedConst() = runtimeTest {
        val code = """
            {
                const x = 10
                x
            }
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    fun blockScopedConstReassignment() = runtimeTest {
        assertFailsWith<TypeError> {
            """
                {
                    const x = 10
                    x = 20
                }
            """.trimIndent().eval()
        }
    }

    @Test
    fun temporalDeadZone() = runtimeTest {
        assertFailsWith<ReferenceError> {
            """
                {
                    console.log(x)
                    let x = 10
                }
            """.trimIndent().eval()
        }
    }

    // ========== Iterator/Iterable Tests ==========

    @Test
    fun iterableEntries() = runtimeTest {
        val code = """
            const obj = { a: 1, b: 2 }
            let entries = [...Object.entries(obj)]
            entries[0][0] + entries[1][0]
        """.trimIndent()
        code.eval(it).assertEqualsTo("ab")
    }

    @Test
    @Ignore("Object.keys returns Iterator instead of Array in some cases")
    fun iterableKeys() = runtimeTest {
        val code = """
            const obj = { a: 1, b: 2 }
            [...Object.keys(obj)].join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("ab")
    }

    @Test
    fun iterableValues() = runtimeTest {
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

    @Test
    fun arrayFrom() = runtimeTest {
        val code = """
            Array.from([1, 2, 3]).join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("123")
    }

    @Test
    @Ignore("Array.from with mapFn not working correctly")
    fun arrayFromMapFn() = runtimeTest {
        val code = """
            Array.from([1, 2, 3], x => x * 2).join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("246")
    }

    @Test
    fun arrayOf() = runtimeTest {
        val code = """
            const arr = Array.of(1, 2, 3)
            arr.join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("123")
    }

    @Test
    @Ignore("Array.find returns wrong value")
    fun arrayFind() = runtimeTest {
        val code = """
            [1, 2, 3, 4, 5].find(x => x > 3)
        """.trimIndent()
        code.eval(it).assertEqualsTo(4L)
    }

    @Test
    @Ignore("Array.findIndex returns wrong value")
    fun arrayFindIndex() = runtimeTest {
        val code = """
            [1, 2, 3, 4, 5].findIndex(x => x > 3)
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun arrayIncludes() = runtimeTest {
        val code = """
            [1, 2, 3].includes(2)
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    @Ignore("Array.flat with Infinity not fully supported")
    fun arrayFlat() = runtimeTest {
        val code = """
            [1, [2, 3], [4, [5]]].flat(Infinity).join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("12345")
    }

    @Test
    @Ignore("Array.flatMap behavior differs from spec")
    fun arrayFlatMap() = runtimeTest {
        val code = """
            [1, 2, 3].flatMap(x => [x, x * 2]).join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("124369")
    }

    // ========== Object Methods Tests ==========

    @Test
    fun objectValues() = runtimeTest {
        val code = """
            Object.values({ a: 1, b: 2 }).join('')
        """.trimIndent()
        code.eval(it).assertEqualsTo("12")
    }

    @Test
    fun objectEntries() = runtimeTest {
        val code = """
            Object.entries({ x: 1, y: 2 }).length
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun objectFromEntries() = runtimeTest {
        val code = """
            const obj = Object.fromEntries([['a', 1], ['b', 2]])
            obj.a + obj.b
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    @Ignore("Object.hasOwn not implemented")
    fun objectHasOwn() = runtimeTest {
        val code = """
            const obj = { a: 1 }
            Object.hasOwn(obj, 'a')
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    // ========== String Methods Tests ==========

    @Test
    fun stringStartsWith() = runtimeTest {
        val code = """
            'hello world'.startsWith('hello')
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun stringEndsWith() = runtimeTest {
        val code = """
            'hello world'.endsWith('world')
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun stringIncludes() = runtimeTest {
        val code = """
            'hello world'.includes('lo')
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun stringRepeat() = runtimeTest {
        val code = """
            'ab'.repeat(3)
        """.trimIndent()
        code.eval(it).assertEqualsTo("ababab")
    }

    @Test
    fun stringPadStart() = runtimeTest {
        val code = """
            '5'.padStart(3, '0')
        """.trimIndent()
        code.eval(it).assertEqualsTo("005")
    }

    @Test
    fun stringPadEnd() = runtimeTest {
        val code = """
            '5'.padEnd(3, '0')
        """.trimIndent()
        code.eval(it).assertEqualsTo("500")
    }

    @Test
    fun stringTrimStart() = runtimeTest {
        val code = """
            '  hello  '.trimStart()
        """.trimIndent()
        code.eval(it).assertEqualsTo("hello  ")
    }

    @Test
    fun stringTrimEnd() = runtimeTest {
        val code = """
            '  hello  '.trimEnd()
        """.trimIndent()
        code.eval(it).assertEqualsTo("  hello")
    }

    // ========== Number Methods Tests ==========

    @Test
    fun numberIsFinite() = runtimeTest {
        val code = """
            Number.isFinite(Infinity)
        """.trimIndent()
        code.eval(it).assertEqualsTo(false)
    }

    @Test
    fun numberIsNaN() = runtimeTest {
        val code = """
            Number.isNaN(NaN)
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun numberIsInteger() = runtimeTest {
        val code = """
            Number.isInteger(5)
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    @Ignore("Number.isSafeInteger comparison issue")
    fun numberIsSafeInteger() = runtimeTest {
        val code = """
            Number.isSafeInteger(Math.pow(2, 53) - 1)
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun numberEpsilon() = runtimeTest {
        val code = """
            Number.EPSILON > 0
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    fun numberMaxSafeInteger() = runtimeTest {
        val code = """
            Number.MAX_SAFE_INTEGER > 0
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    // ========== Math Methods Tests ==========

    @Test
    @Ignore("Math.trunc returns Double instead of Long")
    fun mathTrunc() = runtimeTest {
        val code = """
            Math.trunc(4.9)
        """.trimIndent()
        code.eval(it).assertEqualsTo(4L)
    }

    @Test
    @Ignore("Math.sign returns Double instead of Long for integer values")
    fun mathSign() = runtimeTest {
        val code = """
            Math.sign(-5)
        """.trimIndent()
        code.eval(it).assertEqualsTo(-1L)
    }

    @Test
    fun mathCbrt() = runtimeTest {
        val code = """
            Math.cbrt(27)
        """.trimIndent()
        code.eval(it).assertEqualsTo(3.0)
    }

    @Test
    fun mathImul() = runtimeTest {
        val code = """
            Math.imul(2, 3)
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    @Ignore("Math.clz32 not implemented")
    fun mathClz32() = runtimeTest {
        val code = """
            Math.clz32(1)
        """.trimIndent()
        code.eval(it).assertEqualsTo(31L)
    }

    @Test
    @Ignore("Math.hypot not implemented")
    fun mathHypot() = runtimeTest {
        val code = """
            Math.hypot(3, 4)
        """.trimIndent()
        code.eval(it).assertEqualsTo(5.0)
    }

    @Test
    @Ignore("Math.log10 not implemented")
    fun mathLog10() = runtimeTest {
        val code = """
            Math.log10(100)
        """.trimIndent()
        code.eval(it).assertEqualsTo(2.0)
    }

    @Test
    @Ignore("Math.log2 not implemented")
    fun mathLog2() = runtimeTest {
        val code = """
            Math.log2(8)
        """.trimIndent()
        code.eval(it).assertEqualsTo(3.0)
    }

    @Test
    @Ignore("Math.log1p not implemented")
    fun mathLog1p() = runtimeTest {
        val code = """
            Math.log1p(Math.E - 1)
        """.trimIndent()
        code.eval(it).assertEqualsTo(1.0)
    }

    @Test
    @Ignore("Math.expm1 not implemented")
    fun mathExpm1() = runtimeTest {
        val code = """
            Math.expm1(1) > 1
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    // ========== Set Tests ==========

    @Test
    fun setHas() = runtimeTest {
        val code = """
            const s = new Set([1, 2, 3])
            s.has(2)
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    @Ignore("Set.delete returns wrong value")
    fun setDelete() = runtimeTest {
        val code = """
            const s = new Set([1, 2, 3])
            s.delete(2)
            s.has(2)
        """.trimIndent()
        code.eval(it).assertEqualsTo(false)
    }

    @Test
    @Ignore("Set.size returns property value instead of getter value")
    fun setSize() = runtimeTest {
        val code = """
            new Set([1, 2, 3]).size
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun setClear() = runtimeTest {
        val code = """
            const s = new Set([1, 2, 3])
            s.clear()
            s.size
        """.trimIndent()
        code.eval(it).assertEqualsTo(0L)
    }

    @Test
    @Ignore("Set.forEach callback parameters order or value issue")
    fun setForEach() = runtimeTest {
        val code = """
            const s = new Set([1, 2, 3])
            let sum = 0
            s.forEach(v => sum += v)
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    fun setIteration() = runtimeTest {
        val code = """
            const s = new Set([1, 2, 3])
            let sum = 0
            for (let v of s) {
                sum += v
            }
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    // ========== Map Tests ==========

    @Test
    fun mapHas() = runtimeTest {
        val code = """
            const m = new Map([['a', 1]])
            m.has('a')
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    @Ignore("Map.delete returns wrong value")
    fun mapDelete() = runtimeTest {
        val code = """
            const m = new Map([['a', 1]])
            m.delete('a')
            m.has('a')
        """.trimIndent()
        code.eval(it).assertEqualsTo(false)
    }

    @Test
    fun mapSize() = runtimeTest {
        val code = """
            new Map([['a', 1], ['b', 2]]).size
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun mapClear() = runtimeTest {
        val code = """
            const m = new Map([['a', 1]])
            m.clear()
            m.size
        """.trimIndent()
        code.eval(it).assertEqualsTo(0L)
    }

    @Test
    fun mapForEach() = runtimeTest {
        val code = """
            const m = new Map([['a', 1], ['b', 2]])
            let sum = 0
            m.forEach((v) => sum += v)
            sum
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    @Ignore("Map.keys() iteration not working correctly")
    fun mapIteration() = runtimeTest {
        val code = """
            const m = new Map([['a', 1], ['b', 2]])
            let keys = ''
            for (let k of m.keys()) {
                keys += k
            }
            keys
        """.trimIndent()
        code.eval(it).assertEqualsTo("ab")
    }

    // ========== Template Literal Tests ==========

    @Test
    @Ignore("Template literal multiline indentation handling differs from JS spec")
    fun templateLiteralMultiline() = runtimeTest {
        val code = """
            `line1
            line2
            line3`
        """.trimIndent()
        code.eval(it).assertEqualsTo("line1\n            line2\n            line3")
    }

    @Test
    fun templateLiteralNested() = runtimeTest {
        val code = """
            const a = 5
            const b = 10
            `result: ${'$'}{a + b * 2}`
        """.trimIndent()
        code.eval(it).assertEqualsTo("result: 25")
    }

    @Test
    @Ignore("Tagged template literals not supported")
    fun templateLiteralTagged() = runtimeTest {
        val code = """
            function tag(strings, value) {
                return strings[0] + value + strings[1]
            }
            tag`test${'$'}{1}value`
        """.trimIndent()
        code.eval(it).assertEqualsTo("test1value")
    }

    @Test
    @Ignore("Template literal escape sequences not fully implemented")
    fun templateLiteralEscape() = runtimeTest {
        val code = """
            `backtick: \`\nnewline: \n\ttab: \t`
        """.trimIndent()
        code.eval(it).assertEqualsTo("backtick: `\nnewline: \n\ttab: \t")
    }

    // ========== Promise Tests ==========

    @Test
    @Ignore("Promise constructor callback syntax issue")
    fun promiseBasic() = runtimeTest {
        val code = """
            async function test() {
                return new Promise(resolve => resolve(42))
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun promiseThen() = runtimeTest {
        val code = """
            async function test() {
                const value = await new Promise(r => r(10))
                return value * 2
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(20L)
    }

    @Test
    fun promiseChain() = runtimeTest {
        val code = """
            async function test() {
                const p = new Promise(r => r(2))
                const a = await p
                const b = await p
                return a + b
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(4L)
    }

    @Test
    @Ignore("Promise.all not fully implemented for concurrent promises")
    fun promiseAll() = runtimeTest {
        val code = """
            async function test() {
                const results = await Promise.all([
                    Promise.resolve(1),
                    Promise.resolve(2),
                    Promise.resolve(3)
                ])
                return results.reduce((a, b) => a + b, 0)
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(6L)
    }

    @Test
    @Ignore("Promise.race with setTimeout not supported")
    fun promiseRace() = runtimeTest {
        val code = """
            async function test() {
                return await Promise.race([
                    new Promise(r => setTimeout(() => r(3), 100)),
                    new Promise(r => setTimeout(() => r(1), 50)),
                    new Promise(r => setTimeout(() => r(2), 75))
                ])
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(1L)
    }

    // ========== Async/Await Tests ==========

    @Test
    @Ignore("Promise.all with array destructuring not working correctly")
    fun asyncAwaitParallel() = runtimeTest {
        val code = """
            async function test() {
                const p1 = Promise.resolve(1)
                const p2 = Promise.resolve(2)
                const [a, b] = await Promise.all([p1, p2])
                return a + b
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    @Test
    fun asyncAwaitError() = runtimeTest {
        val code = """
            async function test() {
                try {
                    await Promise.reject(new Error('test'))
                } catch (e) {
                    return e.message
                }
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo("test")
    }

    @Test
    fun asyncArrowFunction() = runtimeTest {
        val code = """
            const test = async () => {
                return await Promise.resolve(42)
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    // ========== Enhanced Regular Expression Tests ==========

    @Test
    fun regexSticky() = runtimeTest {
        val code = """
            const re = /test/y
            re.lastIndex = 0
            re.test('test123')
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    @Test
    @Ignore("Unicode regex (u flag) not supported")
    fun regexUnicode() = runtimeTest {
        val code = """
            const re = /\\u{1F600}/u
            re.test('😀')
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    // ========== Binary and Octal Literals ==========

    @Test
    fun binaryLiteral() = runtimeTest {
        val code = """
            0b10
        """.trimIndent()
        code.eval(it).assertEqualsTo(2L)
    }

    @Test
    fun octalLiteral() = runtimeTest {
        val code = """
            0o10
        """.trimIndent()
        code.eval(it).assertEqualsTo(8L)
    }

    @Test
    fun numberPrototypeToStringRadix() = runtimeTest {
        val code = """
            (255).toString(16)
        """.trimIndent()
        code.eval(it).assertEqualsTo("ff")
    }

    // ========== for-await-of (ES2018) ==========

    @Test
    @Ignore("for-await-of not supported")
    fun forAwaitOf() = runtimeTest {
        val code = """
            async function test() {
                const results = []
                for await (let v of [Promise.resolve(1), Promise.resolve(2)]) {
                    results.push(v)
                }
                return results[0] + results[1]
            }
            await test()
        """.trimIndent()
        code.eval(it).assertEqualsTo(3L)
    }

    // ========== Nullish Coalescing Enhancement ==========

    @Test
    fun nullishCoalescingChain() = runtimeTest {
        val code = """
            const a = null
            const b = undefined
            const c = 0
            a ?? b ?? c ?? 10
        """.trimIndent()
        code.eval(it).assertEqualsTo(0L)
    }

    @Test
    fun nullishCoalescingWithOr() = runtimeTest {
        val code = """
            const a = null
            const b = false
            a ?? (b || true)
        """.trimIndent()
        code.eval(it).assertEqualsTo(true)
    }

    // ========== Optional Chaining Enhancement ==========

    @Test
    fun optionalChainingNested() = runtimeTest {
        val code = """
            const obj = {
                a: {
                    b: {
                        c: 42
                    }
                }
            }
            obj?.a?.b?.c
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun optionalChainingNull() = runtimeTest {
        val code = """
            const obj = null
            obj?.a?.b
        """.trimIndent()
        code.eval(it).assertEqualsTo(Undefined)
    }

    @Test
    fun optionalChainingCall() = runtimeTest {
        val code = """
            const obj = {
                method: () => 42
            }
            obj?.method?.()
        """.trimIndent()
        code.eval(it).assertEqualsTo(42L)
    }

    @Test
    fun optionalChainingArray() = runtimeTest {
        val code = """
            const arr = [1, 2, 3]
            arr?.[0]
        """.trimIndent()
        code.eval(it).assertEqualsTo(1L)
    }

    // ========== Logical Assignment Operators (ES2021) ==========

    @Test
    @Ignore("Logical OR assignment (||=) not supported")
    fun logicalOrAssignment() = runtimeTest {
        val code = """
            let x = null
            x ||= 10
            x
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    @Ignore("Logical AND assignment (&&=) not supported")
    fun logicalAndAssignment() = runtimeTest {
        val code = """
            let x = 5
            x &&= 10
            x
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    @Test
    @Ignore("Nullish coalescing assignment (??=) not supported")
    fun logicalNullishAssignment() = runtimeTest {
        val code = """
            let x = null
            x ??= 10
            x
        """.trimIndent()
        code.eval(it).assertEqualsTo(10L)
    }

    // ========== Numeric Separators ==========

    @Test
    fun numericSeparator() = runtimeTest {
        val code = """
            1_000_000
        """.trimIndent()
        code.eval(it).assertEqualsTo(1000000L)
    }

    @Test
    fun numericSeparatorDecimal() = runtimeTest {
        val code = """
            1.234_567
        """.trimIndent()
        code.eval(it).assertEqualsTo(1.234567)
    }

    @Test
    fun numericSeparatorBinary() = runtimeTest {
        val code = """
            0b1010_0010
        """.trimIndent()
        code.eval(it).assertEqualsTo(162L)
    }
}
