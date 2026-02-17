package me.leonunes.games.common

import kotlin.reflect.KClass

interface Id<T : Any, K : Any> {
    fun get() : K
}

class IdImp<T : Any, K : Any>(private val klass: KClass<T>, private val value: K) : Id<T, K> {

    override fun get(): K {
        return value
    }

    override fun equals(other: Any?): Boolean {
        val o = other as? IdImp<*, *> ?: return false
        return o.klass == klass && o.value == value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

inline fun <reified T : Any, K : Any> K.asId() : Id<T, K> = IdImp(T::class, this)
inline fun <reified T : Any> Int.asId() : Id<T, Int> = IdImp(T::class, this)
inline fun <reified T : Any> String.asId() : Id<T, String> = IdImp(T::class, this)
