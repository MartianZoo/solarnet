package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TerraformingDealTest : TfmGameplayTest() {
  @Test
  internal fun `Pays for both TR gained from a single card`() {
    newTestGame("PreludeExpansion, TerraformingDeal", playerCount = 2)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    kim.doTasks("PlayProject<Class<$BribedCommittee>>", "-7 MC").expect("2 TerraformRating, -3 MC")
  }

  @Test
  internal fun `Pays for becoming chairman after production`() {
    newTestGame(
        "PreludeExpansion, TerraformingDeal, HighCircles, TurmoilExpansion",
        playerCount = 2,
    )
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>")
    kim.completeOperation {
      doTask("PlayCard<Class<PreludeCard>, Class<$HighCircles>>")
      doTask("2 PartyDelegate<Scientists>")
      autoExecNow(EAGER)
    }
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    admin.count("SolarPhase") shouldBe 1
    expectChanges("2 MC<Kim>, 0 TerraformRating<Kim>") {
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
    kim.count("Chairman") shouldBe 1
  }
}
