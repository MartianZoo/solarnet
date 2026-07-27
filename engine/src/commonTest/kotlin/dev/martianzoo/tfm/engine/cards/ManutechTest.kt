package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class ManutechTest : CardTest() {

  @Test
  fun `with Manutech, raises each production type`() {
    newGame("BMV")
    p1.manual("Manutech")
    p1.manual("PROD[8 Megacredit, Steel, 6 Titanium, 7 Plant, 5 Energy, 3 Heat]")
        .expect("8 Megacredit, Steel, 6 Titanium, 7 Plant, 5 Energy, 3 Heat")
  }

  @Test
  fun `with Manutech, adds Nitrophilic Moss`() {
    newGame("BMV")
    p1.manual("Manutech")
    p1.manual("NitrophilicMoss").expect("PROD[2 Plant], 0 Plant")
  }
}
