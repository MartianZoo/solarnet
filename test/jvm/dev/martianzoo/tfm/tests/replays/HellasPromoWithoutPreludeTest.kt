package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class HellasPromoWithoutPreludeTest : AbstractFullGameTest() {
  override val config = GameConfig("HellasMap, promos:cards", "Player1", "Player2")

  @Test
  internal fun earlyGameWithNoPrelude() {
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    admin.startGame()
    p1.doTasks("20 TerraformRating")
    p2.doTasks("20 TerraformRating")
    p1.doTasks("2 CorporationCard", "-CorporationCard", "10 ProjectCard<Selecting>")
    p2.doTasks("2 CorporationCard", "-CorporationCard", "10 ProjectCard<Selecting>")
    p1.doTasks("-3 ProjectCard<Selecting>")
    p2.doTasks("-5 ProjectCard<Selecting>")
    p1.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$InterplanetaryCinematics>>",
          "$InterplanetaryCinematics FROM CorporationCard",
          "30 MC",
          "20 Steel",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$PharmacyUnion>>",
          "$PharmacyUnion FROM CorporationCard",
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
      doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "$Sabotage FROM ProjectCard",
          "-7 MC<Player2>",
      )
      doTask("2 MC")
      doTask("3 MC")
    }

    p2.turn {
      completeOperation {
        doTasks(*playCardTasks("$Research"), "11 Pay<> FROM MC", "$Research FROM ProjectCard")
        doTasks(
            "2 ProjectCard",
            "TerraformRating FROM Disease<$PharmacyUnion>",
            "TerraformRating FROM Disease<$PharmacyUnion>",
        )
      }
    }
    p2.assertCounts(22 to "TerraformRating", 0 to "Disease<$PharmacyUnion>")
    expectPendingTasksAtEnd(1) // Player1's next action offer
  }
}
