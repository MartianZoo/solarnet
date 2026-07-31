package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class RoboticWorkforceTest : CardTest() {
  @Test
  fun `with Strip Mine in play, adds Robotic Workforce`() {
    newGame("TerraformingMars,TharsisMap,CorporateEraExpansion")
    p1.manual("PROD[4 Energy], StripMine")
    p1.assertProds(2 to "Steel", 1 to "Titanium", 2 to "Energy")
    p1.manual("RoboticWorkforce") { doTask("CopyProductionBox<StripMine>") }
    p1.assertProds(4 to "Steel", 2 to "Titanium", 0 to "Energy")
  }

  @Test
  fun `with a non-building card, tries to copy it using Robotic Workforce`() {
    newGame("TerraformingMars,TharsisMap,CorporateEraExpansion")
    p1.manual("PROD[Energy], Mine, MassConverter")
    p1.manual("RoboticWorkforce") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<MassConverter>") }
      abort()
    }
  }

  @Test
  fun `with a p2 building card, tries to copy it using Robotic Workforce`() {
    newGame("TerraformingMars,TharsisMap,CorporateEraExpansion")
    val p2 = requireP2()
    p1.manual("IndustrialMicrobes")
    p2.manual("Mine")

    p1.manual("RoboticWorkforce") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<Mine<Player2>>") }
      abort()
    }
  }

  @Test
  fun `without owning Mine, tries to copy it using Robotic Workforce`() {
    newGame("TerraformingMars,TharsisMap,CorporateEraExpansion")
    p1.manual("IndustrialMicrobes")
    p1.manual("RoboticWorkforce") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<Mine>") }
      abort()
    }
  }
}
