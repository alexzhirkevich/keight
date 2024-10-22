package io.github.alexzhirkevich.keight

import java.io.Writer

internal fun ScriptIO.asWriter(
    isError : Boolean
) = object : Writer() {
    override fun close() {}

    override fun flush() {}

    override fun write(c: Int) {
        writeAny(c)
    }

    override fun write(str: String) {
        writeAny(str as Any)
    }

    override fun write(cbuf: CharArray) {
        write(cbuf.joinToString(separator = ""))
    }

    override fun write(buf: CharArray, off: Int, len: Int) {
        write(buf.asSequence().drop(off).take(len).joinToString(separator = ""))
    }

    private fun writeAny(value : Any?){
        if (isError){
            err(value)
        } else {
            out(value)
        }
    }
}


internal fun ScriptIO(
    writer: () -> Writer,
    errorWriter : () -> Writer
) = object : ScriptIO{

    override fun out(message: Any?) {
        writer().write(message.toString())
    }

    override fun err(message: Any?) {
        errorWriter().write(message.toString())
    }
}