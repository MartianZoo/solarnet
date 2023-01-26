package dev.martianzoo.util

import com.google.common.collect.Lists.cartesianProduct
import com.google.common.truth.Truth.assertThat
import java.util.Collections.nCopies
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

private class PairingCheckerTest {
  @Test
  fun testStuff() {
    PairingChecker.check("")
    PairingChecker.check("(x)")
    PairingChecker.check("x(x)")
    PairingChecker.check("x(x<d[e]>{f})")
  }

  @Test
  fun testInvalid() {
    assertThat(PairingChecker.isValid("(")).isFalse()
    assertThat(PairingChecker.isValid("x()")).isFalse()
    assertThat(PairingChecker.isValid("x((yx))")).isFalse()
    assertThat(PairingChecker.isValid("a(b<c)d>e")).isFalse()
  }

  @Disabled @Test
  fun listValid() {
    weird(true, max = 8)
  }

  @Disabled @Test
  fun listInvalid() {
    weird(false, max = 6)
  }

  fun weird(expectValid: Boolean, max: Int) {
    var length = 0
    val allChars: List<Char> = "[]<>X".toList()
    while (length <= max) {
      length++
      for (chars in cartesianProduct(nCopies(length, allChars))) {
        val s = chars.joinToString("")
        if (s.contains("XX")) continue
        val pos = s.indexOfFirst { it in "<>[]" }
        if (pos < 0 || s[pos] == '<') continue
        if (!s.contains('\\') && s > s.reversed()) continue
        if (PairingChecker.isValid(s) == expectValid) {
          println(s)
        }
      }
    }
  }
}
