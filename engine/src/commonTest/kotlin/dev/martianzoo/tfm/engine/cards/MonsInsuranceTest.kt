package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MonsInsuranceTest : CardTest() {
  @Test
  fun `starting production loss reaches every opponent but not its owner`() {
    newGame(PromoCardPack, players = 3)
    val p3 = game.tfm(PLAYER3)

    p1.playCorp(MonsInsurance, 0)
        .expect(
            "48, PROD[4 Megacredit<Player1>], PROD[-2 Megacredit<Player2>], PROD[-2 Megacredit<Player3>]"
        )

    p3.assertProds(-2 to "Megacredit")
  }

  @Test
  fun `starting production loss does not target the solo opponent`() {
    newGame(PromoCardPack, players = 1)

    p1.playCorp(MonsInsurance, 0)
        .expect("48, PROD[4 Megacredit<Me>], PROD[0 Megacredit<SoloOpponent>]")
  }

  @Test
  fun `played by Merger after Manutech, its owner gains only four megacredits`() {
    newGame(PromoCardPack, PreludeExpansion, VenusNextExpansion)
    val p2 = requireP2()
    p1.playCorp(Manutech, 0)
    engine.phase("Prelude")
    val moneyBefore = p1.count("Megacredit")

    p1.playPrelude(Merger) {
      doTask("PlayCard<Class<CorporationCard>, Class<$MonsInsurance>>")
    }

    p1.count("Megacredit") shouldBe moneyBefore + 10 // -42 + 48 starting money + 4 from Manutech
    p1.assertProds(4 to "Megacredit")
    p2.assertProds(-2 to "Megacredit")
  }

  @Test
  fun `Hired Raiders transfer finishes before Mons compensates the victim after each steal`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    engine.phase("Action")
    p1.manual("$MonsInsurance, 10 Megacredit")
    p2.manual("10 Megacredit, ProjectCard")
    p3.manual("4 Steel")
    val monsMoneyBefore = p1.count("Megacredit")

    p2.playProject(HiredRaiders, 1) { doTask("2 Steel<Player2> FROM Steel<Player3>") }
    p2.manual("2 Steel FROM Steel<Player3>")

    p1.count("Megacredit") shouldBe monsMoneyBefore - 6
    p2.count("Steel") shouldBe 4
    p3.count("Steel") shouldBe 0
    p3.count("Megacredit") shouldBe 6
  }

  @Test
  fun `an attack during the Prelude phase requires compensation`() {
    newGame(PromoCardPack, PreludeExpansion)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 Megacredit")
    p2.manual("Plant")
    engine.phase("Prelude")

    p1.manual("-Plant<Player2>")
        .expect("-Plant<Player2>, -3 Megacredit<Player1>, 3 Megacredit<Player2>")
  }

  @Test
  fun `Mons owner pays the victim once for a multi-step production attack`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 Megacredit")
    p2.manual("PROD[3 Plant]")

    p1.manual("PROD[-2 Plant<Player2>]")
        .expect("PROD[-2 Plant<Player2>], -3 Megacredit<Player1>, 3 Megacredit<Player2>")
  }

  @Test
  fun `self-inflicted losses and Engine-run Global Events cause no payout`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance")
    p2.manual("Plant, PROD[Plant]")

    p2.manual("-Plant, PROD[-Plant]").expect("-Plant<Player2>, PROD[-Plant<Player2>]")
    game
        .gameplay(ENGINE)
        .godMode()
        .manual("Plant<Player2>, -Plant<Player2>")
        .expect("0 Megacredit<Player1>, 0 Megacredit<Player2>")
  }

  @Test
  fun `payment is limited to the Mons owner's available megacredits`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p1.manual("$MonsInsurance")
    p1.manual("-Megacredit / Megacredit")
    p1.manual("2 Megacredit")
    p3.manual("Plant")

    p2.manual("-Plant<Player3>")
        .expect("-Plant<Player3>, -2 Megacredit<Player1>, 2 Megacredit<Player3>")
  }

  @Test
  fun `zero payout is settled before the Mons owner gains money later in the action`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance")
    p1.manual("-${p1.count("Megacredit")} Megacredit")
    p2.manual("Plant")

    val manual = p1.godMode().also { it.autoExecMode = NONE }
    manual.addTasks("-Plant<Player2>, 2 Megacredit")
    val attack = game.tasks.extract { it }.single { "Plant<Player2>" in it.instruction.toString() }
    manual.prepareTask(attack.id)
    manual.tryPreparedTask()
    val payout = game.tasks.extract { it }.single { "FROM Megacredit" in it.instruction.toString() }
    manual.prepareTask(payout.id) shouldBe null
    manual.doTask("2 Megacredit<Player1>")

    p1.count("Megacredit") shouldBe 2
    p2.count("Megacredit") shouldBe 0
  }

  @Test
  fun `Pharmacy Union's own loss does not require compensation from Mons`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, $Decomposers")
    p2.manual("$PharmacyUnion")
    val monsMoneyBefore = p1.count("Megacredit")
    val pharmacyMoneyBefore = p2.count("Megacredit")
    val checkpoint = game.timeline.checkpoint()

    p1.manual("MicrobeTag<$Decomposers>")

    p1.count("Megacredit") shouldBe monsMoneyBefore
    p2.count("Megacredit") shouldBe pharmacyMoneyBefore - 4
    game.events
        .changesSince(checkpoint)
        .single {
          it.change.removing?.let(game.reader::resolve) == p2.resolve("Megacredit")
        }
        .actor shouldBe p2.actor
  }

  @Test
  fun `declining an optional removal avoids compensation`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 Megacredit")
    p2.manual("Plant")

    p1.manual("-Plant<Player2>?") { doTask("Ok") }
        .expect("0 Plant<Player2>, 0 Megacredit<Player1>, 0 Megacredit<Player2>")
  }

  @Test
  fun `solo steals and production attacks make Mons pay the general supply`() {
    newGame(PromoCardPack, players = 1)
    engine.phase("Action")
    p1.manual("$MonsInsurance, ProjectCard")

    p1.playProject(HiredRaiders, 1) {
          doTask("3 Megacredit<Me> FROM Megacredit<SoloOpponent>")
        }
        .expect("-1 Megacredit<Me>")
    p1.manual("PROD[-2 Plant<SoloOpponent>]").expect("-3 Megacredit<Me>")
  }

  @Test
  fun `an attack on the Mons owner requires no transfer`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, Plant, 10 Megacredit")

    p2.manual("-Plant<Player1>").expect("-Plant<Player1>")
  }
}
