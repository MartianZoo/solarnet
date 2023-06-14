package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
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
