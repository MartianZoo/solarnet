package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class DirigiblesTest : CardTest() {
  @Test
  fun `with two floaters, pays for a Venus card using Dirigibles`() {
    newGame(VenusNextExpansion)

    engine.phase("Action")
    p1.manual("ProjectCard, $Dirigibles, 2 Floater<$Dirigibles>, 5")

    p1.playProject(AerialMappers, 5) {
          doTask("-2 Floater<$Dirigibles>! THEN -6 Owed<Class<Megacredit>>.")
        }
        .expect("-2 Floater<$Dirigibles>, $AerialMappers")
  }
}
