package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth
import dev.martianzoo.tfm.petaform.api.Action
import org.junit.jupiter.api.Test

class ActionTest {
  @Test
  fun stupid() {
    testRoundTrip("-> 0")
  }

  private fun testRoundTrip(start: String, end: String = start) {
    val parse: Action = PetaformParser.parse(start)
    Truth.assertThat(parse.toString()).isEqualTo(end)
  }

}
