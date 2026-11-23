package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class HellasPromoWithoutPreludeTest : FullGameTest() {
  override val config = GameConfig("HellasMap, promos:cards", "Player1", "Player2")

  @Test
  internal fun earlyGameWithNoPrelude() {
    startGame()
    p1.doTasks("20 TerraformRating")
    p2.doTasks("20 TerraformRating")
    p1.doTasks("2 CorporationCard", "-CorporationCard", "10 ProjectCard<Selecting>")
    p2.doTasks("2 CorporationCard", "-CorporationCard", "10 ProjectCard<Selecting>")
    p1.doTasks("-3 ProjectCard<Selecting>")
    p2.doTasks("-5 ProjectCard<Selecting>")
    with(p1) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$InterplanetaryCinematics>>",
          "30 MC",
          "20 Steel",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p2) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$PharmacyUnion>>",
          "54 MC",
          "SearchForCard<TagFilter<Class<ScienceTag>>>",
          "Disease<$PharmacyUnion>",
          "Disease<$PharmacyUnion>",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
          "-8 MC",
      )
    }

    p1.turn {
      doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC")
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "-7 MC<Player2>",
          "5 MC",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "2 ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "TerraformRating FROM Disease<$PharmacyUnion>",
      )
    }
    p2.assertCounts(22 to "TerraformRating", 0 to "Disease<$PharmacyUnion>")
  }
}
