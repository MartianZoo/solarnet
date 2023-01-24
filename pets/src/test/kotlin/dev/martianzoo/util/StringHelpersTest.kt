package dev.martianzoo.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StringHelpersTest {
  @Test
  fun testWrap() {
    class Ennie(val stringForm: String) {
      override fun toString() = stringForm
    }

    val pref = Ennie("pref")
    val suff = Ennie("suff")
    assertThat(null.wrap(pref, suff)).isEqualTo("")
    assertThat(null.pre(pref)).isEqualTo("")
    assertThat(null.suf(suff)).isEqualTo("")
    assertThat(Ennie("thing").wrap(pref, suff)).isEqualTo("prefthingsuff")
    assertThat(Ennie("thing").pre(pref)).isEqualTo("prefthing")
    assertThat(Ennie("thing").suf(suff)).isEqualTo("thingsuff")
  }
}
