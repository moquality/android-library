package com.moquality.android

import android.util.Log
import java.lang.reflect.InvocationTargetException
import kotlin.math.floor

const val TAG = "MQ ROBO"

internal fun generateArgs(params: Array<Model.Method.Param>) = params.map {
    if (it.valid != null && it.valid!!.isNotEmpty()) {
        val v = it.valid!![floor(Math.random() * it.valid!!.size).toInt()]
        return@map v
    }

    when (it.type) {
        "int", "long", "short", "double", "float" -> Math.random() * 10000
        "byte", "char" -> Math.random() * 256
        "java.lang.String" -> toPrintable(
                ByteArray((Math.random() * 512).toInt()) {
                    floor(Math.random() * 256).toByte()
                })

        else -> error("Unknown argument type: $it")
    }
}.toTypedArray()

internal fun toPrintable(bytes: ByteArray) = bytes.asSequence().map { ((it % (126 - 32)) + 32).toChar() }.joinToString("")

internal fun selectMethod(methods: Map<String, Model.Method>): String {
    val methodList = methods.entries.flatMap {
        arrayOf(it.key).asRepeatedSequence()
                .take(it.value.weight)
                .asIterable()
    }
    return methodList[floor(Math.random() * methodList.size).toInt()]
}

internal inline fun <T> Array<T>.asRepeatedSequence() =
        generateSequence(0) { (it + 1) % this.size }.map(this::get)

internal inline fun <T> repeatWithVal(times: Int, seed: T, action: (T) -> T): T {
    var prev = seed
    for (index in 0 until times) {
        prev = action(prev)
        if (prev == null) {
            return prev
        }
    }
    return prev
}

class RoboTest(private val config: RoboConfig) {
    private val pages = HashMap<String, Any>()

    fun registerPageObject(obj: Any): RoboTest {
        pages[obj.javaClass.simpleName] = obj
        return this
    }

    fun run(start: String, count: Int = 1000) {
        repeatWithVal(count, pages[start]) { currentPage ->
            if (currentPage == null) {
                Log.e(TAG, "Wound up on a null page. Stopping test.")
                return
            }

            val currentPageName = currentPage.javaClass.simpleName

            val methods = config.getPage(currentPageName)?.methods
                    ?: error("Page $currentPageName not found")
            val selected = selectMethod(methods)

            // TODO: Handle method overloading.
            val next = currentPage.javaClass.methods.find { it.name == selected }
                    ?: error("Couldn't find $currentPageName.$selected")
            val args = generateArgs(methods.getValue(selected).params)
                    .mapIndexed { i, v ->
                        val type = methods.getValue(selected).params[i].type
                        when (type) {
                            "int", "long", "short", "double", "float", "char", "byte" ->
                                v.javaClass.methods.find { it.name == "${type}Value" }?.invoke(v)
                            else -> v
                        }
                    }
                    .toTypedArray()

            try {
                Log.i(TAG, "Calling ${next.name}(${args.joinToString(", ")})")
                next(currentPage, *args)
            } catch (err: InvocationTargetException) {
                // TODO: Collect information about test state during errors.
                System.err.println(err.targetException.message)
                err.targetException.printStackTrace()
                currentPage
            }
        }
    }
}

