package dev.martianzoo.pets.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PairingCheckerTest {
  @Test
  internal fun testStuff() {
    PairingChecker.check("")
    PairingChecker.check("(x)")
    PairingChecker.check("x(x)")
    PairingChecker.check("x(x<d[e]>{f})")
  }

  @Test
  internal fun testInvalid() {
    PairingChecker.isValid("(") shouldBe false
    PairingChecker.isValid("x()") shouldBe false
    PairingChecker.isValid("x((yx))") shouldBe false
    PairingChecker.isValid("a(b<c)d>e") shouldBe false
  }
}
