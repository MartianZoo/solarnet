package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class FakePreservationProgramTest : CardTest() {

  @Test
  internal fun `an earlier Prelude TR gain leaves the first Action phase penalty available`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(cn("FakePreservationProgram")).expect("5 TerraformRating")
    admin.phase("Action")

    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  internal fun `Prelude TR does not waive the penalty when Valley Trust plays Preservation Program`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.playCorp(ValleyTrust, 0)
    admin.phase("Prelude")
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(Donation)
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(cn("FakePreservationProgram")) }
        .expect("4 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  internal fun `an earlier TR gain prevents a penalty when the Prelude is acquired later`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.stdProject("AsteroidProject")

    p1.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          p1.playPrelude(cn("FakePreservationProgram"))
        }
        .expect("5 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    admin.nextGeneration(0, 0)
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
  }

  @Test
  internal fun `without an earlier TR gain playing the Prelude in Action phase loses one TR`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.playCorp(ValleyTrust, 0)
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(cn("FakePreservationProgram")) }
        .expect("4 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

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
