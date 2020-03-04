package com.moquality.android

import android.util.Log
import java.lang.reflect.InvocationTargetException

const val TAG = "MQ ROBO"

internal inline fun <T> repeatWithVal(times: Int, seed: T, action: (T) -> T): T {
    var prev = seed
    for (index in 0 until times) {
        prev = action(prev)
    }
    return prev
}

class RoboTest(private val config: RoboConfig) {
    fun run(start: Any, count: Int = 1000): List<RoboState> {
        val state = mutableListOf<RoboState>()
        repeatWithVal(count, start) { currentPage ->
            val methods = config.selectMethods(state, currentPage.javaClass)
            val selected = methods.random() // TODO: Pick a method properly.
            val args = config.generateArguments(state, selected)

            try {
                Log.i(TAG, "Calling ${currentPage.javaClass.simpleName}.${selected.name}(${args.joinToString(", ")})")

                val nextPage = selected(currentPage, *args.toTypedArray())
                if (nextPage == null) {
                    Log.e(TAG, "Nothing returned from ${selected.name}. Stopping test.")
                    return state
                }

                state.add(RoboState(currentPage.javaClass, selected, args, null))
                config.onSuccess(state)

                nextPage
            } catch (err: InvocationTargetException) {
                state.add(RoboState(currentPage.javaClass, selected, args, err.targetException))
                config.onError(state)

                currentPage
            }
        }

        return state
    }
}