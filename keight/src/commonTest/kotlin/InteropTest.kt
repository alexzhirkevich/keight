//import io.github.alexzhirkevich.keight.js.js
//import io.github.alexzhirkevich.keight.set
//import kotlinx.coroutines.Deferred
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.async
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlin.coroutines.Continuation
//import kotlin.coroutines.coroutineContext
//import kotlin.coroutines.resume
//import kotlin.test.Test
//import kotlin.test.assertTrue
//
//class InteropTest {
//
//    @Test
//    fun lambda()= runtimeTest { runtime ->
//
//        var x = 0L
//
//        runtime.set("test".js()) { x = 2 }
//        "test()".eval(runtime)
//        x.assertEqualsTo(2L)
//
//        runtime.set("test".js()) { a : Long -> x = a }
//        "test(3)".eval(runtime)
//        x.assertEqualsTo(3L)
//
//        runtime.set("test".js())  { a : Long -> a+1 }
//        "test(3)".eval(runtime).assertEqualsTo(4L)
//
//        runtime.set("sum".js(),::sum)
//
//        "sum(test(9), test(4))".eval(runtime).assertEqualsTo(15L)
//    }
//
//    @Test
//    fun suspendFunction() = runtimeTest { runtime ->
//
//        runtime.set("testSuspend", suspend { delay(1); "result" })
//        runtime.set("testInline", suspend { "result" })
//
//        assertTrue {
//            "testSuspend()".eval(runtime) is Deferred<*>
//        }
//        "await testSuspend()".eval(runtime).assertEqualsTo("result")
//
//        assertTrue {
//            "testInline()".eval(runtime) is Deferred<*>
//        }
//
//        "await testInline()".eval(runtime).assertEqualsTo("result")
//    }
//
//    fun sum(a : Long, b : Long) : Long {
//        return a + b
//    }
//}
//
