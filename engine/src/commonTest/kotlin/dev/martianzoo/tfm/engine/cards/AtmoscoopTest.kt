package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import kotlin.test.Test

class AtmoscoopTest : CardTest() {
  // FAQ: "you can choose to raise Temperature or Venus even if that parameter is maxed"
  @Test
  fun `with Venus maxed, resolves Atmoscoop`() {
    newGame(VenusNextExpansion)
    p1.manual("15 VenusStep, AerialMappers")

    p1.manual("Atmoscoop") {
          doTask("2 VenusStep")
          doTask("2 Floater<AerialMappers>")
        }
        .expect("0 TemperatureStep, 0 VenusStep, 2 Floater<AerialMappers>")
  }

  @Test
  fun `at twenty-eight percent, raises Venus only once`() {
    newGame(VenusNextExpansion)
    p1.manual("14 VenusStep, AerialMappers")

    p1.manual("Atmoscoop") {
          doTask("2 VenusStep")
          doTask("2 Floater<AerialMappers>")
        }
        .expect("VenusStep, TerraformRating, 2 Floater<AerialMappers>")
  }

  @Test
  fun `raises Venus two atomized steps`() {
    newGame(VenusNextExpansion)
    p1.manual("AerialMappers")

    p1.manual("Atmoscoop") {
          doTask("2 VenusStep")
          doTask("2 Floater<AerialMappers>")
        }
        .expect("2 VenusStep, 2 TerraformRating, 2 Floater<AerialMappers>")
  }
}
