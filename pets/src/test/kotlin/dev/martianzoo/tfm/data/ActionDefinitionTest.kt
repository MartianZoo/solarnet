package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import org.junit.jupiter.api.Test

class ActionDefinitionTest {
  @Test
  fun test() {
    val claim = Canon.actionsByComponentName["ClaimMilestone"]!!
    assertThat(claim.id).isEqualTo("SAC")
    assertThat(claim.bundle).isEqualTo("B")
    assertThat(claim.project).isFalse()
    assertThat(claim.actionText).isEqualTo("8 -> Milestone")
  }
}
