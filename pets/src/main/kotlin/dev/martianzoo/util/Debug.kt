package dev.martianzoo.util

object Debug {
  fun d(message: String) {
    if (DEBUG) println("$message")
  }

  fun <T : Any> T.d(message: String): T {
    if (DEBUG) println("$message: $this")
    return this
  }

  fun <T : Any> T.d(getMessage: (T) -> String): T {
    if (DEBUG) println(getMessage(this))
    return this
  }
}

const val DEBUG = false
