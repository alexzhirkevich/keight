import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SyntaxTest {

    @Test
    fun newline_property_chaining() = runTest {
        """
            Math.imul(3,4).toString()
        """.trimIndent().eval().assertEqualsTo("12")

        """
            Math
                .imul(3,4)
                .toString()
        """.trimIndent().eval().assertEqualsTo("12")

        """
            Math

                .imul(3,4)

                .toString()
        """.trimIndent().eval().assertEqualsTo("12")

        """
            Math

                .imul(3,4)

                .toString()
                ;
        """.trimIndent().eval().assertEqualsTo("12")


        assertFailsWith<SyntaxError> {
            """
            Math
                .imul(3,4);
                .toString()
        """.trimIndent().eval()
        }
    }


    @Test
    fun typeOf() = runTest {
        "typeof(1)".eval().assertEqualsTo("number")
        "typeof(null)".eval().assertEqualsTo("object")
        "typeof(undefined)".eval().assertEqualsTo("undefined")
        "typeof('str')".eval().assertEqualsTo("string")
        "typeof('str') + 123".eval().assertEqualsTo("string123")

        "typeof 1".eval().assertEqualsTo("number")
        "typeof null".eval().assertEqualsTo("object")
        "typeof undefined".eval().assertEqualsTo("undefined")

        "typeof 1===1".eval().assertEqualsTo(false)
        "typeof 1>2".eval().assertEqualsTo(false)

        "let x = 1; typeof ++x".eval().assertEqualsTo("number")
        "let x = 1; typeof x++".eval().assertEqualsTo("number")
        assertFailsWith<SyntaxError> {
            "let x = 1; typeof x = 2".eval()
        }
        assertFailsWith<SyntaxError> {
            "let x = 1; typeof x += 2".eval()
        }
    }

    @Test
    fun tryCatch()= runTest  {
        """
            let error = undefined
            try {
                let x = null
                x.test = 1
            } catch(x) {
                error = x.message
            }
            error
        """.trimIndent().eval().assertEqualsTo("Cannot set properties of null")

        """
            let error = undefined
            try {
                throw 'test'
            } catch(x) {
                error = x
            }
            error
        """.trimIndent().eval().assertEqualsTo("test")

        """
            let a = 1
            try {
                throw 'test'
            } catch(x) {
                a++
            } finally {
                a++
            }
            a
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            let a = 1
            try {
               
            } catch(x) {
                a++
            } finally {
                a++
            }
            a
        """.trimIndent().eval().assertEqualsTo(2L)
    }

    @Test
    fun operator_precedence_and_associativity() = runTest {
        "1 + 2 ** 3 * 4 / 5 >> 6".eval().assertEqualsTo(0L)

        "2 ** 3 / 3 ** 2".eval().assertEqualsTo(0.8888888888888888)
        // (2 ** 3) / (3 ** 2)

        "4 / 3 / 2".eval().assertEqualsTo(0.6666)
    }

    @Test
    fun comments() = runTest {
        """
            // comment
            "string"
        """.eval().assertEqualsTo("string")

        """
            "string"
            // comment
            // comment
            // comment
        """.eval().assertEqualsTo("string")

        """
            /* comment */
            "string"
        """.eval().assertEqualsTo("string")

        """
            "string"
            /* comment */
            /* comment */
            /* comment */
        """.eval().assertEqualsTo("string")

        """
            /* multiline
            comment */
            "string"
             /* multiline
            comment */
        """.eval().assertEqualsTo("string")

        """
            "string"
            /* multiline
            comment */
        """.eval().assertEqualsTo("string")

        "'str' + /* commment */ 'ing'".eval().assertEqualsTo("string")

        val func = "/**/function /**/name/**/(/**/arg1,/**/arg2/**/){/**/}".eval() as JSFunction
        func.name.assertEqualsTo("name")
        func.parameters.let {
            it[0].name.assertEqualsTo("arg1")
            it[1].name.assertEqualsTo("arg2")
        }

        """
            /**/var i
            /**/for/**/(/**/i =/**/ 0/**/ ;/**/i<2/**/ /**/ ;/**/i++/**/){/**/}
            /**/i/**/
        """.trimIndent().eval().assertEqualsTo(2L)

        """
            var i = 0
            /**/while/**/(/**/i < 2/**/){/**/i++/**/}
            i
        """.trimIndent().eval().assertEqualsTo(2L)

        val runtime = JSRuntime(coroutineContext)
        """
            let /**/ obj = /**/{ /**/name/**/ : /**/'test'/**/}
        """.trimIndent().eval(runtime).let {
            it as JSObject
            it.get("name",runtime).toString().assertEqualsTo("test")
        }
    }

    @Test
    fun ternary_operator() = runTest {
        "false ? 1 : 2".eval().assertEqualsTo(2L)
        "true ? 1 : 2".eval().assertEqualsTo(1L)
    }

    @Test
    fun spread_operator() = runTest {
        """
            let a = [3,4];
            [1,2, ...a, 5,6]
        """.eval().assertEqualsTo(listOf(1L,2L,3L,4L,5L,6L))
    }

    @Test
    fun recursion_with_ternary_return()= runTest {

        """
           function fib(n) {
               return n < 2 ? n : fib(n - 1) + fib(n - 2);
           }

           fib(7)
        """.trimIndent().eval().assertEqualsTo(13L)
    }
}