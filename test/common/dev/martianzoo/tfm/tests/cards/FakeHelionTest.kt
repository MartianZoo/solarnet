package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

// FakeHelion approximates Helion's payments. Its failures characterize this stand-in, not Canon.
internal class FakeHelionTest : TfmSandboxTest() {
  private val fakeHelion = cn("FakeHelion")

  @Test
  internal fun `Can buy starting cards with heat`() {
    newTestGameBeforeCorporationPlay(addOptions = "fakes:cards")
    kim.exMachina("15 Heat")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<FakeHelion>>",
              "FakeHelion FROM CorporationCard",
              "42 MC",
              "PROD[3 Heat]",
          )
          doTasks("15 Pay<Class<Heat>> FROM Heat", "Ok", "5 ProjectCard<Hand FROM Selecting>")
        }
        .expect("42 MC, -15 Heat, 5 ProjectCard")
  }

  @Test
  internal fun `Can pay an entire project cost with heat when it has no money`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(4, "Heat")

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "4 Pay<Class<Heat>> FROM Heat", "Ok")
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("Mine, -4 Heat, 0 MC")
  }

  @Test
  internal fun `May choose heat even when money covers the entire project cost`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.setToExMachina(4, "MC")
    kim.setToExMachina(4, "Heat")

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "4 Pay<Class<Heat>> FROM Heat", "Ok")
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("Mine, -4 Heat, 0 MC")
  }

  @Test
  internal fun `May combine heat and money on the same bill`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.setToExMachina(2, "MC")
    kim.setToExMachina(2, "Heat")

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "2 Pay<Class<Heat>> FROM Heat")
          doTasks("2 Pay<> FROM MC", "Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("Mine, -2 Heat, -2 MC")
  }

  @Test
  internal fun `May keep heat and pay a money bill entirely with money`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.setToExMachina(4, "MC")
    kim.setToExMachina(4, "Heat")

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "Ok", "4 Pay<> FROM MC")
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("Mine, 0 Heat, -4 MC")
  }

  @Test
  internal fun `Can pay a standard project with heat`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(11, "Heat")

    kim.inTurn {
          doTasks(
              "UseStandardProject<PowerPlantProject>",
              "11 Pay<Class<Heat>> FROM Heat",
              "Ok",
              "PROD[Energy]",
          )
        }
        .expect("-11 Heat, PROD[Energy], 0 MC")
  }

  @Ignore // Business Empire still removes M€ directly, outside FakeHelion's payment scope.
  @Test
  internal fun `Can use heat for a self-resolved Prelude money removal`() {
    playBusinessEmpireWithHeat().expect("BusinessEmpire, PROD[6 MC], -6 Heat, 0 MC")
  }

  @Test
  internal fun `BUG - FakeHelion cannot use heat for Business Empire's money removal`() {
    shouldThrow<TaskException> { playBusinessEmpireWithHeat() }
    kim.count("BusinessEmpire") shouldBe 0
    kim.count("Heat") shouldBe 6
  }

  private fun playBusinessEmpireWithHeat(): TaskResult {
    newTestGame(addOptions = "fakes:cards, PreludeExpansion", kimCorporation = fakeHelion)
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(6, "Heat")

    return kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BusinessEmpire>>",
          "BusinessEmpire FROM PreludeCard",
          "PROD[6 MC]",
          "6 Pay<Class<Heat>> FROM Heat",
          "Ok",
          "Ok",
      )
    }
  }

  @Ignore // Industrial Complex's copied Prelude effect still removes M€ directly.
  @Test
  internal fun `Can use heat for Industrial Complex's money removal`() {
    playIndustrialComplexWithHeat().expect("IndustrialComplex, -18 Heat, 0 MC")
  }

  @Test
  internal fun `BUG - FakeHelion cannot use heat for Industrial Complex's money removal`() {
    shouldThrow<TaskException> { playIndustrialComplexWithHeat() }
    kim.count("IndustrialComplex") shouldBe 0
    kim.count("Heat") shouldBe 18
  }

  private fun playIndustrialComplexWithHeat(): TaskResult {
    newTestGame(
        addOptions = "fakes:cards, PreludeExpansion, IndustrialComplex",
        kimCorporation = fakeHelion,
    )
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(18, "Heat")

    return kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
          "IndustrialComplex FROM PreludeCard",
          "18 Pay<Class<Heat>> FROM Heat",
          "Ok",
      )
      selectOnlyTask()
      doTasks(
          "PROD[2 MC]",
          "PROD[2 Steel]",
          "PROD[2 Titanium]",
          "PROD[2 Plant]",
          "PROD[2 Energy]",
          "PROD[2 Heat]",
      )
    }
  }

  @Ignore // The nested action offers a direct M€ removal, not a heat payment.
  @Test
  internal fun `Can use heat for Board of Directors' action payment`() {
    useBoardOfDirectorsWithHeat().expect("-12 Heat, -Director<BoardOfDirectors>")
  }

  @Test
  internal fun `BUG - FakeHelion cannot use heat for Board of Directors' action payment`() {
    shouldThrow<TaskException> { useBoardOfDirectorsWithHeat() }
    kim.count("Director<BoardOfDirectors>") shouldBe 4
    kim.count("Heat") shouldBe 12
  }

  private fun useBoardOfDirectorsWithHeat(): TaskResult {
    newTestGame(
        addOptions = "fakes:cards, PreludeExpansion, BoardOfDirectors",
        kimCorporation = fakeHelion,
    )
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      doTask("4 Director<BoardOfDirectors>")
    }
    advanceUntilActionPhase()
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(12, "Heat")

    return kim.inTurn {
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      doTask("PreludeCard<Kim, Hand>")
      doTasks("12 Pay<Class<Heat>> FROM Heat", "Ok")
      doTask("-Director<BoardOfDirectors>")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<Donation>>",
          "Donation FROM PreludeCard",
          "21 MC",
      )
    }
  }

  @Ignore // Briber's extra 12 M€ is a direct removal outside FakeHelion's payment scope.
  @Test
  internal fun `Can use heat for Briber's extra payment`() {
    claimBriberWithHeat().expect("-20 Heat, Briber, 0 MC")
  }

  @Test
  internal fun `BUG - FakeHelion cannot use heat for Briber's extra payment`() {
    shouldThrow<TaskException> { claimBriberWithHeat() }
    kim.count("Briber") shouldBe 0
    kim.count("Heat") shouldBe 20
  }

  private fun claimBriberWithHeat(): TaskResult {
    newTestGame(
        addOptions = "fakes:cards, Briber, Builder, Engineer",
        kimCorporation = fakeHelion,
    )
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(20, "Heat")

    return kim.inTurn {
      doTasks("ClaimMilestone<Class<Briber>>", "8 Pay<Class<Heat>> FROM Heat", "Ok")
      doTask("Briber")
      doTasks("12 Pay<Class<Heat>> FROM Heat", "Ok")
    }
  }

  @Ignore // Merger's 42 M€ is a direct removal after its selected corporation enters play.
  @Test
  internal fun `Can use heat for Merger's corporation payment`() {
    playMergerWithHeat().expect("Celestic, -42 Heat, 42 MC")
  }

  @Test
  internal fun `BUG - FakeHelion cannot use heat for Merger's corporation payment`() {
    shouldThrow<TaskException> { playMergerWithHeat() }
    kim.count("Merger") shouldBe 0
    kim.count("Heat") shouldBe 42
  }

  private fun playMergerWithHeat(): TaskResult {
    newTestGame(
        addOptions = "fakes:cards, PreludeExpansion, Merger",
        kimCorporation = fakeHelion,
    )
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(42, "Heat")

    return kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      doTask("4 CorporationCard<Kim, Selecting>")
      doTask("PlayCard<Class<CorporationCard>, Class<Celestic>>")
      doTask("Celestic<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTask("42 MC")
      doTasks("42 Pay<Class<Heat>> FROM Heat", "Ok")
    }
  }

  @Test
  internal fun `Can pay a variable card action with heat`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.exMachina("EnergyMarket")
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(6, "Heat")

    kim.inTurn {
          doTask("UseCardAction<EnergyMarket, Action1>")
          selectOnlyTask(x = 3)
          doTasks("6 Pay<Class<Heat>> FROM Heat", "Ok", "3 Energy")
        }
        .expect("-6 Heat, 3 Energy, 0 MC")
  }

  @Ignore // Stormcraft accepts floaters toward Heat debt, but FakeHelion creates M€ debt.
  @Test
  internal fun `Can use Stormcraft floaters as two money each`() {
    playMineWithStormcraftFloaters().expect("Mine, -2 Floater<StormcraftIncorporated>, 0 MC")
  }

  @Test
  internal fun `BUG - FakeHelion cannot use Stormcraft floaters for a money bill`() {
    shouldThrow<TaskException> { playMineWithStormcraftFloaters() }
    kim.count("Mine") shouldBe 0
    kim.count("Floater<StormcraftIncorporated>") shouldBe 2
  }

  private fun playMineWithStormcraftFloaters(): TaskResult {
    newTestGame(
        addOptions = "fakes:cards, ColoniesExpansion",
        kimCorporation = fakeHelion,
    )
    kim.exMachina("StormcraftIncorporated, 2 Floater<StormcraftIncorporated>")
    kim.setToExMachina(0, "MC")

    return kim.inTurn {
      doTasks(
          "PlayProject<Class<Mine>>",
          "2 PayFromCard<StormcraftIncorporated> FROM Floater<StormcraftIncorporated>",
          "Ok",
      )
      doTasks("Mine FROM ProjectCard", "PROD[Steel]")
    }
  }

  @Test
  internal fun `Cannot use money instead of heat for temperature conversion`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.setToExMachina(8, "MC")
    kim.setToExMachina(0, "Heat")

    shouldThrow<TaskException> {
      kim.inTurn { doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM MC") }
    }
    kim.count("MC") shouldBe 8
    kim.count("TemperatureStep") shouldBe 0
  }

  @Test
  internal fun `An opponent can steal money but not heat as money`() {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.setToExMachina(2, "MC")
    kim.setToExMachina(8, "Heat")

    stan
        .inTurn {
          doTasks(
              "PlayProject<Class<HiredRaiders>>",
              "1 Pay<> FROM MC",
              "HiredRaiders FROM ProjectCard",
              "2 MC<Stan FROM Kim>",
          )
        }
        .expect("-2 MC<Kim>, 0 Heat<Kim>")
  }

  @Test
  internal fun `A forced money loss cannot take heat or require more than the money held`() {
    newTestGame(
        addOptions = "fakes:cards, PreludeExpansion, Recession",
        kimCorporation = fakeHelion,
    )
    kim.setToExMachina(2, "MC")
    kim.setToExMachina(8, "Heat")

    stan
        .inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<Recession>>",
              "Recession FROM PreludeCard",
              "10 MC",
          )
          selectOnlyTask()
          doTasks("-5* MC<Kim>", "PROD[-MC<Kim>]")
          doTasks("-5* MC<Rob>", "PROD[-MC<Rob>]")
        }
        .expect("-2 MC<Kim>, 0 Heat<Kim>")
  }

  // R14: a short Mons Insurance payout spends available money. Holding heat does not reduce the
  // amount of money that must be transferred when the owner chooses a partial payout.
  @Test
  internal fun `Mons Insurance pays its available single money before leaving heat untouched`() {
    makeMonsPayout(money = 1, heat = 6, payout = 1).expect("-MC<Kim>, MC<Rob>, 0 Heat<Kim>")
  }

  @Test
  internal fun `Mons Insurance pays its available two money before leaving heat untouched`() {
    makeMonsPayout(money = 2, heat = 6, payout = 2).expect("-2 MC<Kim>, 2 MC<Rob>, 0 Heat<Kim>")
  }

  @Test
  internal fun `Mons Insurance can choose a full money payout while keeping heat`() {
    makeMonsPayout(money = 3, heat = 6, payout = 3).expect("-3 MC<Kim>, 3 MC<Rob>, 0 Heat<Kim>")
  }

  @Ignore // Mons transfers M€ directly; FakeHelion's payment listener does not apply.
  @Test
  internal fun `Mons Insurance can pay the full three money using only heat`() {
    makeMonsPayout(money = 0, heat = 3, payout = 3).expect("-3 Heat<Kim>, 0 MC<Kim>, 3 MC<Rob>")
  }

  @Test
  internal fun `BUG - FakeHelion's heat cannot fund a Mons Insurance payout`() {
    makeMonsPayout(money = 0, heat = 3, payout = 3).expect("0 Heat<Kim>, 0 MC<Kim>, 0 MC<Rob>")
  }

  @Ignore // Mons transfers only available M€ and does not offer a mixed Heat payment.
  @Test
  internal fun `Mons Insurance can combine two money and one heat for a full payout`() {
    makeMonsPayout(money = 2, heat = 1, payout = 3).expect("-2 MC<Kim>, -Heat<Kim>, 3 MC<Rob>")
  }

  @Test
  internal fun `BUG - FakeHelion cannot complete a mixed Mons Insurance payout`() {
    makeMonsPayout(money = 2, heat = 1, payout = 3).expect("-2 MC<Kim>, 0 Heat<Kim>, 2 MC<Rob>")
  }

  private fun makeMonsPayout(money: Int, heat: Int, payout: Int): TaskResult {
    newTestGame(addOptions = "fakes:cards", kimCorporation = fakeHelion)
    kim.exMachina("MonsInsurance")
    kim.setToExMachina(money, "MC")
    kim.setToExMachina(heat, "Heat")
    rob.exMachina("2 Steel")

    return stan.inTurn {
      doTasks(
          "PlayProject<Class<HiredRaiders>>",
          "1 Pay<> FROM MC",
          "HiredRaiders FROM ProjectCard",
          "2 Steel<Stan FROM Rob>",
      )
      kim.doTask("$payout MC<Rob FROM Kim>")
    }
  }
}
