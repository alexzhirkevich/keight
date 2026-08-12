import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Comprehensive multi-scenario tests for V8-style async long stack traces.
 *
 * Verifies that an error thrown inside an asynchronous continuation (a Promise
 * `.then` / `.catch` / `.finally` callback, or across an `await`) carries the
 * stack of the task that scheduled it, mirroring V8's `async` stack frames.
 *
 * Each scenario mirrors the style of the other stack-trace tests: the script is
 * evaluated, converted to a string, and inspected with `contains` assertions
 * for the throwing message, the involved function frames, the `async` marker,
 * and the precise `:line:col` of the deepest (throw-site) frame.
 */
class AsyncStackTraceScenariosTest {

    // ========== .catch handlers ==========

    @Test
    fun catchHandlerKeepsOriginalStack() = runTest {
        val result = """
            function a() { return Promise.reject(new Error("c1")).catch(b); }
            function b(e) { throw e; }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'c1'") { s.contains("c1") }
        // The error is created before `.catch` attaches, so rethrowing it keeps
        // the original stack (V8 behaviour) -> deepest frame is the creation site.
        assertTrue("stack should trace back to the scheduling function 'a'") { s.contains("at a") }
        assertTrue("deepest frame should be at the Error creation site (:1:15)") {
            s.contains("at a (:1:15)")
        }
    }

    @Test
    fun catchRethrowKeepsOriginalStack() = runTest {
        val result = """
            function a() { return Promise.reject(new Error("x")).catch(e => { throw e; }); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'x'") { s.contains("x") }
        assertTrue("deepest frame should be at the Error creation site (:1:15)") {
            s.contains("at a (:1:15)")
        }
    }

    @Test
    fun thenBothArgsRethrowKeepsOriginalStack() = runTest {
        val result = """
            function a(){ return Promise.reject(new Error("rej")).then(v => v, e => { throw e; }); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'rej'") { s.contains("rej") }
        assertTrue("deepest frame should be at the Error creation site (:1:14)") {
            s.contains("at a (:1:14)")
        }
    }

    // ========== .finally handlers ==========

    @Test
    fun finallyHandlerThrowsCarriesBothFrames() = runTest {
        val result = """
            function a() { return Promise.resolve(1).finally(b); }
            function b() { throw new Error("fin"); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'fin'") { s.contains("fin") }
        assertTrue("stack should contain the throwing finally handler 'b'") { s.contains("at b") }
        assertTrue("stack should trace back across the async boundary ('async')") { s.contains("async") }
        assertTrue("throwing finally handler frame should be at (:2:11)") { s.contains("at b (:2:11)") }
        assertTrue("scheduling frame should be 'at async a' at (:1:18)") { s.contains("at async a (:1:18)") }
    }

    // ========== Real async/await chains ==========

    @Test
    fun asyncAwaitChainTracesThroughAwaitSites() = runTest {
        val result = """
            async function a() { await b(); }
            async function b() { await c(); }
            async function c() { throw new Error("deep"); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'deep'") { s.contains("deep") }
        assertTrue("stack should contain 'c'") { s.contains("at c") }
        assertTrue("stack should contain 'b'") { s.contains("at async b") }
        assertTrue("stack should contain 'a'") { s.contains("at async a") }
        assertTrue("throw site frame should be at (:3:13)") { s.contains("at c (:3:13)") }
        assertTrue("await b() frame should be at (:1:13)") { s.contains("at async b (:1:13)") }
        assertTrue("await a() frame should be at (:5:7)") { s.contains("at async a (:5:7)") }
    }

    @Test
    fun awaitRejectedPromisePointsAtCreation() = runTest {
        val result = """
            async function a() { await Promise.reject(new Error("r")); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'r'") { s.contains("r") }
        assertTrue("stack should trace back to 'a'") { s.contains("at a") }
        assertTrue("deepest frame should be at the Error creation site (:1:17)") {
            s.contains("at a (:1:17)")
        }
    }

    @Test
    fun asyncFunctionSyncThrowSingleFrame() = runTest {
        val result = """
            async function a() { throw new Error("asy"); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'asy'") { s.contains("asy") }
        assertTrue("stack should contain 'a'") { s.contains("at a") }
        assertTrue("throw site frame should be at (:1:13)") { s.contains("at a (:1:13)") }
    }

    @Test
    fun topLevelAwaitThrowSingleFrame() = runTest {
        val result = """
            async function main(){ throw new Error("tl"); }
            let stack = null;
            try { await main(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'tl'") { s.contains("tl") }
        assertTrue("stack should contain 'main'") { s.contains("at main") }
        assertTrue("throw site frame should be at (:1:12)") { s.contains("at main (:1:12)") }
    }

    @Test
    fun deepAsyncFiveLevels() = runTest {
        val result = """
            async function a(){ await b(); }
            async function b(){ await c(); }
            async function c(){ await d(); }
            async function d(){ await e(); }
            async function e(){ throw new Error("lvl5"); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'lvl5'") { s.contains("lvl5") }
        assertTrue("stack should contain all five levels a..e") {
            s.contains("at e") && s.contains("at async d") && s.contains("at async c") &&
                s.contains("at async b") && s.contains("at async a")
        }
        assertTrue("throw site frame should be 'at e' at (:5:12)") { s.contains("at e (:5:12)") }
        assertTrue("await a() frame should be at (:7:7)") { s.contains("at async a (:7:7)") }
    }

    // ========== .then callbacks ==========

    @Test
    fun thenReturningRejectedPromiseShowsAnonymousFrame() = runTest {
        val result = """
            function a() { return Promise.resolve().then(() => Promise.reject(new Error("rp"))); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'rp'") { s.contains("rp") }
        // Arrow callbacks are anonymous -> the throwing frame shows <anonymous>.
        assertTrue("stack should contain the anonymous arrow frame") { s.contains("<anonymous>") }
        assertTrue("stack should trace back across the async boundary ('async')") { s.contains("async") }
        assertTrue("anonymous throwing frame should be at (:1:28)") { s.contains("at <anonymous> (:1:28)") }
        assertTrue("scheduling frame should be 'at async a' at (:1:17)") { s.contains("at async a (:1:17)") }
    }

    @Test
    fun arrowHandlerThrowShowsAnonymousFrame() = runTest {
        val result = """
            function a() { return Promise.resolve().then(() => { throw new Error("arrow"); }); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'arrow'") { s.contains("arrow") }
        assertTrue("stack should contain the anonymous arrow frame") { s.contains("<anonymous>") }
        assertTrue("stack should trace back across the async boundary ('async')") { s.contains("async") }
        assertTrue("anonymous throwing frame should be at (:1:28)") { s.contains("at <anonymous> (:1:28)") }
        assertTrue("scheduling frame should be 'at async a' at (:1:17)") { s.contains("at async a (:1:17)") }
    }

    @Test
    fun thenChainThreeWithThrowInMiddle() = runTest {
        val result = """
            function a(){ return Promise.resolve().then(b).then(c).then(d); }
            function b(){ return 1; }
            function c(){ throw new Error("mid"); }
            function d(){ return 2; }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'mid'") { s.contains("mid") }
        assertTrue("stack should contain the throwing handler 'c'") { s.contains("at c") }
        assertTrue("stack should contain the scheduling function 'a'") { s.contains("at async a") }
        assertTrue("throw site frame should be at (:3:10)") { s.contains("at c (:3:10)") }
        assertTrue("scheduling frame should be 'at async a' at (:1:21)") { s.contains("at async a (:1:21)") }
    }

    // ========== Mixed sync + async ==========

    @Test
    fun mixedSyncCallerIntoAsyncContinuation() = runTest {
        val result = """
            function a() { return b(); }
            function b() { return Promise.resolve().then(c); }
            function c() { throw new Error("m"); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'm'") { s.contains("m") }
        assertTrue("stack should contain 'c'") { s.contains("at c") }
        assertTrue("stack should contain 'b'") { s.contains("at async b") }
        assertTrue("stack should contain 'a'") { s.contains("at async a") }
        assertTrue("throw site frame should be at (:3:11)") { s.contains("at c (:3:11)") }
        assertTrue("scheduling frame should be 'at async a' at (:5:7)") { s.contains("at async a (:5:7)") }
    }

    // ========== new Promise executor ==========

    @Test
    fun promiseExecutorThrowShowsNewPromiseFrame() = runTest {
        val result = """
            function a() { return new Promise((res, rej) => { throw new Error("exec"); }); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the message 'exec'") { s.contains("exec") }
        assertTrue("stack should contain the 'new Promise' executor frame") { s.contains("new Promise") }
        assertTrue("stack should trace back across the async boundary ('async')") { s.contains("async") }
        assertTrue("executor frame should be at (:1:28)") { s.contains("at new Promise (:1:28)") }
        assertTrue("await a() frame should be at (:3:7)") { s.contains("at async a (:3:7)") }
    }

    // ========== Promise.all ==========

    @Test
    fun promiseAllResolvesWithValuesInOrder() = runTest {
        val result = """
            let r = null;
            try { r = await Promise.all([Promise.resolve(1), Promise.resolve(2)]); } catch (e) { r = "ERR"; }
            r.toString();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("Promise.all should resolve to an array of the resolved values") { s.contains("1") && s.contains("2") }
        assertTrue("resolved values should keep input order (1 before 2)") { s.indexOf("1") < s.indexOf("2") }
    }

    @Test
    fun promiseAllAcceptsPlainValues() = runTest {
        // Non-promise members are treated as already-resolved (ECMAScript
        // semantics), so Promise.all must NOT surface a "is not a Promise" error.
        val result = """
            let r = null;
            try { r = await Promise.all([1, 2]); } catch (e) { r = "ERR:" + e.message; }
            r.toString();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("Promise.all should accept non-promise values, got: $s") {
            s.contains("1") && s.contains("2")
        }
        assertTrue("Promise.all should NOT report 'is not a Promise', got: $s") {
            !s.contains("is not a Promise")
        }
    }

    @Test
    fun promiseAllRejectionCarriesAsyncStack() = runTest {
        val result = """
            async function a(){ await Promise.all([Promise.resolve(1), Promise.reject(new Error("pa"))]); }
            let stack = null;
            try { await a(); } catch (e) { stack = e.stack; }
            stack;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain the rejection message 'pa'") { s.contains("pa") }
        assertTrue("stack should trace back to the scheduling function 'a'") { s.contains("at a") }
        // The rejection reason is created synchronously inside `a`, so the deepest
        // frame is the Error creation site at the `new Error` call.
        assertTrue("deepest frame should be the Error creation site 'at a (:1:29)'") {
            s.contains("at a (:1:29)")
        }
    }
}
