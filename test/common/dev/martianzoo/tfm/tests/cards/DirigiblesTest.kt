package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DirigiblesTest : CardTest() {
  @Test
  internal fun `Can pay for a Venus card with two floaters`() {
    newGame(VenusNextExpansion)

    admin.phase("Action")
    p1.runOperation("ProjectCard, $Dirigibles, 2 Floater<$Dirigibles>, 5 MC")

    p1.playProject(AerialMappers, 5) {
          doTask("-2 Floater<$Dirigibles>")
        }
        .expect("-2 Floater<$Dirigibles>, $AerialMappers")
  }

  @Test
  internal fun `Can spend floaters before paying the remaining money`() {
    newGame(VenusNextExpansion)
    admin.phase("Action")
    p1.runOperation("ProjectCard, $Dirigibles, 2 Floater<$Dirigibles>, 5 MC")

    p1.playProject(
            AerialMappers,
            payment = {
              doTask("-2 Floater<$Dirigibles>")
              doTask("-5 MC")
            },
        )
        .expect("$AerialMappers")
  }

  @Test
  internal fun `A card's required floater is additional to the floaters spent paying for it`() {
    newGame(VenusNextExpansion)
    admin.phase("Action")
    p1.runOperation("ProjectCard, $Dirigibles, 4 Floater<$Dirigibles>, 3 MC, 6 VenusStep")

    p1.playProject(
            StratosphericBirds,
            payment = {
              doTask("-3 Floater<$Dirigibles>")
              doTask("-3 MC")
            },
        )
        .expect("$StratosphericBirds")
    p1.count("Floater<$Dirigibles>") shouldBe 0
  }

  @Test
  internal fun `Comet for Venus can remove money from another Venus card owner`() {
    newGame(VenusNextExpansion)
    val p2 = requireP2()
    p2.runOperation("4 MC, $Dirigibles")

    p1.runOperation("$CometForVenus") { doTask("-4 MC<Player2>") }

    p2.count("MC") shouldBe 0
  }
}
