package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.Prelude2Expansion
import kotlin.test.Test

internal class FakePreservationProgramBugsTest : CardTest() {
  @Test
  internal fun `reversed TR still pays Terraforming Deal`() {
    newGame(Prelude2Expansion, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("FakePreservationProgram, TerraformingDeal")
    admin.phase("Action")

    // The printed Preservation Program prevents the gain, and therefore this rebate.
    p1.runOperation("TerraformRating").expect("0 TerraformRating, 2 MC")
  }
}
