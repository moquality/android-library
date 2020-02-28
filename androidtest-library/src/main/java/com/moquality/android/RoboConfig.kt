package com.moquality.android

import java.io.PrintStream
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.floor

interface RoboConfig {
    fun selectMethods(state: RoboState): Collection<Method> = state.currentPage.declaredMethods.asSequence()
            .filter { it.modifiers and Modifier.PUBLIC != 0 }
            .filter { it.modifiers and Modifier.STATIC == 0 }
            .filter { !it.name.startsWith("assert") && !it.name.startsWith("waitFor") && !it.name.startsWith("expect") }
            .toHashSet()

    fun generateArguments(state: RoboState, method: Method): List<Any> = method.generateArguments()

    fun onSuccess(state: RoboState) {}
    fun onError(state: RoboState) {}
}

internal fun Method.generateArguments() = this.parameterTypes.asSequence()
        .map {
            when (it.name) {
                "int", "long", "short", "double", "float" -> ((Math.random() * 10000) as java.lang.Double).toNumberType(it.name)
                "byte", "char" -> ((Math.random() * 256) as java.lang.Double).toNumberType(it.name)
                "java.lang.String" -> toPrintable(
                        ByteArray((Math.random() * 512).toInt()) {
                            floor(Math.random() * 256).toByte()
                        })
                "boolean" -> Math.random() > .5

                else -> error("Unknown argument type: $it")
            }
        }
        .toCollection(ArrayList(this.parameterTypes.size))

internal fun java.lang.Double.toNumberType(type: String) = this.javaClass.methods.find { it.name == "${type}Value" }?.invoke(this)
        ?: error("Couldn't convert number: $type")

internal fun toPrintable(bytes: ByteArray) = bytes.asSequence()
        .map { ((it % (126 - 32)) + 32).toChar() }
        .joinToString("")

data class RoboState(
        val previous: RoboState?,
        val currentPage: Class<*>,
        val method: Method? = null,
        val args: List<Any> = emptyList(),
        val error: Throwable? = null
) {
    val originMethodCall by lazy {
        if (previous != null) {
            "${previous.currentPage.simpleName}.${method?.name}(${args.joinToString(", ")})"
        } else {
            null
        }
    }

    val errorInfo by lazy {
        if (error != null) {
            "${originMethodCall}: ${error.localizedMessage?.substringBefore("\n")}"
        } else {
            null
        }
    }

    fun printStackTrace(size: Int = 10, out: PrintStream = System.err) {
        if (size == 0) {
            return
        }

        out.println(errorInfo ?: originMethodCall)
        previous?.printStackTrace(size - 1, out)
    }
}