package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DirigiblesTest : CardTest() {
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
}
