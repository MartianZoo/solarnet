package dev.martianzoo.pets.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StringHelpersTest {
  @Test
  internal fun testWrap() {
    class Ennie(private val stringForm: String) {
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
