package io.github.alexzhirkevich.keight

public interface Console {

    public fun verbose(message: Any?)

    public fun info(message: Any?): Unit = verbose(message)

    public fun debug(message: Any?) : Unit = verbose(message)

    public fun warn(message: Any?) : Unit = verbose(message)

    public fun error(message: Any?)
}

internal object DefaultConsole : Console {
    override fun verbose(message: Any?) {
        print(message)
    }

    override fun error(message: Any?) {
        if (message is Throwable){
            message.printStackTrace()
        } else {
            print(message)
        }
    }
}