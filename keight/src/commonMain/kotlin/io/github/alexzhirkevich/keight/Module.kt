package io.github.alexzhirkevich.keight

internal interface Module : Script {

    override val runtime : ModuleRuntime

    suspend fun invokeIfNeeded()
}