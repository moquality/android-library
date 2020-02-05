package com.moquality.android

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.floor

fun genModels(target: Class<*>): Map<String, Model> {
    val models = java.util.HashMap<String, Model>()
    genModels(models, target, HashSet())
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

                                returns = m.returnType.name
                        )

                        genModels(models, m.returnType, visited)

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

    fun getPage(name: String) = pages[name]
    fun hasPage(name: String) = pages.containsKey(name)
}

internal fun Map<String, Model.Method>.select(config: RoboConfig): String {
    var totalLen = 0
    val methodList = this.entries.asSequence()
            .filter { (name, method) -> config.hasPage(method.returns) }
            .flatMap {
                totalLen += it.value.weight
                arrayOf(it.key).asRepeatedSequence()
                        .take(it.value.weight)
            }.toCollection(ArrayList(totalLen))
    return methodList[floor(Math.random() * methodList.size).toInt()]
}

data class Model(var methods: Map<String, Method>) {
    data class Method(var params: Array<Param>, var returns: String, var weight: Int = 1) {
        data class Param(var type: String, var valid: Array<Any>? = null)
    }
}