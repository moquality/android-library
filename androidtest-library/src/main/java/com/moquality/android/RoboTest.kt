package com.moquality.android

import android.util.Log
import java.lang.reflect.InvocationTargetException

const val TAG = "MQ ROBO"

internal fun <T> Array<T>.asRepeatedSequence() =
        generateSequence(0) { (it + 1) % this.size }.map(this::get)

internal inline fun <T> repeatWithVal(times: Int, seed: T, action: (T) -> T): T {
    var prev = seed
    for (index in 0 until times) {
        prev = action(prev)
    }
    return prev
}

class RoboTest(private val config: RoboConfig) {
    fun run(start: Any, count: Int = 1000): RoboState {
        val (_, state) = repeatWithVal(count, start to RoboState(null, start.javaClass)) { (currentPage, state) ->
            val methods = config.validMethods(state)
            val selected = config.selectMethod(state, methods)
            val args = config.generateArguments(state, selected)

            try {
                Log.i(TAG, "Calling ${state.currentPage.name}.${selected.name}(${args.joinToString(", ")})")

                val nextPage = selected(currentPage, *args.toTypedArray())
                if (nextPage == null) {
                    Log.e(TAG, "Nothing returned from ${selected.name}. Stopping test.")
                    return state
                }

                nextPage to RoboState(state, nextPage.javaClass, selected)
            } catch (err: InvocationTargetException) {
                // TODO: Collect information about test state during errors.
                System.err.println(err.targetException.message)
                err.targetException.printStackTrace()

                currentPage to RoboState(state, currentPage.javaClass, selected, err.targetException)
            }
        }

        return state
    }
}