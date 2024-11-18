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
                val test = Test262Case.fromSource(it)
                if ("cross-realm" !in test.features && "Proxy" !in test.features) {
                    test.test(r)
                    println("#${i++}\t ✅ PASSED ${it.name}")
                } else {
                    println("#${i++}\t ⚠\uFE0F IGNORED ${it.name}")
                }
            }
        }
//        resources().resolve("language/types/boolean")
//            .listFiles(FileFilter { it.isFile })!!
//            .forEach {
//                Test262Case.fromSource(it).test(r)
//            }
    }
}