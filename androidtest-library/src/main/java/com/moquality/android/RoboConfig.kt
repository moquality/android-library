package com.moquality.android

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.HashMap

@RequiresApi(Build.VERSION_CODES.N)
fun genModels(target: Class<*>): Map<String, Model> {
    val models = java.util.HashMap<String, Model>()
    genModels(models, target, HashSet())
    return models
}

@RequiresApi(Build.VERSION_CODES.N)
fun genModels(models: MutableMap<String, Model>, target: Class<*>, visited: MutableSet<Class<*>>) {
    if (visited.contains(target)) {
        return
    }
    visited.add(target)

    val model = Model(
            methods = Arrays.stream(target.declaredMethods)
                    .filter { it.modifiers and Modifier.PUBLIC != 0 }
                    .filter { it.modifiers and Modifier.STATIC == 0 }
                    .map { m ->
                        val method = Model.Method(
                                params = Arrays.stream(m.parameterTypes)
                                        .map { Model.Method.Param(type = it.name) }
                                        .toArray { arrayOfNulls<Model.Method.Param>(it) },

                                returns = m.returnType.simpleName
                        )

                        genModels(models, m.returnType, visited)

                        m.name to method
                    }
                    .collect<HashMap<String, Model.Method>>(
                            ::HashMap,
                            { m, (name, method) -> m[name] = method },
                            { obj, m -> obj.putAll(m) }
                    )
    )

    models[target.simpleName] = model
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

    fun getPage(name: String): Model? = pages[name]
}

data class Model(var methods: Map<String, Method>) {
    data class Method(var params: Array<Param>, var returns: String, var weight: Int = 1) {
        data class Param(var type: String, var valid: Array<Any>? = null)
    }
}