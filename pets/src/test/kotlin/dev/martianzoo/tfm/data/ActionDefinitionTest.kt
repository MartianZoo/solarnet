package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

class ActionDefinitionTest {
  @Test
  fun test() {
    val claim = Canon.actionsByClassName[cn("ClaimMilestone")]!!
    assertThat(claim.id).isEqualTo(cn("SAC"))
    assertThat(claim.bundle).isEqualTo("B")
    assertThat(claim.project).isFalse()
    assertThat(claim.actionText).isEqualTo("8 -> Milestone")
  }
}
