package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ManutechTest : CardTest() {

  @Test
  internal fun `Pays for every production increase`() {
    newGame(VenusNextExpansion)
    p1.manual("$Manutech")
    p1.manual("PROD[8 Megacredit, Steel, 6 Titanium, 7 Plant, 5 Energy, 3 Heat]")
        .expect("8 Megacredit, Steel, 6 Titanium, 7 Plant, 5 Energy, 3 Heat")
  }

  @Test
  internal fun `Pays when Nitrophilic Moss raises plant production`() {
    newGame(VenusNextExpansion)
    p1.manual("$Manutech")
    p1.manual("$NitrophilicMoss").expect("PROD[2 Plant], 0 Plant")
  }
}
