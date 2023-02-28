package dev.martianzoo.util

import com.google.common.truth.Truth.assertThat
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
}
