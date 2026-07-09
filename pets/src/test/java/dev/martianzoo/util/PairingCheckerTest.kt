package dev.martianzoo.util

import io.kotest.matchers.shouldBe
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
    PairingChecker.isValid("(") shouldBe false
    PairingChecker.isValid("x()") shouldBe false
    PairingChecker.isValid("x((yx))") shouldBe false
    PairingChecker.isValid("a(b<c)d>e") shouldBe false
  }
}
