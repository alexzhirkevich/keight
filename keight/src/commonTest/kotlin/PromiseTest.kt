import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.measureTime

class PromiseTest {

    @Test
    fun construct() = runtimeTest {
        """
            let promise = new Promise((resolve) => resolve(123))
            await promise
        """.eval().assertEqualsTo(123L)

        assertFailsWith<ThrowableValue> {
            """
            let promise = new Promise((_, reject) => reject("failure"))
            await promise
        """.eval()
        }.value.toString().assertEqualsTo("failure")
    }

    @Test
    fun then() = runtimeTest {
        """
            let promise = new Promise((resolve) => resolve(5)).then(x => x * x)
            await promise
        """.eval().assertEqualsTo(25L)

        """
            let promise = new Promise((resolve) => resolve(5)).then(x => x * x).then(x => x + 1)
            await promise
        """.eval().assertEqualsTo(26L)

        """
            let promise = new Promise((resolve) => { throw "test" }).then(_ =>{}, e => e)
            await promise
        """.eval().assertEqualsTo("test")

    }

    @Test
    fun catch() = runtimeTest {

        """
            let error;
            let promise = new Promise((resolve) => { throw "error"}).catch(e => error = e)
            await promise
            error
        """.eval().assertEqualsTo("error")

        """
            let error;
            let promise = new Promise((resolve) => { throw "error"})
                .catch(e => e).then(e => error = e)
            await promise
            error
        """.eval().assertEqualsTo("error")
    }

    @Test
    fun finally() = runtimeTest {
        """
            let error;
            try {
                let promise = new Promise((resolve) => { throw "error"}).finally(e => error = "finally")
                await promise
            } catch {
            }
            error
        """.eval().assertEqualsTo("finally")
    }

    @Test
    fun static_resolve_reject() = runTest {
        "await Promise.resolve(3)".eval().assertEqualsTo(3L)

        """
            try {
                await Promise.reject(3)
            } catch(x) { res = x }
            res
        """.eval().assertEqualsTo(3L)
    }

    @Test
    fun delay() = runTest {
        measureTime {
            """
            var delay = function(ms) {
                return new Promise((res) => setTimeout(() => res(), ms)) 
            }
            await delay(50)
           """.eval()
        }.let { assertTrue { it.inWholeMilliseconds >= 50L } }
    }

    @Test
    fun error_handle() = runTest {
        """
            async function fetch() {
                throw Error("bad thing happened");
            };
            await fetch().catch((e) => {});
            'Caught';
        """.trimIndent().eval().assertEqualsTo("Caught")
    }
}