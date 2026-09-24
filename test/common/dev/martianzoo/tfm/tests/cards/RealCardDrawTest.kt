package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.cardTags
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.canonicalPremise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RealCardDrawTest {
  @Test
  internal fun `draw creates an exact back`() {
    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)

    player.runOperation("DrawCard<Class<ProjectCard>, Hand>")

    player.count("ProjectCard<Hand>") shouldBe 1
    player.reader.getComponents("ProjectCard<Hand>").elements.single().typeDependencies.any {
      it.boundType.representedClass != null
    } shouldBe true
    player.count("DeckSpent") shouldBe 1
  }

  @Test
  internal fun `three concrete cards enter selecting and the unchosen two leave`() {
    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)
    lateinit var keptFace: String

    player.runOperation("3 DrawCard<Class<ProjectCard>, Selecting>") {
      val offered = reader.getComponents(player.resolve("ProjectCard<Selecting>")).elements
      offered.size shouldBe 3
      val selected = offered.first()
      keptFace =
          selected.typeDependencies
              .mapNotNull { it.boundType.representedClass }
              .single()
              .className
              .toString()
      player.addTasks("ProjectCard<Hand FROM Selecting>")
      doTask("ProjectCard<Class<$keptFace>, Hand FROM Selecting>")
    }

    player.count("ProjectCard<Selecting>") shouldBe 0
    player.count("ProjectCard<Class<$keptFace>, Hand>") shouldBe 1
    player.count("DeckSpent") shouldBe 3
    shouldThrow<GameplayException> {
      player.runOperation("ProjectCard<Class<$keptFace>, Selecting>")
    }
  }

  @Test
  internal fun `a runtime counted move preserves each card's face`() {
    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)
    player.runOperation("3 DrawCard<Class<ProjectCard>, Hand>")
    val faces =
        player.reader.getComponents(player.resolve("ProjectCard<Hand>")).elements.map { back ->
          back.typeDependencies.mapNotNull { it.boundType.representedClass }.single().className
        }
    faces.size shouldBe 3

    player.runOperation("3 ProjectCard<Selecting FROM Hand>") {
      faces.forEach { face -> doTask("ProjectCard<Class<$face>, Selecting FROM Hand>") }
      val moved =
          reader.getComponents(player.resolve("ProjectCard<Selecting>")).elements.map { back ->
            back.typeDependencies.mapNotNull { it.boundType.representedClass }.single().className
          }
      moved.toSet() shouldBe faces.toSet()
      player.count("ProjectCard<Hand>") shouldBe 0
    }

    player.count("ProjectCard<Hand>") shouldBe 0
    player.count("ProjectCard<Selecting>") shouldBe 0
    player.count("DeckSpent") shouldBe 3
  }

  @Test
  internal fun `filtered search skips nonmatching faces without making backs for them`() {
    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)

    player.runOperation("SearchForCard(HAS PrintedTag<Class<ScienceTag>>)")

    player.count("ProjectCard<Class<AdaptationTechnology>, Hand>") shouldBe 1
    player.count("ProjectCard<Hand>") shouldBe 1
    player.count("DeckSpent") shouldBe 2
  }

  @Test
  internal fun `reference search finds a card in the enabled Venus deck`() {
    val player = Engine.newGame(canonicalPremise(VenusNextExpansion)).testAgent(PLAYER1)

    player.runOperation("SearchForCard(HAS ReferenceTo<Class<Floater>>)")

    player.count("ProjectCard<Class<AerialMappers>, Hand>") shouldBe 1
    player.count("ProjectCard<Hand>") shouldBe 1
  }

  @Test
  internal fun `tagless search skips tagged cards`() {
    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)

    player.runOperation("SearchForCard(HAS MAX 0 PrintedTag)")

    val back = player.reader.getComponents(player.resolve("ProjectCard<Hand>")).elements.single()
    val face = back.typeDependencies.mapNotNull { it.boundType.representedClass }.single()
    cardTags(player.reader.tfmCatalog.card(face.className)).size shouldBe 0
    player.count("DeckSpent") shouldBe 23
  }

  @Test
  internal fun `Invention Contest offers three exact backs and keeps one`() {
    val player = Engine.newGame(canonicalPremise(CorporateEraExpansion)).testAgent(PLAYER1)
    lateinit var keptFace: String
    val event = player.resolve("PlayedEvent<Class<InventionContest>>")
    check(!event.abstract) { "event=${event.expressionFull} deps=${event.typeDependencies}" }

    player.runOperation("InventionContest") {
      val offered = reader.getComponents(player.resolve("ProjectCard<Selecting>")).elements
      offered.size shouldBe 3
      keptFace =
          offered
              .first()
              .typeDependencies
              .mapNotNull { it.boundType.representedClass }
              .single()
              .className
              .toString()
      doTask("ProjectCard<Class<$keptFace>, Hand FROM Selecting>")
    }

    player.count("ProjectCard<Selecting>") shouldBe 0
    player.count("ProjectCard<Class<$keptFace>, Hand>") shouldBe 1
    player.count("PlayedEvent<Class<InventionContest>>") shouldBe 1
    shouldThrow<GameplayException> {
      player.runOperation("ProjectCard<Class<InventionContest>, Hand>")
    }
  }

  @Test
  internal fun `playing a card consumes its exact back`() {
    val player = Engine.newGame(canonicalPremise(CorporateEraExpansion)).testAgent(PLAYER1)
    player.runOperation("ProjectCard<Class<InventionContest>, Hand>, 2 MC")

    player.runOperation("PlayCard<Class<ProjectCard>, Class<InventionContest>, Hand>") {
      doTask("2 Pay<> FROM MC")
      val offered = reader.getComponents(player.resolve("ProjectCard<Selecting>")).elements
      val kept =
          offered
              .first()
              .typeDependencies
              .mapNotNull { it.boundType.representedClass }
              .single()
              .className
      doTask("ProjectCard<Class<$kept>, Hand FROM Selecting>")
    }

    player.count("ProjectCard<Class<InventionContest>, Hand>") shouldBe 0
    player.count("PlayedEvent<Class<InventionContest>>") shouldBe 1
  }

  @Test
  internal fun `standard setup deals concrete project and corporation offers`() {
    val world = Engine.newGame(canonicalPremise(players = 2))
    val agents = world.testAgents()
    val player = agents[PLAYER1]
    val other = agents[PLAYER2]

    TfmWorkflow.Stepwise(agents).setupPhase()

    val projects = player.list("ProjectCard<Selecting>").elements
    val corporations = player.list("StandardCorporationCard<Selecting>").elements
    projects.size shouldBe 10
    corporations.size shouldBe 2
    fun face(back: Expression) =
        back.arguments.single { it.className == cn("Class") }.arguments.single().className
    projects.map(::face).toSet().size shouldBe 10
    player.count("DeckSpent") shouldBe 24

    val corporation = face(corporations.first())
    val project = face(projects.first())
    player.doTask("StandardCorporationCard<Class<$corporation>, Hand FROM Selecting>")
    player.doTask("ProjectCard<Class<$project>, Hand FROM Selecting>")
    repeat(9) { player.doTask("Ok") }
    val otherCorporation = face(other.list("StandardCorporationCard<Selecting>").elements.first())
    other.doTask("StandardCorporationCard<Class<$otherCorporation>, Hand FROM Selecting>")
    repeat(10) { other.doTask("Ok") }

    player.count("ProjectCard<Class<$project>, Hand>") shouldBe 1
    player.count("ProjectCard<Selecting>") shouldBe 0
    player.count("StandardCorporationCard<Class<$corporation>, Hand>") shouldBe 1
    player.count("StandardCorporationCard<Selecting>") shouldBe 0
  }
}
