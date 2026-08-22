package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class WildTagTest : CardTest() {
  @Test
  fun `Nested standard projects preserve pending payments offer positions and wild tags`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.manual("2 PreludeCard")
    engine.phase("Prelude")
    p1.startTurn()

    p1.playPrelude(ResearchNetwork)
    p1.startTurn()
    p1.doTask("PowerTag<WildTagUse<$ResearchNetwork>>")
    p1.playPrelude(FakeEstablishedMethods) {
      val offers =
          game.tasks
              .extract { it }
              .filter {
                val instruction = it.instruction.toString()
                it.assignee == p1.actor &&
                    "UseAction" in instruction &&
                    "StandardAction" in instruction
              }
              .map { it.id }
      offers.size shouldBe 2

      repeat(2) { projectIndex ->
        doTask("UseAction1<UseStandardProjectSA>")
        doTask("UseAction1<PowerPlantSP>")

        tasks
            .extract { it }
            .any {
              val instruction = it.instruction.toString()
              "Pay" in instruction && "Megacredit" in instruction
            } shouldBe true
        p1.count("Owed<>") shouldBe 11
        p1.count("WildTagUse<$ResearchNetwork>") shouldBe 1

        p1.pay(11)

        p1.count("WildTagUse<$ResearchNetwork>") shouldBe 1
        val remainingOffers =
            game.tasks
                .extract { it }
                .filter {
                  val instruction = it.instruction.toString()
                  it.assignee == p1.actor &&
                      "UseAction" in instruction &&
                      "StandardAction" in instruction
                }
                .map { it.id }
        remainingOffers shouldBe offers.drop(projectIndex + 1)
      }
    }

    p1.count("WildTagUse") shouldBe 0
  }

  @Test
  fun `Another player's earlier task does not change a wild tag offer's position`() {
    newGame(PreludeExpansion)
    val p2 = requireP2()
    p1.manual("$ResearchCoordination")
    engine.phase("Action")

    val otherPlayerTask = p2.godMode().addTasks("UseAction<StandardAction>?").single()
    p1.startTurn()

    p1.stdAction("SellPatents") { abort() }

    p2.godMode().dropTask(otherPlayerTask)
    // The aborted synthetic action leaves its temporary holder; remove it before ending the test.
    p1.godMode().manual("-WildTagUse<$ResearchCoordination>")
    p1.count("WildTagUse") shouldBe 0
  }

  @Test
  fun `Temporary tags count normally without triggering printed-tag effects`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    p1.manual(
        "$ResearchCoordination, $ResearchNetwork, $PointLuna, $MarsUniversity, 2 ProjectCard"
    ) {
      doTask("Ok")
    }
    p1.count("WildTag") shouldBe 2
    val cardsBeforeWildTags = p1.count("ProjectCard")
    engine.phase("Action")
    p1.startTurn()

    p1.count("WildTagUse") shouldBe 2
    p1.doTask("EarthTag<WildTagUse<$ResearchCoordination>>")
    p1.doTask("ScienceTag<WildTagUse<$ResearchNetwork>>")

    p1.count("EarthTag") shouldBe 2
    p1.count("EarthTag<CardFront>") shouldBe 1
    p1.count("ScienceTag") shouldBe 2
    p1.count("ScienceTag<CardFront>") shouldBe 1
    p1.count("ProjectCard") shouldBe cardsBeforeWildTags

    p1.sellPatents(1)

    p1.count("EarthTag") shouldBe 1
    p1.count("ScienceTag") shouldBe 1
    p1.count("WildTagUse") shouldBe 0
    p1.startTurn()
    p1.count("WildTagUse") shouldBe 2
    p1.pass()
    p1.count("WildTagUse") shouldBe 0
  }

  @Test
  fun `Robotic Workforce follows a claimed building wild tag`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    p1.manual("$ResearchNetwork, ProjectCard, 9 Megacredit")
    p1.count("WildTag") shouldBe 1
    engine.phase("Action")
    p1.startTurn()
    p1.doTask("BuildingTag<WildTagUse<$ResearchNetwork>>")

    p1.playProject(RoboticWorkforce, 9) {
          doTask("CopyProductionBox<$ResearchNetwork>")
        }
        .expect("PROD[1]")

    p1.count("BuildingTag") shouldBe 0
  }

  @Test
  fun `Excentric Sponsor can play a card whose requirement needs the wild tag`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    p1.manual("$Teractor, 2 PreludeCard")
    engine.phase("Prelude")
    p1.startTurn()
    p1.playPrelude(ResearchNetwork)
    p1.startTurn()
    p1.doTask("EarthTag<WildTagUse<$ResearchNetwork>>")

    p1.playPrelude(ExcentricSponsor) { p1.playProject(SpaceHotels, 0) }.expect("PROD[4]")
  }

  @Test
  fun `A per-tag production box counts the wild tag`() {
    newGame(PreludeExpansion)
    p1.manual("2 PreludeCard")
    engine.phase("Prelude")
    p1.startTurn()
    p1.playPrelude(ResearchNetwork)
    p1.startTurn()
    p1.doTask("PowerTag<WildTagUse<$ResearchNetwork>>")

    p1.playPrelude(ExcentricSponsor) { p1.playProject(PowerGrid, 0) }.expect("PROD[2 Energy]")
  }
}
