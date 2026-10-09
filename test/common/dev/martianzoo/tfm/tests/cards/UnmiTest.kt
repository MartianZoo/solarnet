package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class UnmiTest : TfmSandboxTest() {
  @Test
  internal fun `Can use its action after raising TR`() {
    newTestGame(kimCorporation = UnitedNationsMarsInitiative)
    kim.setToExMachina(17, "MC")
    kim.stdProject("AsteroidProject")

    kim.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating")
  }

  @Test
  internal fun `Cannot use its action without raising TR`() {
    newTestGame(kimCorporation = UnitedNationsMarsInitiative)

    shouldThrow<RequirementException> { kim.cardAction1(UnitedNationsMarsInitiative) }
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Can use UNMI acquired after an earlier TR gain in the generation`() {
      newTestGame(addOptions = "PreludeExpansion", playerCount = 2, kimCorporation = CrediCor)
      kim.playPrelude(UnmiContractor)
      kim.playPrelude(Merger) { kim.playCorp(UnitedNationsMarsInitiative) }
      stan.playPrelude(Supplier)
      stan.playPrelude(MetalsCompany)

      kim.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating")
    }

    @Test
    internal fun `UNMI Contractor TR during Prelude qualifies UNMI in generation one`() {
      newTestGame(
          addOptions = "PreludeExpansion",
          playerCount = 2,
          kimCorporation = UnitedNationsMarsInitiative,
      )
      kim.playPrelude(UnmiContractor)
      kim.playPrelude(Donation)
      stan.playPrelude(Supplier)
      stan.playPrelude(MetalsCompany)

      kim.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating")
    }
  }
}
