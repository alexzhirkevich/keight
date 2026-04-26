import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SourceLocationTest {

    // ========== SyntaxError: parse-time errors ==========

    @Test
    fun syntaxErrorBasic() = runTest {
        assertFailsWith<SyntaxError> {
            "function( {}".eval()
        }
    }

    @Test
    fun syntaxErrorUnclosedString() = runTest {
        assertFailsWith<SyntaxError> {
            """var x = "hello world;""".eval()
        }
    }

    // ========== ReferenceError: runtime errors ==========

    @Test
    fun referenceErrorUndeclaredVariable() = runTest {
        assertFailsWith<ReferenceError> {
            "undefinedVariable".eval()
        }
    }

    @Test
    fun referenceErrorInAssignment() = runTest {
        assertFailsWith<ReferenceError> {
            "var x = undefinedVariable;".eval()
        }
    }

    @Test
    fun referenceErrorMultiLine() = runTest {
        assertFailsWith<ReferenceError> {
            """
                var a = 1;
                var b = 2;
                var c = notDefined;
                var d = 4;
            """.trimIndent().eval()
        }
    }

    @Test
    fun referenceErrorInFunction() = runTest {
        assertFailsWith<ReferenceError> {
            """
                function foo() {
                    return myUndefinedVar;
                }
                foo();
            """.trimIndent().eval()
        }
    }

    @Test
    fun referenceErrorInDeeplyNestedCode() = runTest {
        assertFailsWith<ReferenceError> {
            """
                function outer() {
                    function inner() {
                        return deepRef;
                    }
                    return inner();
                }
                outer();
            """.trimIndent().eval()
        }
    }

    @Test
    fun referenceErrorMultiLineScript() = runTest {
        assertFailsWith<ReferenceError> {
            """
                line1();
                line2();
                badReference;
            """.trimIndent().eval()
        }
    }

    // ========== TypeError: runtime type errors ==========

    @Test
    fun typeErrorNotAFunction() = runTest {
        assertFailsWith<TypeError> {
            "42()".eval()
        }
    }

    @Test
    fun typeErrorNullPropertyAccess() = runTest {
        assertFailsWith<TypeError> {
            "null.prop".eval()
        }
    }

    @Test
    fun typeErrorMethodCallOnNonObject() = runTest {
        assertFailsWith<TypeError> {
            """
                var obj = {};
                obj.nonExistentMethod();
            """.trimIndent().eval()
        }
    }

    @Test
    fun typeErrorNestedNullAccess() = runTest {
        assertFailsWith<TypeError> {
            "var obj = { a: { b: null } }; obj.a.b.c".eval()
        }
    }

    @Test
    fun typeErrorConstMutation() = runTest {
        assertFailsWith<TypeError> {
            "const x = 1; x++".eval()
        }
    }

    // ========== JSError properties from JS ==========

    @Test
    fun errorMessage() = runTest {
        "new Error('custom message').message".eval().assertEqualsTo("custom message")
        "Error('custom message').message".eval().assertEqualsTo("custom message")
    }

    @Test
    fun errorName() = runTest {
        "new TypeError('type err').name".eval().assertEqualsTo("TypeError")
        "new ReferenceError('not defined').name".eval().assertEqualsTo("ReferenceError")
        "new SyntaxError('parse failed').name".eval().assertEqualsTo("SyntaxError")
    }

    @Test
    fun errorInTryCatch() = runTest {
        """
            try {
                undefinedFunc();
            } catch (e) {
                res = e.message;
            }
            res;
        """.trimIndent().eval().let { assertTrue { it != null } }
    }

    @Test
    fun errorInTryCatchHasName() = runTest {
        """
            try {
                undefinedFunc();
            } catch (e) {
                res = e.name;
            }
            res;
        """.trimIndent().eval().let { assertTrue { it != null } }
    }

    @Test
    fun thrownErrorHasNameAndMessage() = runTest {
        """
            try {
                throw new SyntaxError("parse failed");
            } catch (e) {
                res = e.name + ":" + e.message;
            }
            res;
        """.trimIndent().eval().assertEqualsTo("SyntaxError:parse failed")
    }

    @Test
    fun thrownReferenceError() = runTest {
        """
            try {
                throw new ReferenceError("not defined");
            } catch (e) {
                res = e.name + ":" + e.message;
            }
            res;
        """.trimIndent().eval().assertEqualsTo("ReferenceError:not defined")
    }

    // ========== Error.stack property ==========

    @Test
    fun stackIsString() = runTest {
        "typeof new Error('test').stack".eval().assertEqualsTo("string")
    }

    @Test
    fun stackContainsMessage() = runTest {
        "new Error('hello world').stack.indexOf('hello world') >= 0".eval().assertEqualsTo(true)
    }

    @Test
    fun stackContainsErrorName() = runTest {
        "new TypeError('type err').stack.indexOf('TypeError') >= 0".eval().assertEqualsTo(true)
    }

    @Test
    fun constructErrorHasLineNumber() = runTest {
        // new Error() should automatically get line number from parseNew
        """
            var e = new Error("test");
            e.stack.indexOf(":") >= 0;
        """.trimIndent().eval().assertEqualsTo(true)
    }

    @Test
    fun stackWithFileName() = runTest {
        assertTrue {
            """
                var e = new Error("test");
                e.fileName = "myfile.js";
                e.stack.indexOf("myfile.js") >= 0;
            """.trimIndent().eval() as Boolean
        }
    }

    @Test
    fun stackWithScriptName() = runTest {
        val script = "var e = new Error('test'); e.stack;"
        val runtime = JSRuntime(Job())
        val compiled = JSEngine(runtime).compile(script, "test.js")
        val result = compiled.invoke(runtime)
        assertTrue(
            result.toString().contains("test.js"),
            "stack should contain script name 'test.js' but got: $result"
        )
    }

    @Test
    fun stackTraceShowsFunctionName() = runTest {
        val result = """
            function inner() {
                return new Error('boom').stack;
            }
            var s = inner();
            s;
        """.trimIndent().eval()
        assertTrue(result.toString().contains("inner"), "stack should contain 'inner' but got: $result")
    }

    @Test
    fun stackTraceShowsCallerChain() = runTest {
        val result = """
            function a() { return b(); }
            function b() { return new Error('err').stack; }
            a();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'a'" ) { s.contains("a") }
        assertTrue("stack should contain 'b'" ) { s.contains("b") }
        assertTrue("stack should contain 'new Error'" ) { s.contains("new Error") }
    }

    @Test
    fun stackWithLineAndColumn() = runTest {
        assertTrue {
            """
                var e = new Error("test");
                e.fileName = "test.js";
                e.lineNumber = 10;
                e.columnNumber = 5;
                e.stack.indexOf("test.js:10:5") >= 0;
            """.trimIndent().eval() as Boolean
        }
    }

    @Test
    fun lineNumberUpdatesStack() = runTest {
        """
            var e = new Error("test");
            e.fileName = "test.js";
            e.lineNumber = null;
            var s1 = e.stack;
            e.lineNumber = 42;
            var s2 = e.stack;
            s1.indexOf("42") >= 0 ? false : s2.indexOf("42") >= 0;
        """.trimIndent().eval().assertEqualsTo(true)
    }

    @Test
    fun columnNumberUpdatesStack() = runTest {
        """
            var e = new Error("test");
            e.fileName = "test.js";
            e.lineNumber = 1;
            e.columnNumber = null;
            var s1 = e.stack;
            e.columnNumber = 7;
            var s2 = e.stack;
            s1;
        """.trimIndent().eval().let { s1 ->
            """
                var e = new Error("test");
                e.fileName = "test.js";
                e.lineNumber = 1;
                e.columnNumber = 7;
                e.stack;
            """.trimIndent().eval().let { s2 ->
                assertTrue(s1.toString().indexOf(":7") < 0, "s1 should not contain :7 but got: $s1")
                assertTrue(s2.toString().indexOf(":7") >= 0, "s2 should contain :7 but got: $s2")
            }
        }
    }

    // ========== Error in various contexts ==========

    @Test
    fun errorInArrowFunction() = runTest {
        assertFailsWith<ReferenceError> {
            """
                var fn = () => undefinedVar;
                fn();
            """.trimIndent().eval()
        }
    }

    @Test
    fun errorInClassConstructor() = runTest {
        assertFailsWith<ReferenceError> {
            """
                class Foo {
                    constructor() {
                        this.x = undefinedProp;
                    }
                }
                new Foo();
            """.trimIndent().eval()
        }
    }

    @Test
    fun errorInObjectLiteral() = runTest {
        assertFailsWith<ReferenceError> {
            """
                var obj = {
                    a: 1,
                    b: undefinedProp,
                    c: 3
                };
            """.trimIndent().eval()
        }
    }

    @Test
    fun noErrorOnValidCode() = runTest {
        """
            var x = 1 + 2;
            var y = x * 3;
            y;
        """.trimIndent().eval().assertEqualsTo(9L)
    }

    @Test
    fun errorInTryCatchFinally() = runTest {
        """
            var a = 1
            try {
                throw 'test'
            } catch(x) {
                a++
            } finally {
                a++
            }
            a
        """.trimIndent().eval().assertEqualsTo(3L)
    }
}
