package dev.martianzoo.util

// should go in libutil but man, so many libs!

fun Iterable<Any>.joinOrEmpty(sep: String = ", ", prefix: String = "", suffix: String = "") =
    if (any()) joinToString(sep, prefix, suffix) else ""
