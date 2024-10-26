import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import kotlin.test.Test
import kotlin.test.assertFailsWith

class VariablesTest {

    @Test
    fun multiple_declaration() {
        "var a=1,b=2; a+b".eval().assertEqualsTo(3L)
        assertFailsWith<TypeError> {
            "const a=1,b=2; b++".eval().assertEqualsTo(3L)
        }
    }

    @Test
    fun variable_scopes() {
        """
            var x;
            if(true) {
                x = 5
            }
            x
        """.trimIndent().eval().assertEqualsTo(5L)

        """
            var x = 1;
            if(true){
                let x = 5
            }
            x
        """.trimIndent().eval().assertEqualsTo(1L)


        """
            if(true){
                var x = 5
            }
            x
        """.trimIndent().eval().assertEqualsTo(5L)

        assertFailsWith<ReferenceError> {
            """
                if(true){
                    let x = 5
                }
                x
            """.trimIndent().eval()
        }
        assertFailsWith<ReferenceError> {
            """
                if(true){
                    const x = 5
                }
                x
            """.trimIndent().eval()
        }
    }

    @Test
    fun constMutating() {
        assertFailsWith<TypeError> { "const x = 1; x++".eval() }
    }

    @Test
    fun variable_redeclaration() {
        assertFailsWith<SyntaxError> { "const x = 1; const x = 2".eval() }
        assertFailsWith<SyntaxError> { "const x = 1; let x = 2".eval() }
        assertFailsWith<SyntaxError> { "const x = 1; var x = 2".eval() }
        assertFailsWith<SyntaxError> { "var x = 1; const x = 2".eval() }
        assertFailsWith<SyntaxError> { "var x = 1; let x = 2".eval() }
        assertFailsWith<SyntaxError> { "var x = 1; var x = 2".eval() }
        assertFailsWith<SyntaxError> { "let x = 1; const x = 2".eval() }
        assertFailsWith<SyntaxError> { "let x = 1; var x = 2".eval() }
        assertFailsWith<SyntaxError> { "let x = 1; let x = 2".eval() }

        "const x = 1; { const x = 2 }; x".eval().assertEqualsTo(1L)
        "const x = 1; { let x = 2 }; x".eval().assertEqualsTo(1L)
        assertFailsWith<SyntaxError> {
            "const x = 1; { var x = 2 }; x".eval().assertEqualsTo(1L)
        }
        "var x = 1; { const x = 2 }; x".eval().assertEqualsTo(1L)
        "var x = 1; { let x = 2 }; x".eval().assertEqualsTo(1L)
        assertFailsWith<SyntaxError> {
            "var x = 1; { var x = 2 }; x".eval().assertEqualsTo(1L)
        }
        "let x = 1; { const x = 2 }; x".eval().assertEqualsTo(1L)
        assertFailsWith<SyntaxError> {
            "let x = 1; { var x = 2 }; x".eval().assertEqualsTo(1L)
        }
        "let x = 1; { let x = 2 }; x".eval().assertEqualsTo(1L)
    }

}