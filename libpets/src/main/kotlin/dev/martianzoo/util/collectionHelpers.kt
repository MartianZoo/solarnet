package dev.martianzoo.util

// should go in libutil but man, so many libs!

fun Iterable<Any>.joinOrEmpty(sep: String = ", ", prefix: String = "", suffix: String = "") =
    if (any()) joinToString(sep, prefix, suffix) else ""

fun Iterable<Any>.joinOrEmpty(sep: String = ", ", surround: String): String {
  require(surround.length == 2)
  return joinOrEmpty(sep, surround.substring(0, 1), surround.substring(1, 2))
}