//class MoQuality private constructor() {
//    var config: RoboConfig? = null
//    var pageObjects: MutableList<Any> = ArrayList()
//    var objectMap: MutableMap<String?, Any> = HashMap()
//
//    private fun log(message: String) {
//        Log.i(TAG, message)
//    }
//
//    @Throws(IOException::class)
//    fun takeScreenshot(name: String) {
//        log("Saving screenshot $name")
//        val capture = Screenshot.capture()
//        capture.name = name
//        capture.format = Bitmap.CompressFormat.PNG
//        try {
//            capture.process()
//        } catch (e: IOException) {
//            e.printStackTrace()
//            throw e
//        }
//    }
//
//    fun startRoboTest(config: Map<String?, String?>?) {
//        log("Starting Robo Test")
//        // TODO: Implement Android Robo
//        val roboConfig = RoboConfig(config)
//        startPageObjectRobo(roboConfig)
//    }
//
//    fun startPageObjectRobo(config: RoboConfig?) {
//        this.config = config
//        val currentPage = currentPage
//        var count = 1000
//        while (count > 0) {
//            if (currentPage != null) {
//                val pageMethods = this.config!!.pom[currentPage.javaClass.canonicalName]!!
//                val mSignature = query(currentPage, pageMethods)
//                Log.d(TAG, "query selected $mSignature")
//                val methodName: String = mSignature.removeAt(0)
//                val mParamTypes: MutableList<Class<*>?> = ArrayList()
//                try {
//                    for (mParam in mSignature) {
//                        mParamTypes.add(getClass(mParam))
//                    }
//                    val m = currentPage.javaClass.getMethod(methodName, *mParamTypes.toTypedArray())
//                    m.invoke(currentPage, *generateParams(mSignature))
//                } catch (e: NoSuchMethodException) {
//                    e.printStackTrace()
//                } catch (e: IllegalAccessException) {
//                    e.printStackTrace()
//                } catch (e: InvocationTargetException) {
//                    e.printStackTrace()
//                }
//            } else {
//                Log.e(TAG, "Current Page is NULL. Cannot proceed")
//                break
//            }
//            count--
//        }
//
//        //        Log.d(TAG, "Page Object = "+currentPage.getClass().getCanonicalName());
//        //        Method[] methods = currentPage.getClass().getDeclaredMethods();
//        //        for(Method m : methods) {
//        //            Log.d(TAG, "- Method="+m.getName());
//        //            try {
//        //                Object[] args = getOrGenerateParams(m.getParameterTypes());
//        //                m.invoke(currentPage, args);
//        //            } catch (IllegalAccessException e) {
//        //                e.printStackTrace();
//        //            } catch (InvocationTargetException e) {
//        //                e.printStackTrace();
//        //            }
//        //        }
//    }
//
//    private fun getClass(klass: String?): Class<*>? {
//        when (klass) {
//            "int" -> return Int::class.javaPrimitiveType
//            "java.lang.Integer" -> return Int::class.java
//            "java.lang.String" -> return String::class.java
//        }
//        return null
//    }
//
//    private fun generateParams(classes: List<String?>): Array<Any> {
//        val params: MutableList<Any> = ArrayList()
//        val random = Random()
//        val strings = arrayOf("+", "-", "*", "/")
//        for (klass in classes) {
//            when (klass) {
//                "int", "java.lang.Integer" -> params.add(random.nextInt(10))
//                "java.lang.String" -> params.add(strings[random.nextInt(strings.size)])
//            }
//        }
//        return params.toTypedArray()
//    }
//
//    private fun query(currentPage: Any, pageMethods: Map<String, Map<String, String>>): List<String?> { // TODO: Change random to querying server.
//        val methods = pageMethods.keys
//        var i = Random().nextInt(methods.size)
//        var method: String? = null
//        val it = methods.iterator()
//        while (i >= 0) {
//            method = it.next()
//            i--
//        }
//        val mSignature: MutableList<String?> = ArrayList()
//        mSignature.add(method)
//        val params = pageMethods[method]!!["params"]!!.split(",").toTypedArray()
//        mSignature.addAll(Arrays.asList(*params))
//        return mSignature
//    }
//
//    private fun getOrGenerateParams(parameterTypes: Array<Class<*>>): Array<Any> {
//        val objects: MutableList<Any> = ArrayList()
//        for (pType in parameterTypes) {
//            Log.d(TAG, "-- pType class=" + pType.canonicalName)
//            when (pType.canonicalName) {
//                "java.lang.String" -> objects.add("0")
//                "int" -> objects.add(1)
//            }
//        }
//        return objects.toTypedArray()
//    }
//
//    private val currentPage: Any?
//        private get() {
//            val mapping = config!!.mapping
//            val matchers: Set<String> = mapping.keys
//            for (matcher in matchers) {
//                when (matcher) {
//                    "*" -> {
//                        val klass = mapping[matcher]
//                        return objectMap[klass]
//                    }
//                    else -> {
//                    }
//                }
//            }
//            return null
//        }
//
//    fun registerPageObjects(vararg objects: Any): MoQuality {
//        for (`object` in objects) {
//            pageObjects.add(`object`)
//            objectMap[`object`.javaClass.canonicalName] = `object`
//            objectMap[`object`.javaClass.simpleName] = `object`
//        }
//        return this
//    }
//
//    companion object {
//        private const val TAG = "MQ"
//        private var instance: MoQuality? = null
//        fun get(): MoQuality? {
//            if (instance == null) {
//                instance = MoQuality()
//            }
//            return instance
//        }
//    }
//}