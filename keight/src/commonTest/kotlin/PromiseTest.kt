import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PromiseTest {

    @Test
    fun resolve_reject() = runtimeTest {
        """
            let promise = new Promise((resolve) => resolve(123))
            await promise
        """.eval().assertEqualsTo(123L)

        assertFailsWith<ThrowableValue> {
            """
            let promise = new Promise((_, reject) => reject("failure"))
            await promise
        """.eval()
        }.value.assertEqualsTo("failure")
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
}