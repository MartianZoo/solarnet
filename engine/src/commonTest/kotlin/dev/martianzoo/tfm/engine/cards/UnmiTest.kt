package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class UnmiTest : CardTest() {
  @Test
  internal fun `Can use its action after raising TR`() {
    initializeUnmi()
    p1.stdProject("AsteroidSP").expect("-14, TerraformRating")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("-3, TerraformRating")
  }

  @Test
  internal fun `Can choose UNMI after raising TR earlier in the generation`() {
    newGame()
    p1.manual("TemperatureStep")
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    engine.phase("Action")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("-3, TerraformRating")
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
