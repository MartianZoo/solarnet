package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

private class StandardActionDefinitionTest {
  @Test
  fun testOneFromCanon() {
    val claim = Canon.action(cn("ClaimMilestoneSA"))
    assertThat(claim.shortName).isEqualTo(cn("SAC"))
    assertThat(claim.bundle).isEqualTo("B")
    assertThat(claim.project).isFalse()
    assertThat(claim.actions).containsExactly("8 -> Milestone")
  }
}
