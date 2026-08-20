package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class UnmiTest : CardTest() {
  @Test
  fun `after raising TR, uses the UNMI action`() {
    initializeUnmi()
    p1.stdProject("AsteroidSP").expect("-14, TerraformRating")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("-3, TerraformRating")
  }

  @Test
  fun `after raising TR before choosing UNMI, uses its action`() {
    newGame()
    p1.manual("TemperatureStep")
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    engine.phase("Action")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("-3, TerraformRating")
  }

  @Test
  fun `without raising TR, tries to use the UNMI action`() {
    initializeUnmi()
    shouldThrow<RequirementException> { p1.cardAction1(UnitedNationsMarsInitiative) }
  }

  private fun initializeUnmi() {
    newGame()
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    engine.phase("Action")
  }
}
