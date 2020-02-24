package com.moquality.android

import java.io.PrintStream
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.floor

interface RoboConfig {
    fun selectMethods(state: RoboState): List<Method> = state.currentPage.declaredMethods.asSequence()
            .filter { it.modifiers and Modifier.PUBLIC != 0 }
            .filter { it.modifiers and Modifier.STATIC == 0 }
            .filter { !it.name.startsWith("assert") && !it.name.startsWith("waitFor") && !it.name.startsWith("expect") }
            .toList()

    fun generateArguments(state: RoboState, method: Method): List<Any> = method.generateArguments()
}

internal fun Method.generateArguments() = this.parameterTypes.asSequence()
        .map {
            when (it.name) {
                "int", "long", "short", "double", "float" -> fixNumber(it.name, Math.random() * 10000)
                "byte", "char" -> fixNumber(it.name, Math.random() * 256)
                "java.lang.String" -> toPrintable(
                        ByteArray((Math.random() * 512).toInt()) {
                            floor(Math.random() * 256).toByte()
                        })
                "boolean" -> Math.random() > .5

                else -> error("Unknown argument type: $it")
            }
        }
        .toCollection(ArrayList(this.parameterTypes.size))

fun fixNumber(type: String, gen: Double) = gen.javaClass.methods.find { it.name == "${type}Value" }?.invoke(gen)
        ?: error("Couldn't fix number: $type")

internal fun toPrintable(bytes: ByteArray) = bytes.asSequence()
        .map { ((it % (126 - 32)) + 32).toChar() }
        .joinToString("")

data class RoboState(val previous: RoboState?, val currentPage: Class<*>, val method: Method? = null, val args: List<Any> = emptyList(), val error: Throwable? = null) {
    val originMethodCall = if (previous != null) {
        "${previous.currentPage.simpleName}.${method?.name}(${args.joinToString(", ")})"
    } else {
        null
    }

    val errorInfo = if (error != null) {
            "${originMethodCall}: ${error.localizedMessage?.substringBefore("\n")}"
        } else {
            null
        }

    fun printStackTrace(size: Int = 10, out: PrintStream = System.err) {
        if (size == 0) {
            return
        }

        out.println(errorInfo ?: originMethodCall)
        previous?.printStackTrace(size - 1, out)
    }
}