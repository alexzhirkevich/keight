package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

public interface Script {
    public suspend operator fun invoke(): JsAny?
}


