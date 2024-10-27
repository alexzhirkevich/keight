import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.JSRuntime
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun constructor_function() = runTest {

        val runtime = JSRuntime(Job())

        """
            function Person(name,age){
                this.name = name
                this.age = age
            }
            const p = new Person('John', 25)
        """.eval(runtime)

        "p.name".eval(runtime).assertEqualsTo("John")
        "p.age".eval(runtime).assertEqualsTo(25L)
        "p.prototype".eval(runtime).assertEqualsTo(Unit)
        "p.__proto__".eval(runtime).assertEqualsTo("Person.prototype".eval(runtime))
    }

    @Test
    fun add_field_to_prototype_constructor() = runTest {
        val runtime = JSRuntime(coroutineContext)

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
    }

    @Test
    fun apply() = runTest {
        "Number.prototype.toFixed.apply(12.12345)".eval()
            .assertEqualsTo("12")

        "Number.prototype.toFixed.apply(12.12345, [1])".eval()
            .assertEqualsTo("12.1")
    }
}