package io.github.alexzhirkevich.keight

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Per-task call stack. In JS, every asynchronous task (a microtask / promise
 * callback / timer handler) executes on its own, isolated call stack, exactly
 * like V8 gives each task its own stack. We model that by storing the stack as
 * a [kotlin.coroutines.CoroutineContext] element so that each concurrently-running coroutine that
 * executes JS code gets its own independent list of frames instead of mutating
 * the shared root runtime stack.
 *
 * When this element is absent (e.g. synchronous top-level execution), the
 * runtime's own [ScriptRuntime.callStack] is used as a fallback.
 */
public class CallStackElement(
    public val stack: MutableList<CallFrame> = mutableListOf(),
    /**
     * Snapshot of the parent (asynchronous) task's full call stack, taken at the
     * moment this task was scheduled. Models V8's async long stacks: each async
     * continuation keeps the stack of the task that scheduled it. Frames are
     * stored most-recent-call-LAST, ending with the frame that performed the
     * `await`/scheduling call.
     */
    public val asyncParent: List<CallFrame> = emptyList(),
) : AbstractCoroutineContextElement(CallStackElement) {
    public companion object Key : CoroutineContext.Key<CallStackElement>
}

/**
 * Build the async-parent stack snapshot for a child task scheduled from [context]:
 * the full chain of the current task's frames (its own async parent + its live
 * stack, copied so later mutations don't leak into the snapshot).
 */
private fun asyncParentOf(context: CoroutineContext): List<CallFrame> {
    val el = context[CallStackElement] ?: return emptyList()
    return el.asyncParent + el.stack.toList()
}

/**
 * Compared to the original version, this will open an independent stack record.
 *
 * Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a [Job].
 * The coroutine is cancelled when the resulting job is [cancelled][Job.cancel].
 *
 * The coroutine context is inherited from a [CoroutineScope]. Additional context elements can be specified with [context] argument.
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [Dispatchers.Default] is used.
 * The parent job is inherited from a [CoroutineScope] as well, but it can also be overridden
 * with a corresponding [context] element.
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other start options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,
 * the coroutine [Job] is created in _new_ state. It can be explicitly started with [start][Job.start] function
 * and will be started implicitly on the first invocation of [join][Job.join].
 *
 * Uncaught exceptions in this coroutine cancel the parent job in the context by default
 * (unless [CoroutineExceptionHandler] is explicitly specified), which means that when `launch` is used with
 * the context of another coroutine, then any uncaught exception leads to the cancellation of the parent coroutine.
 *
 * See [newCoroutineContext] for a description of debugging facilities that are available for a newly created coroutine.
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code which will be invoked in the context of the provided scope.
 **/
public suspend fun CoroutineScope.launchFormStack(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    // Capture the parent (asynchronous) task's stack from the *active* continuation
    // context (which carries the CallStackElement added by an enclosing
    // withContext/async), not from the scope's `coroutineContext` property that
    // does not see nested context elements.
    val parent = asyncParentOf(currentCoroutineContext())
    return launch(context + CallStackElement(asyncParent = parent), start, block)
}

/**
 * Compared to the original version, this will open an independent stack record.
 *
 * Creates a coroutine and returns its future result as an implementation of [Deferred].
 * The running coroutine is cancelled when the resulting deferred is [cancelled][Job.cancel].
 * The resulting coroutine has a key difference compared with similar primitives in other languages
 * and frameworks: it cancels the parent job (or outer scope) on failure to enforce *structured concurrency* paradigm.
 * To change that behaviour, supervising parent ([SupervisorJob] or [supervisorScope]) can be used.
 *
 * Coroutine context is inherited from a [CoroutineScope], additional context elements can be specified with [context] argument.
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [Dispatchers.Default] is used.
 * The parent job is inherited from a [CoroutineScope] as well, but it can also be overridden
 * with corresponding [context] element.
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,
 * the resulting [Deferred] is created in _new_ state. It can be explicitly started with [start][Job.start]
 * function and will be started implicitly on the first invocation of [join][Job.join], [await][Deferred.await] or [awaitAll].
 *
 * @param block the coroutine code.
 */
public suspend fun <T> CoroutineScope.asyncFormStack(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    // See launchFormStack: capture the parent stack from the active continuation.
    val parent = asyncParentOf(currentCoroutineContext())
    return async(context + CallStackElement(asyncParent = parent), start, block)
}

/**
 * Compared to the original version, this will open an independent stack record.
 *
 * Calls the specified suspending block with a given coroutine context, suspends until it completes, and returns
 * the result.
 *
 * The resulting context for the [block] is derived by merging the current [coroutineContext] with the
 * specified [context] using `coroutineContext + context` (see [CoroutineContext.plus]).
 * This suspending function is cancellable. It immediately checks for cancellation of
 * the resulting context and throws [CancellationException] if it is not [active][CoroutineContext.isActive].
 *
 * Calls to [withContext] whose [context] argument provides a [CoroutineDispatcher] that is
 * different from the current one, by necessity, perform additional dispatches: the [block]
 * can not be executed immediately and needs to be dispatched for execution on
 * the passed [CoroutineDispatcher], and then when the [block] completes, the execution
 * has to shift back to the original dispatcher.
 *
 * Note that the result of `withContext` invocation is dispatched into the original context in a cancellable way
 * with a **prompt cancellation guarantee**, which means that if the original [coroutineContext]
 * in which `withContext` was invoked is cancelled by the time its dispatcher starts to execute the code,
 * it discards the result of `withContext` and throws [CancellationException].
 *
 * The cancellation behaviour described above is enabled if and only if the dispatcher is being changed.
 * For example, when using `withContext(NonCancellable) { ... }` there is no change in dispatcher and
 * this call will not be cancelled neither on entry to the block inside `withContext` nor on exit from it.
 */
public suspend fun <T> withContextFormStack(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T = withContext(
    context + CallStackElement(asyncParent = asyncParentOf(currentCoroutineContext())),
    block
)