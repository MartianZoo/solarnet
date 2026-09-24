package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A short game-path recording that keeps card identities in the World throughout setup and play.
 */
internal class ExactCardsReplayTest : AbstractFullGameTest() {
  override val config = GameConfig("CorporateEraExpansion", "Player1", "Player2")

  @Test
  internal fun selectedCorporationAndProjectsKeepTheirExactFacesAndPayForBothProjects() {
    val workflow = TfmWorkflow.Stepwise(agents)
    workflow.setupPhase()

    fun face(back: Expression) =
        back.arguments.single { it.className == cn("Class") }.arguments.single().className

    fun TfmGameplay.chooseSetupCards(retained: Int): Pair<String, List<String>> {
      val corporation = face(list("StandardCorporationCard<Selecting>").elements.first())
      val projects = list("ProjectCard<Selecting>").elements.take(retained).map(::face)
      doTask("StandardCorporationCard<Class<$corporation>, Hand FROM Selecting>")
      projects.forEach { project ->
        doTask("ProjectCard<Class<$project>, Hand FROM Selecting>")
      }
      repeat(10 - retained) { doTask("Ok") }
      return corporation.toString() to projects.map { it.toString() }
    }

    val (corporation, projects) = p1.chooseSetupCards(2)
    val (otherCorporation, otherProjects) = p2.chooseSetupCards(1)
    p1.count("StandardCorporationCard<Class<$corporation>, Hand>") shouldBe 1
    projects.forEach { project -> p1.count("ProjectCard<Class<$project>, Hand>") shouldBe 1 }

    workflow.corporationPhase()
    p1.playCorp(cn(corporation), 2)
    p2.playCorp(cn(otherCorporation), 1)
    p1.count(corporation) shouldBe 1
    projects.forEach { project -> p1.count("ProjectCard<Class<$project>, Hand>") shouldBe 1 }
    otherProjects.forEach { project ->
      p2.count("ProjectCard<Class<$project>, Hand>") shouldBe 1
    }
    p1.count("ProjectCard<Hand>") shouldBe 2
    p2.count("ProjectCard<Hand>") shouldBe 1
    p1.count("MC") shouldBe 51 // CrediCor's 57 starting MC less two project purchases.
    p2.count("MC") shouldBe 27 // Interplanetary Cinematics' 30 less one purchase.
    p1.count("ProjectCard<Selecting>") shouldBe 0
    p1.count("Owed") shouldBe 0
    p1.count("StandardCorporationCard<Class<$corporation>, Hand>") shouldBe 0
  }
}
