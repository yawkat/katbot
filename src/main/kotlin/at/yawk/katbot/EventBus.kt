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

    fun post(o: Any) {
        try {
            subscribers[o.javaClass]?.forEach { it.handler.invoke(o) }
        } catch(cancel: CancelEvent) {
        }
    }
}

object CancelEvent : RuntimeException()

@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe(val priority: Int = 0)