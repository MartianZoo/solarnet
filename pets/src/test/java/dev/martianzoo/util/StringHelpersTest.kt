package dev.martianzoo.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private class StringHelpersTest {
  @Test
  fun testWrap() {
    class Ennie(val stringForm: String) {
      override fun toString() = stringForm
    }

    val pref = Ennie("pref")
    val suff = Ennie("suff")
    null.wrap(pref, suff) shouldBe ""
    null.pre(pref) shouldBe ""
    null.suf(suff) shouldBe ""
    Ennie("thing").wrap(pref, suff) shouldBe "prefthingsuff"
    Ennie("thing").pre(pref) shouldBe "prefthing"
    Ennie("thing").suf(suff) shouldBe "thingsuff"
  }
}
