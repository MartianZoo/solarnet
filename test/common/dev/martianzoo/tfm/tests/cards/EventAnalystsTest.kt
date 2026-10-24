package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test

internal class EventAnalystsTest : TfmGameplayTest() {
  @Test
  internal fun `Influence stacks with High Circles and Colonial Representation beyond four`() {
    newTestGame(
        "PreludeExpansion, HighCircles, TurmoilExpansion, ColonialRepresentation, Luna",
        playerCount = 2,
    )
    kim.completeOperation {
      doTask("PlayCard<Class<PreludeCard>, Class<$HighCircles>>")
      doTasks("PartyLeader<Scientists>", "PartyFollower<Scientists>")
      autoExecNow(EAGER)
    }
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    kim.doTasks(*playCardTasks("$EventAnalysts"))
    kim.completeOperation {
      doTasks(*standardProjectTasks("BuildColonyProject"))
      doTask("Colony<Luna>")
      autoExecNow(EAGER)
    }
    stan.doTask("Pass")
    kim.doTasks(*playCardTasks("$ColonialRepresentation"))
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    admin.doTasks(
        "PartyLeader<Greens, Neutral>",
        "ExploreFirstDirective",
        "PartyFollower<Reds, Neutral>",
    )
    kim.doTask("-4 ProjectCard<Selecting>")
    stan.doTask("-4 ProjectCard<Selecting>")
    stan.doTask("Pass")
    // The chairman retains its influence; the dominant party supplies leader and delegate
    // influence.
    kim.completeOperation {
      doTask("UseAction<LobbyAction, Action1>")
      doTask("PartyLeader<Scientists> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTask("UseAction<PaidLobbyAction, Action1>")

      doTask("PartyFollower<Scientists>")
    }
    kim.completeOperation {
      doTask("UseAction<PaidLobbyAction, Action1>")

      doTask("PartyFollower<Scientists>")
    }
    kim.completeOperation {
      doTask("UseAction<PaidLobbyAction, Action1>")

      doTask("PartyFollower<Scientists>")
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("6 Steel<Kim>, 6 Plant<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      stan.doTask("OceanTile<Tharsis_1_5> BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }
}
