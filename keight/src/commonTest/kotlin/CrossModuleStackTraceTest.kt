import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Cross-module stack trace tests.
 *
 * Tests error stack traces that span across multiple compiled scripts (modules).
 * Verifies that fileName, function names, and call chains are correctly tracked
 * when functions from different modules call each other.
 */
class CrossModuleStackTraceTest {

    // ========== Basic cross-module error propagation ==========

    @Test
    fun errorInModuleShowsModuleName() = runTest {
        engineTest {
            it.compile(
                """
                    export function throwError() {
                        return new Error('from module').stack;
                    }
                """.trimIndent(),
                "errorModule.js"
            )
            val result = it.evaluate("""
                import { throwError } from "errorModule.js"
                throwError();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'from module'") { s.contains("from module") }
            assertTrue("stack should contain 'throwError'") { s.contains("throwError") }
        }
    }

    @Test
    fun errorInModuleShowsScriptName() = runTest {
        engineTest {
            it.compile(
                """
                    export function getErrorStack() {
                        return new Error('mod error').stack;
                    }
                """.trimIndent(),
                "stackModule.js"
            )
            val result = it.evaluate("""
                import { getErrorStack } from "stackModule.js"
                getErrorStack();
            """.trimIndent())
            val s = result.toString()
            assertTrue(
                "stack should contain 'stackModule.js' but got: $s"
            ) { s.contains("stackModule.js") }
        }
    }

    // ========== Cross-module call chains ==========

    @Test
    fun stackTraceCrossModuleChain() = runTest {
        engineTest {
            it.compile(
                """
                    export function moduleFunc() {
                        return new Error('cross-module').stack;
                    }
                """.trimIndent(),
                "moduleA.js"
            )
            val result = it.evaluate("""
                function localFunc() {
                    // Dynamically resolve the module function
                    return moduleFunc();
                }
                import { moduleFunc } from "moduleA.js"
                localFunc();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'cross-module'") { s.contains("cross-module") }
            assertTrue("stack should contain 'moduleFunc'") { s.contains("moduleFunc") }
            assertTrue("stack should contain 'localFunc'") { s.contains("localFunc") }
        }
    }

    @Test
    fun stackTraceMultiModuleChain() = runTest {
        engineTest {
            it.compile(
                "export function funcA() { return funcB(); }",
                "modA.js"
            )
            it.compile(
                """
                    import { funcB } from "modC.js"
                    export function funcA2() { return funcB(); }
                """.trimIndent(),
                "modB.js"
            )
            it.compile(
                "export function funcB() { return new Error('multi-mod').stack; }",
                "modC.js"
            )
            val result = it.evaluate("""
                import { funcA2 } from "modB.js"
                funcA2();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'multi-mod'") { s.contains("multi-mod") }
            assertTrue("stack should contain 'funcA2'") { s.contains("funcA2") }
            assertTrue("stack should contain 'funcB'") { s.contains("funcB") }
        }
    }

    // ========== Cross-module with different error types ==========

    @Test
    fun referenceErrorFromModule() = runTest {
        engineTest {
            it.compile(
                """
                    export function accessUndefined() {
                        try {
                            return notDefinedInModule;
                        } catch(e) {
                            return e.stack;
                        }
                    }
                """.trimIndent(),
                "refModule.js"
            )
            val result = it.evaluate("""
                import { accessUndefined } from "refModule.js"
                accessUndefined();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'ReferenceError' or 'not defined'") {
                s.contains("ReferenceError") || s.contains("is not defined") || s.contains("not defined")
            }
            assertTrue("stack should contain 'accessUndefined'") { s.contains("accessUndefined") }
        }
    }

    @Test
    fun typeErrorFromModule() = runTest {
        engineTest {
            it.compile(
                """
                    export function callNonFunction() {
                        try {
                            var x = 42;
                            x();
                        } catch(e) {
                            return e.stack;
                        }
                    }
                """.trimIndent(),
                "typeModule.js"
            )
            val result = it.evaluate("""
                import { callNonFunction } from "typeModule.js"
                callNonFunction();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'TypeError'") { s.contains("TypeError") }
            assertTrue("stack should contain 'callNonFunction'") { s.contains("callNonFunction") }
        }
    }

    // ========== Module exports and re-exports with errors ==========

    @Test
    fun stackTraceThroughReexportedFunction() = runTest {
        engineTest {
            it.compile(
                "export function deep() { return new Error('re-exported').stack; }",
                "original.js"
            )
            it.compile(
                "export { deep } from 'original.js'",
                "reexport.js"
            )
            val result = it.evaluate("""
                import { deep } from 'reexport.js'
                deep();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 're-exported'") { s.contains("re-exported") }
            assertTrue("stack should contain 'deep'") { s.contains("deep") }
        }
    }

    @Test
    fun stackTraceThroughAggregatingExport() = runTest {
        engineTest {
            it.compile(
                """
                    export function util() {
                        return new Error('aggregate').stack;
                    }
                """.trimIndent(),
                "utilMod.js"
            )
            it.compile(
                "export { util } from 'utilMod.js'",
                "aggregate.js"
            )
            val result = it.evaluate("""
                import { util } from 'aggregate.js'
                function wrapper() { return util(); }
                wrapper();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'aggregate'") { s.contains("aggregate") }
            assertTrue("stack should contain 'util'") { s.contains("util") }
            assertTrue("stack should contain 'wrapper'") { s.contains("wrapper") }
        }
    }

    // ========== Cross-module with classes ==========

    @Test
    fun stackTraceClassFromModule() = runTest {
        engineTest {
            it.compile(
                """
                    export class ModuleClass {
                        doError() {
                            return new Error('module class').stack;
                        }
                    }
                """.trimIndent(),
                "classModule.js"
            )
            val result = it.evaluate("""
                import { ModuleClass } from "classModule.js"
                function caller() {
                    return new ModuleClass().doError();
                }
                caller();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'module class'") { s.contains("module class") }
            assertTrue("stack should contain 'doError'") { s.contains("doError") }
        }
    }

    @Test
    fun stackTraceClassExtendsAcrossModule() = runTest {
        engineTest {
            it.compile(
                """
                    export class BaseClass {
                        errorMethod() {
                            return new Error('cross extend').stack;
                        }
                    }
                """.trimIndent(),
                "baseModule.js"
            )
            val result = it.evaluate("""
                import { BaseClass } from "baseModule.js"
                class LocalClass extends BaseClass {}
                function localCaller() {
                    return new LocalClass().errorMethod();
                }
                localCaller();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'cross extend'") { s.contains("cross extend") }
            assertTrue("stack should contain 'errorMethod'") { s.contains("errorMethod") }
        }
    }

    // ========== CommonJS cross-module errors ==========

    @Test
    fun stackTraceCommonJSModule() = runTest {
        engineTest {
            it.compile(
                """
                    module.exports.getError = function() {
                        return new Error('commonjs').stack;
                    }
                """.trimIndent(),
                "cjsModule.js"
            )
            val result = it.evaluate("""
                var mod = require('cjsModule.js');
                mod.getError();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'commonjs'") { s.contains("commonjs") }
        }
    }

    @Test
    fun stackTraceCommonJSAndESMMix() = runTest {
        engineTest {
            it.compile(
                """
                    module.exports.helper = function() {
                        return new Error('mixed require').stack;
                    }
                    export function esmHelper() {
                        return new Error('mixed import').stack;
                    }
                """.trimIndent(),
                "mixedModule.js"
            )
            val cjsResult = it.evaluate("""
                var mod = require('mixedModule.js');
                function cjsCaller() { return mod.helper(); }
                cjsCaller();
            """.trimIndent())
            val cjsStack = cjsResult.toString()
            assertTrue("cjs stack should contain 'mixed require'") { cjsStack.contains("mixed require") }

            val esmResult = it.evaluate("""
                import { esmHelper } from "mixedModule.js"
                function esmCaller() { return esmHelper(); }
                esmCaller();
            """.trimIndent())
            val esmStack = esmResult.toString()
            assertTrue("esm stack should contain 'mixed import'") { esmStack.contains("mixed import") }
            assertTrue("esm stack should contain 'esmCaller'") { esmStack.contains("esmCaller") }
        }
    }

    // ========== Module initialization errors ==========

    @Test
    fun errorDuringModuleEvaluation() = runTest {
        engineTest {
            // Module evaluation is lazy - errors occur when imported/evaluated, not when compiled.
            // The ReferenceError for undefinedGlobalVariable happens at eval time.
            it.compile(
                """
                    var x = undefinedGlobalVariable;
                    export var y = 1;
                """.trimIndent(),
                "initErrorModule.js"
            )
            // Error should occur when the module is actually evaluated via import
            assertFailsWith<ReferenceError> {
                it.evaluate("import { y } from 'initErrorModule.js'")
            }
        }
    }

    // ========== Error thrown from module caught in main ==========

    @Test
    fun errorThrownFromModuleCaughtInMain() = runTest {
        engineTest {
            it.compile(
                """
                    export function throwRefError() {
                        undefinedModuleVar;
                    }
                """.trimIndent(),
                "throwMod.js"
            )
            val result = it.evaluate("""
                var caught = null;
                try {
                    import { throwRefError } from "throwMod.js"
                    throwRefError();
                } catch(e) {
                    caught = e.name;
                }
                caught;
            """.trimIndent())
            assertTrue("should catch error from module") {
                result.toString().contains("ReferenceError")
            }
        }
    }

    // ========== Cross-module with eval ==========

    @Test
    fun stackTraceThroughEvalInModule() = runTest {
        engineTest {
            it.compile(
                """
                    export function evalError() {
                        try {
                            eval("undefinedEvalVar");
                        } catch(e) {
                            return e.stack;
                        }
                    }
                """.trimIndent(),
                "evalMod.js"
            )
            val result = it.evaluate("""
                import { evalError } from "evalMod.js"
                evalError();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'evalError'") { s.contains("evalError") }
            assertTrue("stack should contain 'ReferenceError' or 'not defined'") {
                s.contains("ReferenceError") || s.contains("is not defined") || s.contains("not defined")
            }
        }
    }

    // ========== Cross-module error with nested function calls ==========

    @Test
    fun stackTraceModuleFunctionCallingLocalFunction() = runTest {
        engineTest {
            it.compile(
                """
                    export function moduleEntry() {
                        return new Error('entry').stack;
                    }
                """.trimIndent(),
                "entryMod.js"
            )
            val result = it.evaluate("""
                import { moduleEntry } from "entryMod.js"
                function local1() { return local2(); }
                function local2() { return moduleEntry(); }
                local1();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'entry'") { s.contains("entry") }
            assertTrue("stack should contain 'moduleEntry'") { s.contains("moduleEntry") }
            assertTrue("stack should contain 'local1'") { s.contains("local1") }
            assertTrue("stack should contain 'local2'") { s.contains("local2") }
        }
    }

    @Test
    fun stackTraceLocalFunctionCallingModuleFunction() = runTest {
        engineTest {
            it.compile(
                """
                    export function deepModuleFunc() {
                        return new Error('deep mod').stack;
                    }
                """.trimIndent(),
                "deepMod.js"
            )
            val result = it.evaluate("""
                import { deepModuleFunc } from "deepMod.js"
                function l1() {
                    function l2() {
                        function l3() {
                            return deepModuleFunc();
                        }
                        return l3();
                    }
                    return l2();
                }
                l1();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'deep mod'") { s.contains("deep mod") }
            assertTrue("stack should contain 'deepModuleFunc'") { s.contains("deepModuleFunc") }
            assertTrue("stack should contain 'l3'") { s.contains("l3") }
            assertTrue("stack should contain 'l2'") { s.contains("l2") }
            assertTrue("stack should contain 'l1'") { s.contains("l1") }
        }
    }

    // ========== Module script name in error properties ==========

    @Test
    fun moduleErrorHasScriptNameInProperties() = runTest {
        engineTest {
            it.compile(
                """
                    export function createError() {
                        var e = new Error('prop check');
                        return e.fileName + ':' + (e.lineNumber != null);
                    }
                """.trimIndent(),
                "propMod.js"
            )
            val result = it.evaluate("""
                import { createError } from "propMod.js"
                createError();
            """.trimIndent())
            val s = result.toString()
            assertTrue("error fileName should contain 'propMod.js' but got: $s") {
                s.contains("propMod.js")
            }
            assertTrue("error should have a lineNumber") { s.contains("true") }
        }
    }

    // ========== Error in default export function ==========

    @Test
    fun stackTraceDefaultExportFunction() = runTest {
        engineTest {
            it.compile(
                """
                    export default function defaultFn() {
                        return new Error('default export').stack;
                    }
                """.trimIndent(),
                "defaultMod.js"
            )
            val result = it.evaluate("""
                import defaultFn from "defaultMod.js"
                defaultFn();
            """.trimIndent())
            val s = result.toString()
            assertTrue("stack should contain 'default export'") { s.contains("default export") }
            assertTrue("stack should contain 'defaultFn'") { s.contains("defaultFn") }
        }
    }

    // ========== Module reset clears stack ==========

    @Test
    fun moduleResetClearsCallStack() = runTest {
        engineTest {
            it.compile(
                """
                    export function test() { return 42; }
                """.trimIndent(),
                "resetMod.js"
            )
            it.evaluate("import { test } from 'resetMod.js'; test();").assertEqualsTo(42L)
            it.reset()
            // After reset, module variables should be gone
            assertFailsWith<ReferenceError> {
                it.evaluate("test()")
            }
            // Module should still be available for re-import
            it.evaluate("import { test } from 'resetMod.js'; test();").assertEqualsTo(42L)
        }
    }
}
