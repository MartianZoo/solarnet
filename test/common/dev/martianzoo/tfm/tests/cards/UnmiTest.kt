package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class UnmiTest : CardTest() {
  @Test
  internal fun `Can use its action after raising TR`() {
    initializeUnmi()
    p1.stdProject("AsteroidSP").expect("-14 MC, TerraformRating")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("-3 MC, TerraformRating")
  }

  @Test
  internal fun `Can choose UNMI after raising TR earlier in the generation`() {
    newGame()
    p1.manual("TemperatureStep")
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    engine.phase("Action")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("-3 MC, TerraformRating")
  }

  @Test
  internal fun `Cannot use its action without raising TR`() {
    initializeUnmi()
    shouldThrow<RequirementException> { p1.cardAction1(UnitedNationsMarsInitiative) }
  }

  private fun initializeUnmi() {
    newGame()
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    engine.phase("Action")
  }
}
