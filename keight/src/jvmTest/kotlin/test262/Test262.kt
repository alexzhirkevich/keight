package test262

import eval
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsAny
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals


fun test262() = File("src/jvmTest/test262/test")


suspend fun harness(name : String, runtime: JSRuntime) {
    File("src/jvmTest/test262/harness").resolve(name).readText().eval(runtime)
}

 class Test262Case(
    private val file: File,
    private val source: String,
    val harnessFiles: List<String>,
    private val expectedError: String?,
    private val hasEarlyError: Boolean,
    private val flags: Set<String>,
    val features: Set<String>
) {

    suspend fun test(runtime: JSRuntime) {
        val result = try {
            val s = if (hasFlag("onlyStrict")) {
                "'use strict';\n$source"
            } else {
                source
            }
            s.eval(runtime)
            null
        } catch (t: Throwable) {
            t
        }

        if (isNegative && result != null) {
            if (expectedError != null) {
                if (result is JSError) {
                    val name = result.get("name", runtime)

                    assertEquals(expectedError, name, result.stackTraceToString())
                } else {
                    throw result
                }
            }
        } else {
            if (result != null) {
                val msg = if (result is ThrowableValue && result.value is JsAny) {
                    if (result.value.contains("message", runtime)) {
                        result.value.get("message", runtime)
                    } else {
                        JSStringFunction.toString(result.value, runtime)
                    }
                } else {
                    null
                }
                throw Exception("Failed test $file (MSG: $msg)", result)
            }
        }
    }

    fun hasFlag(flag: String): Boolean {
        return flags.contains(flag)
    }

    val isNegative: Boolean
        get() = expectedError != null

    companion object {

        private const val FLAG_RAW = "raw"

        private val YAML: Yaml = Yaml()

        @Throws(IOException::class)
        fun fromSource(testFile: File): Test262Case {
            val testSource = testFile.readText()

            val harnessFiles: MutableList<String> = ArrayList()

            val metadata: Map<String, Any>

            if (testSource.indexOf("/*---") != -1) {
                val metadataStr =
                    testSource.substring(
                        testSource.indexOf("/*---") + 5, testSource.indexOf("---*/")
                    )
                metadata = YAML.load(metadataStr)
            } else {
                System.err.format(
                    "WARN: file '%s' doesnt contain /*--- ... ---*/ directive",
                    testFile.path
                )
                metadata = HashMap()
            }

            var expectedError: String? = null
            var isEarly = false
            if (metadata.containsKey("negative")) {
                val negative = metadata["negative"] as Map<String, String>?
                expectedError = negative!!["type"]
                isEarly = "early" == negative["phase"]
            }

            val flags: MutableSet<String> = HashSet()
            if (metadata.containsKey("flags")) {
                flags.addAll((metadata["flags"] as Collection<String>?)!!)
            }

            val features: MutableSet<String> = HashSet()
            if (metadata.containsKey("features")) {
                features.addAll((metadata["features"] as Collection<String>?)!!)
            }

            if (flags.contains(FLAG_RAW) && metadata.containsKey("includes")) {
                System.err.format(
                    "WARN: case '%s' is flagged as 'raw' but also has defined includes%n",
                    testFile.path
                )
            } else {
                // present by default harness files
                harnessFiles.add("assert.js")
                harnessFiles.add("sta.js")
                harnessFiles.add("doneprintHandle.js")

                if (metadata.containsKey("includes")) {
                    harnessFiles.addAll((metadata["includes"] as List<String>?)!!)
                }
            }

            return Test262Case(
                testFile, testSource, harnessFiles, expectedError, isEarly, flags, features
            )
        }
    }
}