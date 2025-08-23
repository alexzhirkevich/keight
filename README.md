# Keight

JavaScript runtime for Kotlin Multiplatform. Written entirely in Kotlin. Works on every Kotlin target.

Powers After Effects expressions in [Compottie](https://github.com/alexzhirkevich/compottie) library.

> [!WARNING]  
> Project is experimental and WIP. Some features may not work. No support guarantees.
> Use only for evaluation purposes or on your own risk

# Installation

```toml
[versions]
keight="<version>"

[libraries]
keight = { module = "io.github.alexzhirkevich:keight", version.ref = "keight" }
```

# Usage

## Basic

```kotlin

val engine = JSEngine(JSRuntime(coroutineContext))

val script = engine.compile("const js = 'JS'; 'Hello, ' + js")
val result = script.invoke()?.toKotlin(engine.runtime)
println(result)

// or

val result = engine.evaluate("const js = 'JS'; 'Hello, ' + js")
println(result)

```




