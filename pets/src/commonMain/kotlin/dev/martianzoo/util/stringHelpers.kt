package dev.martianzoo.util

/**
 * If the "receiver" is null, returns the empty string; otherwise transforms it with [transform],
 * converts the result to a string, and returns it with [prefix] before it and [suffix] after it.
 */
fun <T : Any> T?.wrap(
    prefix: Any,
    suffix: Any,
    transform: (T) -> Any = { it },
): String = this?.let { "$prefix${transform(this)}$suffix" } ?: ""

/** Same as `wrap(prefix, "", transform)`. */
fun <T : Any> T?.pre(prefix: Any, transform: (T) -> Any = { it }) = wrap(prefix, "", transform)

/** Same as `wrap("", suffix, transform)`. */
fun <T : Any> T?.suf(suffix: Any, transform: (T) -> Any = { it }) = wrap("", suffix, transform)

fun Any.iff(b: Boolean): String = if (b) toString() else ""
