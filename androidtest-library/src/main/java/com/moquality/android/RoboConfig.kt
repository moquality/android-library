package com.moquality.android

import java.io.PrintStream
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.floor

interface RoboConfig {
    fun selectMethods(state: List<RoboState>, currentPage: Class<*>): Collection<Method> = currentPage.declaredMethods.asSequence()
            .filter { it.modifiers and Modifier.PUBLIC != 0 }
            .filter { it.modifiers and Modifier.STATIC == 0 }
            .filter { !it.name.startsWith("assert") && !it.name.startsWith("waitFor") && !it.name.startsWith("expect") }
            .toHashSet()

    fun generateArguments(state: List<RoboState>, method: Method): List<Any> = method.generateArguments()

    fun onSuccess(state: List<RoboState>) {}
    fun onError(state: List<RoboState>) {}
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
        val state: Class<*>,
        val method: Method,
        val args: List<Any>,
        val error: Throwable?
) {
    companion object {
    }

    val methodCall by lazy { "${state.simpleName}.${method.name}(${args.joinToString(", ")})" }

    val errorInfo by lazy {
        if (error != null) {
            "${methodCall}: ${error.localizedMessage?.substringBefore("\n")}"
        } else {
            null
        }
    }
}

fun List<RoboState>.printStackTrace(size: Int = 10, out: PrintStream = System.err) {
    asReversed().subList(0, size).forEach {
        out.println(it.errorInfo ?: it.methodCall)
    }
}