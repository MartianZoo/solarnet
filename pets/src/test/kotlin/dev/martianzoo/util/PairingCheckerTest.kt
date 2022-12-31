package dev.martianzoo.util

import org.junit.jupiter.api.Test
import java.util.*

class PairingCheckerTest {
  @Test
  fun testStuff() {
    PairingChecker.check("")
    PairingChecker.check("()")
    PairingChecker.check("x()")
    PairingChecker.check("x(x)")
    PairingChecker.check("x(x<d[]>{f})")
  }

  @Test
  fun listValid() {
    weird(true)
  }

  @Test
  fun listInvalid() {
    weird(false)
  }

  fun weird(expectValid: Boolean) {
    val chars = "[]<>O\"\\".toCharArray()
    val maxMisses = 10_000
    var length = 0
    while (length < 6) {
      length++
      var misses = 0
      val set = TreeSet<String>()
      while (misses < maxMisses) {
        val s = (1..length).map { chars.random() }.joinToString("")
        if (s.contains("OO")) continue
        if (PairingChecker.isValid(s) == expectValid) {
          misses = if (set.add(fix(s))) 0 else misses + 1
        }
      }
      set.forEach(::println)
    }
  }

  private fun fix(s: String): String {
    return if (s.indexOf("<") < s.indexOf("[")) {
      s
    } else {
      s.toCharArray().map {
        when (it) {
          '[' -> '<'
          ']' -> '>'
          '<' -> '['
          '>' -> ']'
          else -> it
        }
      }.joinToString("")
    }
  }
}
