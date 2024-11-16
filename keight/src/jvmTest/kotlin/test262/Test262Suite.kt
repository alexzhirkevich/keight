package test262

import io.github.alexzhirkevich.keight.JavaScriptEngine
import runtimeTest
import kotlin.test.Test

class Test262Suite {

    @Test
    fun test() = runtimeTest { r ->
        var i = 0
        resources().walk().forEach {
            if (it.isFile) {
                r.reset()
                evalFile("/harness/sta.js", r)
                evalFile("/harness/assert.js", r)
                Test262Case.fromSource(it).test(r)
                println("#${i++}\t ✅ ${it.name}")
            }
        }
//        resources().resolve("language/types/boolean")
//            .listFiles(FileFilter { it.isFile })!!
//            .forEach {
//                Test262Case.fromSource(it).test(r)
//            }
    }
}