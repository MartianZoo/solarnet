package dev.martianzoo.pets.util

/** Returns the initialized value, computing it on the first call. */
@Suppress("NOTHING_TO_INLINE") public inline operator fun <T> Lazy<T>.invoke(): T = value
