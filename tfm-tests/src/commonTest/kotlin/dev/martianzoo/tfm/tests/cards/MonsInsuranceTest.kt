package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MonsInsuranceTest : CardTest() {
  @Test
  internal fun `Starting production loss reaches every opponent but not its owner`() {
    newGame(PromoCardPack, players = 3)
    val p3 = game.tfm(PLAYER3)

    p1.playCorp(MonsInsurance, 0)
        .expect("48 MC, PROD[4 MC<Player1>], PROD[-2 MC<Player2>], PROD[-2 MC<Player3>]")

    p3.assertProds(-2 to "MC")
  }

  @Test
  internal fun `Starting production loss does not target the solo opponent`() {
    newGame(PromoCardPack, players = 1)

    p1.playCorp(MonsInsurance, 0).expect("48 MC, PROD[4 MC<Me>], PROD[0 MC<SoloOpponent>]")
  }

  @Test
  internal fun `Gains only four mc when Merger plays it after Manutech`() {
    newGame(PromoCardPack, PreludeExpansion, VenusNextExpansion)
    val p2 = requireP2()
    p1.playCorp(Manutech, 0)
    engine.phase("Prelude")
    val moneyBefore = p1.count("MC")

    p1.playPrelude(Merger) {
      doTask("PlayCard<Class<CorporationCard>, Class<$MonsInsurance>>")
    }

    p1.count("MC") shouldBe moneyBefore + 10 // -42 + 48 starting money + 4 from Manutech
    p1.assertProds(4 to "MC")
    p2.assertProds(-2 to "MC")
  }

  @Test
  internal fun `Hired Raiders transfer finishes before Mons compensates the victim after each steal`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    engine.phase("Action")
    p1.manual("$MonsInsurance, 10 MC")
    p2.manual("10 MC, ProjectCard")
    p3.manual("4 Steel")
    val monsMoneyBefore = p1.count("MC")

    p2.playProject(HiredRaiders, 1) { doTask("2 Steel<Player2> FROM Steel<Player3>") }
    p2.manual("2 Steel FROM Steel<Player3>")

    p1.count("MC") shouldBe monsMoneyBefore - 6
    p2.count("Steel") shouldBe 4
    p3.count("Steel") shouldBe 0
    p3.count("MC") shouldBe 6
  }

  @Test
  internal fun `An attack during the Prelude phase requires compensation`() {
    newGame(PromoCardPack, PreludeExpansion)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 MC")
    p2.manual("Plant")
    engine.phase("Prelude")

    p1.manual("-Plant<Player2>").expect("-Plant<Player2>, -3 MC<Player1>, 3 MC<Player2>")
  }

  @Test
  internal fun `Mons owner pays the victim once for a multi-step production attack`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 MC")
    p2.manual("PROD[3 Plant]")

    p1.manual("PROD[-2 Plant<Player2>]")
        .expect("PROD[-2 Plant<Player2>], -3 MC<Player1>, 3 MC<Player2>")
  }

  @Test
  internal fun `Self-inflicted losses and Engine-run Global Events cause no payout`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance")
    p2.manual("Plant, PROD[Plant]")

    p2.manual("-Plant, PROD[-Plant]").expect("-Plant<Player2>, PROD[-Plant<Player2>]")
    game
        .gameplay(ENGINE)
        .godMode()
        .manual("Plant<Player2>, -Plant<Player2>")
        .expect("0 MC<Player1>, 0 MC<Player2>")
  }

  @Test
  internal fun `Payment is limited to the Mons owner's available mc`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p1.manual("$MonsInsurance")
    p1.manual("-1 MC / 1 MC")
    p1.manual("2 MC")
    p3.manual("Plant")

    p2.manual("-Plant<Player3>").expect("-Plant<Player3>, -2 MC<Player1>, 2 MC<Player3>")
  }

  @Test
  internal fun `Zero payout is settled before the Mons owner gains money later in the action`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance")
    p1.manual("-${p1.count("MC")} MC")
    p2.manual("Plant")

    val manual = p1.godMode().also { it.autoExecMode = NONE }
    manual.addTasks("-Plant<Player2>, 2 MC")
    val attack = game.tasks.extract { it }.single { "Plant<Player2>" in it.instruction.toString() }
    manual.selectTask(attack.id)
    val payout = game.tasks.extract { it }.single { "FROM MC" in it.instruction.toString() }
    manual.selectTask(payout.id)
    manual.doTask("2 MC<Player1>")

    p1.count("MC") shouldBe 2
    p2.count("MC") shouldBe 0
  }

  @Test
  internal fun `Pharmacy Union's own loss does not require compensation from Mons`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, $Decomposers")
    p2.manual("$PharmacyUnion")
    val monsMoneyBefore = p1.count("MC")
    val pharmacyMoneyBefore = p2.count("MC")
    val checkpoint = game.timeline.checkpoint()

    p1.manual("MicrobeTag<$Decomposers>")

    p1.count("MC") shouldBe monsMoneyBefore
    p2.count("MC") shouldBe pharmacyMoneyBefore - 4
    game.events
        .changesSince(checkpoint)
        .single {
          it.change.removing?.let(game.reader::resolve) == p2.resolve("MC")
        }
        .actor shouldBe p2.actor
  }

  @Test
  internal fun `Declining an optional removal avoids compensation`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 MC")
    p2.manual("Plant")

    p1.manual("-Plant<Player2>?") {
          // Decline removing Player 2's plant.
          declineTask()
        }
        .expect("0 Plant<Player2>, 0 MC<Player1>, 0 MC<Player2>")
  }

  @Test
  internal fun `Solo steals and production attacks make Mons pay the general supply`() {
    newGame(PromoCardPack, players = 1)
    engine.phase("Action")
    p1.manual("$MonsInsurance, ProjectCard")

    p1.playProject(HiredRaiders, 1) {
          doTask("3 MC<Me> FROM MC<SoloOpponent>")
        }
        .expect("-1 MC<Me>")
    p1.manual("PROD[-2 Plant<SoloOpponent>]").expect("-3 MC<Me>")
  }

  @Test
  internal fun `An attack on the Mons owner requires no transfer`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, Plant, 10 MC")

    p2.manual("-Plant<Player1>").expect("-Plant<Player1>")
  }
}
