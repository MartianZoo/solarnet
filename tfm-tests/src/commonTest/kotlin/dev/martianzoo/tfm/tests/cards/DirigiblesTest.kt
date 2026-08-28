package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DirigiblesTest : CardTest() {
  @Test
  internal fun `Can pay for a Venus card with two floaters`() {
    newGame(VenusNextExpansion)

    engine.phase("Action")
    p1.manual("ProjectCard, $Dirigibles, 2 Floater<$Dirigibles>, 5 MC")

    p1.playProject(AerialMappers, 5) {
          doTask("2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
        }
        .expect("-2 Floater<$Dirigibles>, $AerialMappers")
  }

  @Test
  internal fun `Comet for Venus can remove money from another Venus card owner`() {
    newGame(VenusNextExpansion)
    val p2 = requireP2()
    p2.manual("4 MC, $Dirigibles")

    p1.manual("$CometForVenus") { doTask("-4 MC<Player2>") }

    p2.count("MC") shouldBe 0
  }
}
