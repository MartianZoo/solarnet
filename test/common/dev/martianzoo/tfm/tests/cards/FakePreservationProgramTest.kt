package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import kotlin.test.Test

internal class FakePreservationProgramTest : CardTest() {
  @Test
  internal fun `prelude gains five TR and reverses only one TR each action phase`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("PreludeCard")
    p1.playPrelude(cn("FakePreservationProgram")).expect("5 TerraformRating")
    admin.phase("Action")

    p1.runOperation("3 TerraformRating").expect("2 TerraformRating")
    p1.runOperation("TerraformRating").expect("TerraformRating")
    requireP2().runOperation("TerraformRating").expect("TerraformRating")

    admin.runOperation("Generation")
    p1.runOperation("TerraformRating").expect("0 TerraformRating")
    p1.runOperation("TerraformRating").expect("TerraformRating")
  }
}
