package com.moquality.android

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Modifier
import kotlin.math.floor

fun genModels(target: Class<*>): Map<String, Model> {
    val models = java.util.HashMap<String, Model>()
    genModels(models, target, hashSetOf(Any::class.java))
    return models
}

fun genModels(models: MutableMap<String, Model>, target: Class<*>, visited: MutableSet<Class<*>>) {
    if (visited.contains(target)) {
        return
    }
    visited.add(target)

    val model = Model(
            methods = target.declaredMethods.asSequence()
                    .filter { it.modifiers and Modifier.PUBLIC != 0 }
                    .filter { it.modifiers and Modifier.STATIC == 0 }
                    .map { m ->
                        val method = Model.Method(
                                params = m.parameterTypes.map { Model.Method.Param(type = it.name) }.toTypedArray(),

                                returns = if (m.returnType.name != "java.lang.Object") {
                                    m.returnType.name
                                } else {
                                    "generic"
                                }
                        )

                        if (m.returnType.name != "java.lang.Object") {
                            genModels(models, m.returnType, visited)
                        }

                        m.name to method
                    }
                    .toMap()
    )

    models[target.name] = model
}

class RoboConfig {
    private val pages: MutableMap<String, Model>

    constructor(pages: Map<String, Model>) {
        this.pages = pages.toMutableMap()
    }

    constructor(config: String) : this(HashMap()) {
        val gson = GsonBuilder().run {
            setPrettyPrinting()
        }.create()
        pages.putAll(gson.fromJson(config, object : TypeToken<HashMap<String, Model>>() {}.type))
    }

    fun hasPage(name: String) = pages.containsKey(name)
    fun getPage(name: String) = pages[name]
}

internal fun Map<String, Model.Method>.select(config: RoboConfig): String {
    var totalLen = 0
    val methodList = this.entries.asSequence()
            .filter { (_, method) -> method.returns == "generic" || config.hasPage(method.returns) }
            .flatMap { (name, method) ->
                totalLen += method.weight
                arrayOf(name).asRepeatedSequence()
                        .take(method.weight)
            }.toCollection(ArrayList(totalLen))
    return methodList[floor(Math.random() * methodList.size).toInt()]
}

/**
 * toArgumentList creates an equivalent list of argument values.
 */
internal fun Array<Model.Method.Param>.toArgumentList() = this.asSequence()
        .map {
            val validValues = it.valid
            if (validValues != null && validValues.isNotEmpty()) {
                return@map validValues[floor(Math.random() * validValues.size).toInt()]
            }

            @Suppress("IMPLICIT_CAST_TO_ANY")
            when (it.type) {
                "int", "long", "short", "double", "float" -> Math.random() * 10000
                "byte", "char" -> Math.random() * 256
                "java.lang.String" -> toPrintable(
                        ByteArray((Math.random() * 512).toInt()) {
                            floor(Math.random() * 256).toByte()
                        })
                "boolean" -> Math.random() > .5

                else -> error("Unknown argument type: $it")
            }
        }
        .mapIndexed { i, v ->
            when (val type = this[i].type) {
                "int", "long", "short", "double", "float", "char", "byte" ->
                    v.javaClass.methods.find { it.name == "${type}Value" }?.invoke(v)
                else -> v
            }
        }
        .toCollection(ArrayList(this.size))

data class Model(var methods: Map<String, Method>) {
    data class Method(var params: Array<Param>, var returns: String, var weight: Int = 1, var after: String? = null) {
        data class Param(var type: String, var valid: Array<Any>? = null)
    }
}