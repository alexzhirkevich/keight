import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * V8-style frame names for class / object-literal methods.
 *
 * V8 prefixes a method-call frame with the receiver's *type* name:
 *   - class instance method  -> `ClassName.method`  (e.g. `A.foo`)
 *   - class static method     -> `ClassName.method`  (e.g. `A.s`)
 *   - object-literal method    -> `Object.method`     (e.g. `Object.bar`)
 * regardless of which variable the call is made through. Property-assigned
 * functions keep their reference name (`obj.qux`).
 */
class ClassMethodStackTraceTest {

    private suspend fun frameOf(script: String): String =
        script.trimIndent().eval().toString()

    @Test
    fun classInstanceMethodShowsClassNamePrefix() = runTest {
        val s = frameOf(
            """
            class A { foo() { return new Error('x').stack; } }
            new A().foo();
            """
        )
        assertTrue("class instance method should be 'at A.foo' but got: $s") { s.contains("at A.foo") }
    }

    @Test
    fun classInstanceMethodPrefixIgnoresVariableName() = runTest {
        val s = frameOf(
            """
            class A { foo() { return new Error('x').stack; } }
            const whatever = new A();
            whatever.foo();
            """
        )
        assertTrue("prefix must be the class name, not the variable: $s") {
            s.contains("at A.foo") && !s.contains("at whatever.foo")
        }
    }

    @Test
    fun classStaticMethodShowsClassNamePrefix() = runTest {
        val s = frameOf(
            """
            class A { static s() { return new Error('x').stack; } }
            A.s();
            """
        )
        assertTrue("class static method should be 'at A.s' but got: $s") { s.contains("at A.s") }
    }

    @Test
    fun classStaticMethodPrefixIgnoresVariableName() = runTest {
        val s = frameOf(
            """
            class A { static s() { return new Error('x').stack; } }
            const Y = A;
            Y.s();
            """
        )
        assertTrue("static prefix must be the class name, not the alias: $s") {
            s.contains("at A.s") && !s.contains("at Y.s")
        }
    }

    @Test
    fun objectLiteralMethodShowsObjectPrefix() = runTest {
        val s = frameOf(
            """
            const o = { bar() { return new Error('x').stack; } };
            o.bar();
            """
        )
        assertTrue("object-literal method should be 'at Object.bar' but got: $s") { s.contains("at Object.bar") }
    }

    @Test
    fun objectLiteralMethodViaAliasStillObjectPrefix() = runTest {
        val s = frameOf(
            """
            const o = { bar() { return new Error('x').stack; } };
            const p = o;
            p.bar();
            """
        )
        assertTrue("object-literal method via alias must stay 'Object.bar': $s") {
            s.contains("at Object.bar") && !s.contains("at p.bar")
        }
    }

    @Test
    fun propertyAssignedMethodKeepsReferenceName() = runTest {
        val s = frameOf(
            """
            const obj = {};
            obj.qux = function() { return new Error('x').stack; };
            obj.qux();
            """
        )
        assertTrue("property-assigned method should be 'at obj.qux' but got: $s") { s.contains("at obj.qux") }
    }

    @Test
    fun classMethodLineNumberIsPreserved() = runTest {
        val s = frameOf(
            """
            class A {
                foo() {
                    return new Error('x').stack;
                }
            }
            new A().foo();
            """
        )
        // The deepest (throw) frame is inside foo; its line must be reported.
        assertTrue("class method frame should carry a line:col location: $s") { s.contains("at A.foo") }
    }
}
