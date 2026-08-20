package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.Prelude2Expansion
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class Prelude2CardsTest : CardTest() {
  @Test
  fun `Applied Science supplies a wild tag and converts its science`() {
    newGame(Prelude2Expansion)
    p1.manual("$AppliedScience")

    p1.count("WildTag") shouldBe 1
    p1.count("Science<$AppliedScience>") shouldBe 6

    engine.phase("Action")
    p1.startTurn()
    p1.doTask("PlantTag<WildTagUse<$AppliedScience>>")
    p1.cardAction1(AppliedScience) { doTask("Plant") }.expect("Plant")

    p1.count("Science<$AppliedScience>") shouldBe 5
  }

  @Test
  fun `Nobel Prize supplies its wild tag and immediate gains`() {
    newGame(Prelude2Expansion)
    p1.manual("$NobelPrize")

    p1.count("WildTag") shouldBe 1
    p1.count("Megacredit") shouldBe 5
    p1.count("ProjectCard") shouldBe 2
  }

  @Test
  fun `Board of Directors remains in play and can play another prelude`() {
    newGame(Prelude2Expansion)
    engine.phase("Prelude")
    p1.manual("12, 2 PreludeCard")
    p1.playPrelude(BoardOfDirectors)
    engine.phase("Action")

    p1.cardAction1(BoardOfDirectors) {
      doTask(
          "-12 THEN -Director<$BoardOfDirectors> THEN " +
              "PlayCard<Class<PreludeCard>, Class<$Donation>>"
      )
    }

    p1.count("Director<$BoardOfDirectors>") shouldBe 3
    p1.count("$Donation") shouldBe 1
  }

  @Test
  fun `Preservation Program cancels only the first TR increase each action phase`() {
    newGame(Prelude2Expansion)
    p1.manual("$PreservationProgram")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.manual("TemperatureStep")
    p1.count("TerraformRating") shouldBe startingTr

    p1.manual("TemperatureStep")
    p1.count("TerraformRating") shouldBe startingTr + 1

    engine.nextGeneration(0, 0)
    p1.manual("TemperatureStep")
    p1.count("TerraformRating") shouldBe startingTr + 1
  }

  @Test
  fun `World Government Advisor raises a parameter as Engine`() {
    newGame(Prelude2Expansion)
    p1.manual("$WorldGovernmentAdvisor")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep! BY Engine") }

    engine.count("TemperatureStep") shouldBe 1
    p1.count("TerraformRating") shouldBe startingTr
  }

  @Test
  fun `EcoTec rewards both of its starting tags`() {
    newGame(Prelude2Expansion)

    p1.manual("$EcoTec") {
      doTask("Plant")
      doTask("Plant")
    }

    p1.count("Plant") shouldBe 2
  }

  @Test
  fun `Suitable Infrastructure pays once for each action`() {
    newGame(Prelude2Expansion)
    engine.phase("Prelude")
    p1.manual("$SuitableInfrastructure")
    val beforeTwoProductions = p1.count("Megacredit")

    p1.manual("$DomeFarming")
    p1.count("Megacredit") shouldBe beforeTwoProductions + 2

    p1.manual("50")
    engine.phase("Action")
    val startingMoney = p1.count("Megacredit")

    p1.manual("NewTurn") {
      doTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<PowerPlantSP>")
    }
    p1.count("Megacredit") shouldBe startingMoney - 9

    p1.manual("SecondAction") {
      doTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<PowerPlantSP>")
    }

    p1.count("Megacredit") shouldBe startingMoney - 18
  }
}
