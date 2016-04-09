/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import java.lang.invoke.MethodHandles

/**
 * @author yawkat
 */
class EventBus {
    private data class Subscription(val priority: Int, val handler: (Any) -> Unit) : Comparable<Subscription> {
        override fun compareTo(other: Subscription): Int {
            return priority - other.priority
        }
    }

    private val subscribers: MutableMap<Class<out Any>, MutableList<Subscription>> = hashMapOf()

    fun subscribe(o: Any) {
        for (method in o.javaClass.methods) {
            val subscribe = method.getAnnotation(Subscribe::class.java)
            if (subscribe != null) {
                assert(method.parameterCount == 1)
                val handle = MethodHandles.lookup().unreflect(method).bindTo(o)
                val list = subscribers.getOrPut(method.parameterTypes[0], { arrayListOf() })
                list.add(Subscription(subscribe.priority, { handle.invokeWithArguments(it) }))
                list.sort()
            }
        }
    }

    /**
     * @return `false` if the event was cancelled.
     */
    fun post(o: Any): Boolean {
        try {
            subscribers[o.javaClass]?.forEach { it.handler.invoke(o) }
            return true
        } catch(cancel: CancelEvent) {
            return false
        }
    }
}

object CancelEvent : RuntimeException()

@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe(val priority: Int = 0)