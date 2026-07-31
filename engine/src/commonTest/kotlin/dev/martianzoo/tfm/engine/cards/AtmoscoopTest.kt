package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class AtmoscoopTest : CardTest() {
  // FAQ: "you can choose to raise Temperature or Venus even if that parameter is maxed"
  @Test
  fun `with Venus maxed, resolves Atmoscoop`() {
    newGame("VenusNextExpansion")
    p1.manual("15 VenusStep, AerialMappers")

    p1.manual("Atmoscoop") {
          doTask("Ok THEN VenusStep")
          doTask("2 Floater<AerialMappers>")
        }
        .expect("0 TemperatureStep, 0 VenusStep, 2 Floater<AerialMappers>")
  }
}
