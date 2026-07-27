package dev.martianzoo.util

internal object Debug {
  internal fun d(message: String) {
    if (DEBUG) println(message)
  }

  internal fun <T : Any> T.d(message: String): T {
    if (DEBUG) println("$message: $this")
    return this
  }

  internal fun <T : Any> T.d(getMessage: (T) -> String): T {
    if (DEBUG) println(getMessage(this))
    return this
  }
}

internal const val DEBUG = false
