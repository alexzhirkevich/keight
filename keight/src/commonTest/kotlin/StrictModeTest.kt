import io.github.alexzhirkevich.keight.js.ReferenceError
import kotlin.test.Test
import kotlin.test.assertFailsWith

class StrictModeTest {

    @Test
    fun assign() = runtimeTest {
        assertFailsWith<ReferenceError> {
            "'use strict'; x = 1;".eval(it)
        }
    }
}