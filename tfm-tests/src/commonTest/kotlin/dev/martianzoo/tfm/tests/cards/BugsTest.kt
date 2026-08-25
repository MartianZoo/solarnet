package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.pets.data.Task
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
internal class BugsTest : CardTest() {
  @Test
  internal fun `Philares incorrectly gives its owner the resource choice immediately`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.beginManual("CityTile<Tharsis_3_3>")
    manual.addTasks("Plant?")

    val reward = philaresReward()
    reward.assignee shouldBe PLAYER2
    shouldThrow<TaskException> { manual.prepareTask(reward.id) }

    p2.doTask("Steel")
    manual.doTask("Plant")
    p2.count("Steel") shouldBe 1
    p1.count("Plant") shouldBe 1
  }

  @Test
  internal fun `Philares incorrectly lets the active player continue while its reward is unresolved`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.beginManual("CityTile<Tharsis_3_3>")
    manual.addTasks("Heat?")
    philaresReward().assignee shouldBe PLAYER2

    manual.doTask("Heat")
    p2.doTask("Steel")

    p1.count("Heat") shouldBe 1
    p2.count("Steel") shouldBe 1
  }

  // NOTE: Established Methods says that an unaffordable second standard project is replaced by
  // NOTE: losing 10 M€ (or as much as possible). Fake Established Methods intentionally omits
  // NOTE: that fallback.
  @Test
  internal fun `Established Methods without its note dead-ends when no second project is affordable`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.phase("Prelude")
    p1.manual("PreludeCard")

    val deadEnd =
        shouldThrow<AbstractException> {
          p1.playPrelude(FakeEstablishedMethods) {
            p1.manual("-20")
            doTask("UseAction<GreenerySP, First>")
            p1.autoExecNow()
          }
        }
    deadEnd.message shouldContain "FakeEstablishedMethods"
  }

  // https://boardgamegeek.com/thread/3361875/questions-about-the-head-start
  @Test
  internal fun `Head Start incorrectly allows its two actions to interleave`() {
    newGame(PreludeExpansion, TurmoilCardPack, PromoCardPack)
    p1.phase("Prelude")
    p1.manual("4, 10 ProjectCard, PreludeCard, 10 Heat")

    p1.playPrelude(HeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "Megacredit")
      doTask("UseAction<ConvertHeatSA, First>")
      doTask("8 Pay<Class<Heat>> FROM Heat")
      doTask("UseAction<AquiferSP, First>")
      doTask("18 Pay<Class<Megacredit>> FROM Megacredit")
      placeTile(5, 5)
    }
  }

  @Test
  internal fun `Philantropist incorrectly counts Vitor as a card with victory points`() {
    newGame(GameConfig("PreludeExpansion, Philantropist", "Player1", "Player2"))
    p1.manual("$Vitor, $SearchForLife, $Tardigrades, $ColonizerTrainingCamp, $DustSeals")

    p1.manual("Philantropist")

    p1.count("Philantropist") shouldBe 1
  }

  @Test
  internal fun `Prelude incorrectly allows discarding a playable card`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    val moneyBefore = p1.count("Megacredit")

    p1.startTurn()
    p1.doTask("-PreludeCard")
    p1.startTurn()
    p1.doTask("PlayCard<Class<PreludeCard>, Class<$DomeFarming>>")

    p1.assertCounts(1 to "$DomeFarming", 0 to "PreludeCard")
    p1.count("Megacredit") shouldBe moneyBefore + 15
  }

  // Solar Probe should count its own science tag and draw one card for all three tags.
  @Test
  internal fun `Solar Probe incorrectly draws no card during normal play`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    engine.phase("Action")
    p1.manual("9, ProjectCard, $TransNeptuneProbe, $PhysicsComplex")

    p1.playProject(SolarProbe, 9).expect("-9, -ProjectCard")
  }

  @Test
  internal fun `Stealing zero is incorrectly allowed and prevents Mons Insurance compensation`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 Megacredit")
    p2.manual("5 Megacredit")

    p1.manual("3 Megacredit FROM Megacredit<Player2>?") {
          // Decline taking Player 2's megacredits.
          declineTask()
        }
        .expect("0 Megacredit<Player1>, 0 Megacredit<Player2>")
  }

  @Test
  internal fun `Air Raid incorrectly remains playable when only its player has money`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.manual("ProjectCard, 5 Megacredit")

    p1.playProject(AirRaid, 0).expect("-Floater<$AtmoCollectors>, 0 Megacredit<Player1>")
    p2.assertCounts(0 to "Megacredit")
  }

  @Test
  internal fun `Public Plans incorrectly remains playable while revealing no other cards`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("7 Megacredit, ProjectCard")

    p1.playProject(PublicPlans, 7)

    p1.assertCounts(0 to "ProjectCard", 1 to "PlayedEvent<Class<$PublicPlans>>")
  }

  @Test
  fun `Space Elevator incorrectly accepts payment that wastes one steel`() {
    newGame()
    engine.phase("Action")
    p1.manual("10 Steel, 10 Titanium, ProjectCard")

    p1.inTurn {
      doTask("UseAction<PlayCardSA, First>")
      doTask("PlayCard<Class<ProjectCard>, Class<$SpaceElevator>>")
      doTask("7 Pay<Class<Steel>> FROM Steel")
      doTask("5 Pay<Class<Titanium>> FROM Titanium")
      doTask("Ok")
    }

    p1.assertCounts(
        3 to "Steel",
        5 to "Titanium",
        0 to "ProjectCard",
        1 to "$SpaceElevator",
    )
  }

  private fun philaresReward(): Task =
      game.tasks
          .extract { it }
          .single {
            it.instruction.toString().startsWith("StandardResource<Player2>")
          }
}
