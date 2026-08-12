import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Async long-stack-trace tests.
 *
 * Verifies that an error thrown inside an asynchronous continuation (a Promise
 * `.then`/`.catch` callback) carries the stack of the task that scheduled it,
 * mirroring V8's `async` stack frames.
 */
class AsyncStackTraceTest {

    @Test
    fun thenCallbackTracesBackToScheduler() = runTest {
        val result = """
            function a() {
                return Promise.resolve().then(b);
            }
            function b() {
                throw new Error("boom");
            }
            let stack = null;
            try {
                await a();
            } catch (e) {
                stack = e.stack;
            }
            stack;
        """.trimIndent().eval()
        val s = result.toString()

        assertTrue("stack should contain the throwing frame 'b'") { s.contains("b") }
        assertTrue("stack should trace back across the async boundary ('async')") {
            s.contains("async")
        }
        assertTrue("stack should trace back to the scheduling function 'a'") {
            s.contains("a")
        }
        // deepest frame (b) must appear before the async continuation frames
        val bIdx = s.indexOf("b")
        val asyncIdx = s.indexOf("async")
        assertTrue("frame 'b' should precede the 'async' continuation (bIdx=$bIdx, asyncIdx=$asyncIdx)") {
            bIdx in 0..<asyncIdx
        }
    }

    @Test
    fun nestedAsyncChainsMultipleLevels() = runTest {
        val result = """
            function l1() { return Promise.resolve().then(l2); }
            function l2() { return Promise.resolve().then(l3); }
            function l3() { throw new Error("deep-boom"); }
            let stack = null;
            try { await l1(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()

        assertTrue("should contain the throw site 'l3'") { s.contains("l3") }
        assertTrue("should contain async 'l2'") { s.contains("l2") }
        assertTrue("should contain async 'l1'") { s.contains("l1") }

        // Frames mirror V8: the deepest frame names the throwing function at the
        // throw site, and each async continuation collapses into a single
        // `at async <handler> (call-site)` frame carrying (:line:col).
        assertTrue("deepest frame should be 'at l3 (line:col)'") { s.contains("at l3") }
        assertTrue("continuation should be 'at async l2 (line:col)'") { s.contains("at async l2") }
        assertTrue("outer continuation should be 'at async l1 (line:col)'") { s.contains("at async l1") }

        // The throw-site frame must point at the line where `new Error` is thrown
        // (content line 3), not the function's definition.
        assertTrue("l3 frame should point at throw line 3") { s.contains("at l3 (:3:") }
    }

    @Test
    fun synchronousErrorHasNoAsyncFrames() = runTest {
        val result = """
            function a() { return b(); }
            function b() { throw new Error("sync-boom"); }
            let stack = null;
            try { a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()

        assertTrue("should contain 'b'") { s.contains("b") }
        assertTrue("should contain 'a'") { s.contains("a") }
        assertTrue("synchronous stack should NOT contain 'async' marker") {
            !s.contains("async")
        }
    }
}
