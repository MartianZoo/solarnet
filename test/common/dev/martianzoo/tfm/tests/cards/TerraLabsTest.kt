package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TerraLabsTest : TfmGameplayTest() {
  @Test
  internal fun `Buys all four research cards for four MC`() {
    newTestGame(
        addOptions = "TerraLabsResearch",
        playerCount = 2,
        kimCorporation = TerraLabsResearch,
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")

    kim.doTasks(
            "Ok", // Keep all four research cards.
            "-4 MC",
        )
        .expect("4 ProjectCard, -4 MC")
  }

  @Test
  internal fun `Acquiring Polyphemos cancels the card-purchase discount`() {
    newTestGame(
        addOptions = "PreludeExpansion, TerraLabsResearch",
        playerCount = 2,
        kimCorporation = TerraLabsResearch,
    )
    kim.completeOperation {
      doTask("PlayCard<Class<PreludeCard>, Class<$Merger>>")
      doTask("PlayCard<Class<CorporationCard>, Class<$Polyphemos>>")
      autoExecNow(EAGER)
    }
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$SupplyDrop>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")

    kim.doTasks(
            "Ok", // Keep all four research cards.
            "-12 MC",
        )
        .expect("4 ProjectCard, -12 MC")
  }
}
