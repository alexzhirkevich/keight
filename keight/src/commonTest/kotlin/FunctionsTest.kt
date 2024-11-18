import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.interpreter.Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FunctionsTest {

    @Test
    fun creation() = runTest {
        """
            var x;
            function test(a,b) { return a + b }
            x = test(1,2)
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            function test(a,b) {
                return a + b
            }
            test(1,2)
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            function test(a,b)
            {
                let x = b + 1
                return a + x
            }
            test(1,2)
        """.trimIndent().eval().assertEqualsTo(4L)
    }

    @Test
    fun keyword_name() = runTest{
        assertFailsWith<SyntaxError> {
            "function throw() {}".eval()
        }
        assertFailsWith<SyntaxError> {
            "function return() {}".eval()
        }
        assertFailsWith<SyntaxError> {
            "function await() {}".eval()
        }
    }

    @Test
    fun noReturn()  = runtimeTest{
        """
            function test(){
                5  
            }
            test()
        """.trimIndent().eval().assertEqualsTo(Unit)
    }

    @Test
    fun defaultArgs() = runTest {
        """
            function test(a, b = 2)
            {
                return  a + b
            }
            test(1)
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            var x;
            function test(a, b = 2)
            {
                return a + b
            }
            x = test(2,3)
        """.trimIndent().eval().assertEqualsTo(5L)

        """
            function test(a = 1, b = 2)
            {
                return a + b
            }
            test()
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun scope() = runTest {
        """
            let x = 1
            
            function fun1(){
                function fun2() {
                    x = 2
                }
                
                fun2()
            }
            fun1()
            
            x
        """.trimIndent().eval().assertEqualsTo(2L)

        assertFailsWith<ReferenceError> {
            """
            function fun1(){
                function fun2() {
                    
                }
            }
            fun2()
        """.trimIndent().eval().assertEqualsTo(2L)
        }
    }

    @Test
    fun variable_func() = runTest {
        """
            const test = function(a,b) { return a + b }
            test(1,2)
        """.trimIndent().eval().assertEqualsTo(3L)

        assertFailsWith<TypeError> {
            """
                let test = function(a,b) { return a + b }
                test = 1
                test(1,2)
            """.trimIndent().eval().assertEqualsTo(3L)
        }
        assertFailsWith<ReferenceError> {
            """
            {
                const test = function(a,b) { return a + b }
            }
            test(1,2)
        """.trimIndent().eval()
        }
    }

    @Test
    fun arrow() = runTest {
        """
            const test = (a,b) => a + b
            test(1,2)
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            const test = (a,b) => { return a + b }
            test(1,2)
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            const test = (a,b) => { a + b }
            test(1,2)
        """.trimIndent().eval().assertEqualsTo(Unit)

        """
            const test = a => a+1
            test(1)
        """.trimIndent().eval().assertEqualsTo(2L)


        """
            let x = 0
            const test = (a,b) => a + b; x = 1
            x
        """.trimIndent().eval().assertEqualsTo(1L)
    }

    @Test
    fun recursion() = runTest {

        """
           function fib(n) {
                if (n<2){
                    return n
                }
                return  fib(n - 1) + fib(n - 2)
           }

           fib(7)
        """.trimIndent().eval().assertEqualsTo(13L)
    }

    @Test
    fun vararg() = runTest {
        """
            function test(x, ...rest) {
                return x.toString() + "_"  + rest.join('')
            }
            
            test(1, 2, 3, 4)
        """.eval().assertEqualsTo("1_234")
    }

    @Test
    fun constructor_function() = runtimeTest { runtime ->

        """
            function Person(name,age){
                this.name = name
                this.age = age
            }
            const p = new Person('John', 25)
        """.eval(runtime)

        "typeof p".eval(runtime).assertEqualsTo("object")
        "p.name".eval(runtime).assertEqualsTo("John")
        "p.age".eval(runtime).assertEqualsTo(25L)
        "p.prototype".eval(runtime).assertEqualsTo(Unit)
        "p.__proto__".eval(runtime).assertEqualsTo("Person.prototype".eval(runtime))
    }

    @Test
    fun add_field_to_prototype_constructor() = runtimeTest { runtime ->

        """
            function Person(name,age){
                this.name = name
                this.age = age
            }
            Person.prototype.nationality = 'English'
        """.eval(runtime)

        "new Person('John', 25).nationality".eval(runtime).assertEqualsTo("English")
    }

    @Test
    fun changed_prototype() = runTest {
        """
            Number.prototype.toFixed = function() { return this * this }
            5.toFixed()
        """.eval().assertEqualsTo(25L)


        // arrow function should not receive binding
        """
            Number.prototype.toFixed = () => this * this
            5.toFixed()
        """.eval().assertEqualsTo(Double.NaN)

        """
            Number.prototype.test = 'abc'
            5..test
        """.eval().assertEqualsTo("abc")
    }

    @Test
    fun apply() = runTest {
        "Number.prototype.toFixed.apply(12.12345)".eval()
            .assertEqualsTo("12")

        "Number.prototype.toFixed.apply(12.12345, [1])".eval()
            .assertEqualsTo("12.1")
    }

    @Test
    fun call() = runtimeTest{
        "Array.prototype.indexOf.call([2, 9, 9], 2)".eval().assertEqualsTo(0L)
    }

    @Test
    fun bind() = runtimeTest{
        "Array.prototype.indexOf.bind([2, 9, 9])(2)".eval().assertEqualsTo(0L)
        "Array.prototype.indexOf.bind([2, 9, 9], 2)()".eval().assertEqualsTo(0L)
        "Array.prototype.indexOf.bind([2, 5, 9, 2], 2)()".eval().assertEqualsTo(0L)
        "Array.prototype.indexOf.bind([2, 5, 9, 2], 2)(3)".eval().assertEqualsTo(3L)
        "Array.prototype.indexOf.bind([2, 5, 9, 2], 2).bind([2, 5, 9, 2], 3)()".eval().assertEqualsTo(3L)
        "Array.prototype.indexOf.bind([2, 5, 9, 2], 2).bind('not rebindable', 3)()".eval().assertEqualsTo(3L)
        "Array.prototype.indexOf.bind([2, 5, 9, 2], 2).call('not rebindable', 3)".eval().assertEqualsTo(3L)
        "Array.prototype.indexOf.bind([2, 5, 9, 2], 2).apply('not rebindable', [3])".eval().assertEqualsTo(3L)
    }

    @Test
    fun async_function() = runTest {
        assertFailsWith<JSError> {
            """
                function notAsync() {
                      let promise = new Promise((resolve) => resolve(123))
                      await promise
                }
                notAsync()
           """.eval()
        }

        """
            function returnPromise() {
                  return new Promise((resolve) => resolve(123))
            }
            await returnPromise()
       """.eval().assertEqualsTo(123L)

        """
            async function asyncFunc() {
              return 123
            }
            
            await asyncFunc()
       """.eval().assertEqualsTo(123L)
    }
}