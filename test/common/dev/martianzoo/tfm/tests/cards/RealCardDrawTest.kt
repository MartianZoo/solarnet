package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.cardTags
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestOption.BeginnerVariant
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
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
  internal fun `prelude and beginner corporation draws also have exact faces`() {
    val player =
        Engine.newGame(canonicalPremise(PreludeExpansion, BeginnerVariant)).testAgent(PLAYER1)

    player.runOperation(
        "DrawCard<Class<PreludeCard>, Hand>, DrawCard<Class<BeginnerCorporationCard>, Hand>"
    )

    player.count("PreludeCard<Hand>") shouldBe 1
    player.count("BeginnerCorporationCard<Hand>") shouldBe 1
    player.reader
        .getComponents(player.resolve("PreludeCard<Hand>"))
        .elements
        .single()
        .typeDependencies
        .any { it.boundType.representedClass != null } shouldBe true
    player.reader
        .getComponents(player.resolve("BeginnerCorporationCard<Hand>"))
        .elements
        .single()
        .typeDependencies
        .any { it.boundType.representedClass != null } shouldBe true
    player.count("DeckSpent") shouldBe 2
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

    player.runOperation("SearchForTag<Class<ScienceTag>>")

    player.count("ProjectCard<Class<AdaptationTechnology>, Hand>") shouldBe 1
    player.count("ProjectCard<Hand>") shouldBe 1
    player.count("DeckSpent") shouldBe 2
  }

  @Test
  internal fun `reference search finds a card in the enabled Venus deck`() {
    val player = Engine.newGame(canonicalPremise(VenusNextExpansion)).testAgent(PLAYER1)

    player.runOperation("SearchForReference<Class<Floater>>")

    player.count("ProjectCard<Class<AerialMappers>, Hand>") shouldBe 1
    player.count("ProjectCard<Hand>") shouldBe 1
  }

  @Test
  internal fun `tagless search skips tagged cards`() {
    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)

    player.runOperation("SearchForUntaggedCard")

    val back = player.reader.getComponents(player.resolve("ProjectCard<Hand>")).elements.single()
    val face = back.typeDependencies.mapNotNull { it.boundType.representedClass }.single()
    cardTags(player.reader.tfmCatalog.card(face.className)).size shouldBe 0
    player.count("DeckSpent") shouldBe 23
  }

  @Test
  internal fun `a search without any matching card exhausts the deck without creating backs`() {
    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)

    player.runOperation("SearchForReference<Class<DeckSpent>>")

    player.count("ProjectCard<Hand>") shouldBe 0
    val spent = player.count("DeckSpent")
    (spent > 0) shouldBe true
    player.runOperation("SearchForReference<Class<DeckSpent>>")
    player.count("DeckSpent") shouldBe spent
  }

  @Test
  internal fun `a directly supplied exact back cannot also be dealt`() {
    val firstGame = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)
    firstGame.runOperation("DrawCard<Class<ProjectCard>, Hand>")
    val firstFace =
        firstGame.reader
            .getComponents(firstGame.resolve("ProjectCard<Hand>"))
            .elements
            .single()
            .typeDependencies
            .mapNotNull { it.boundType.representedClass }
            .single()
            .className

    val player = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)
    player.runOperation("ProjectCard<Class<$firstFace>, Hand>")
    player.runOperation("DrawCard<Class<ProjectCard>, Hand>")

    player.count("ProjectCard<Class<$firstFace>, Hand>") shouldBe 1
    player.count("ProjectCard<Hand>") shouldBe 2
  }

  @Test
  internal fun `printed reveal test distinguishes matching and nonmatching fronts`() {
    val matching = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)
    matching.runOperation(
        "ProjectCard<Class<AdaptationTechnology>, Revealed> THEN " +
            "EACH @ProjectCard<Revealed> { " +
            "(PrintedTagOf<@ProjectCard, Class<ScienceTag>>: MC) OR " +
            "(MAX 0 PrintedTagOf<@ProjectCard, Class<ScienceTag>>: Ok) }"
    )
    matching.count("MC") shouldBe 1
    matching.count("ProjectCard<Revealed>") shouldBe 0

    val nonmatching = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)
    nonmatching.runOperation(
        "ProjectCard<Class<AdaptationTechnology>, Revealed> THEN " +
            "EACH @ProjectCard<Revealed> { " +
            "(PrintedTagOf<@ProjectCard, Class<MicrobeTag>>: MC) OR " +
            "(MAX 0 PrintedTagOf<@ProjectCard, Class<MicrobeTag>>: Ok) }"
    )
    nonmatching.count("MC") shouldBe 0
    nonmatching.count("ProjectCard<Revealed>") shouldBe 0
  }

  @Test
  internal fun `Pets can retain selected backs by their represented front's printed tag`() {
    val player = Engine.newGame(canonicalPremise(VenusNextExpansion)).testAgent(PLAYER1)

    player.runOperation(
        "ProjectCard<Class<AerialMappers>, Selecting> THEN " +
            "ProjectCard<Class<AdaptationTechnology>, Selecting> THEN " +
            "EACH @ProjectCard<Selecting> { " +
            "(PrintedTagOf<@ProjectCard, Class<VenusTag>>: " +
            "MoveSelectedCard<@ProjectCard>) OR " +
            "(MAX 0 PrintedTagOf<@ProjectCard, Class<VenusTag>>: Ok) }"
    )

    player.count("ProjectCard<Class<AerialMappers>, Hand>") shouldBe 1
    player.count("ProjectCard<Hand>") shouldBe 1
    player.count("ProjectCard<Selecting>") shouldBe 0
  }

  @Test
  internal fun `Search for Life action reveals a concrete nonmatching card`() {
    val world = Engine.newGame(canonicalPremise())
    val player = world.testTfm(PLAYER1)
    val admin = world.testTfm(ADMIN)
    admin.runOperation("GenerationScope")
    admin.phase("Action")
    player.runOperation("SearchForLife, 1 MC")

    player.cardAction1(cn("SearchForLife")) {
      player.count("ProjectCard<Revealed>") shouldBe 1
    }

    player.count("Science<SearchForLife>") shouldBe 0
    player.count("ProjectCard<Revealed>") shouldBe 0
    player.count("DeckSpent") shouldBe 1
  }

  @Test
  internal fun `Venus Orbital Survey offers exact backs and purchases the unretained cards`() {
    val world =
        Engine.newGame(canonicalPremise(PreludeExpansion, Prelude2CardPack, VenusNextExpansion))
    val player = world.testTfm(PLAYER1)
    player.autoExecPolicy = CONCRETE
    val admin = world.testTfm(ADMIN)
    admin.runOperation("GenerationScope")
    admin.phase("Action")
    player.runOperation("VenusOrbitalSurvey, 6 MC")

    player.cardAction1(cn("VenusOrbitalSurvey")) {
      player.buyCards(2)
    }

    player.count("ProjectCard<Hand>") shouldBe 2
    player.count("ProjectCard<Selecting>") shouldBe 0
    player.count("MC") shouldBe 0
    player.count("DeckSpent") shouldBe 2
  }

  @Test
  internal fun `Venus Orbital Survey retains a Venus card before offering the other for purchase`() {
    val world =
        Engine.newGame(canonicalPremise(PreludeExpansion, Prelude2CardPack, VenusNextExpansion))
    val player = world.testTfm(PLAYER1)
    val admin = world.testTfm(ADMIN)
    admin.runOperation("GenerationScope")
    admin.phase("Action")
    player.runOperation("5 DrawCard<Class<ProjectCard>, Hand>, VenusOrbitalSurvey, 3 MC")
    player.autoExecPolicy = CONCRETE

    player.cardAction1(cn("VenusOrbitalSurvey")) {
      player.count("ProjectCard<Class<AerialMappers>, Hand>") shouldBe 1
      player.count("ProjectCard<Selecting>") shouldBe 1
      player.buyCards(1)
    }

    player.count("ProjectCard<Class<AerialMappers>, Hand>") shouldBe 1
    player.count("ProjectCard<Hand>") shouldBe 7
    player.count("ProjectCard<Selecting>") shouldBe 0
    player.count("DeckSpent") shouldBe 7
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
  internal fun `recovering a played event restores the same card face`() {
    val player = Engine.newGame(canonicalPremise(CorporateEraExpansion)).testAgent(PLAYER1)
    player.runOperation("InventionContest") {
      val offered = reader.getComponents(player.resolve("ProjectCard<Selecting>")).elements
      val face =
          offered
              .first()
              .typeDependencies
              .mapNotNull { it.boundType.representedClass }
              .single()
              .className
      doTask("ProjectCard<Class<$face>, Hand FROM Selecting>")
    }

    player.runOperation("RecoverPlayedEvent<PlayedEvent<Owner>>") {
      doTask("RecoverPlayedEvent<PlayedEvent<Class<InventionContest>>>")
    }

    player.count("PlayedEvent<Class<InventionContest>>") shouldBe 0
    player.count("ProjectCard<Class<InventionContest>, Hand>") shouldBe 1
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

  @Test
  internal fun `beginner setup chooses an exact beginner back while another player takes a standard offer`() {
    val world = Engine.newGame(canonicalPremise(BeginnerVariant, players = 2))
    val agents = world.testAgents()
    val beginner = agents[PLAYER1]
    val standard = agents[PLAYER2]
    beginner.autoExecPolicy = NONE
    standard.autoExecPolicy = NONE
    fun face(back: Expression) =
        back.arguments.single { it.className == cn("Class") }.arguments.single().className

    TfmWorkflow.Stepwise(agents).setupPhase()
    beginner.doTask("DrawCard<Class<BeginnerCorporationCard>, Hand>")
    // The chosen setup arm fixes the path; eager settlement then deals its remaining cards.
    beginner.autoExecPolicy = EAGER
    standard.doTask("DrawCard<Class<StandardCorporationCard>, Selecting>")
    standard.autoExecPolicy = EAGER
    val standardFace = face(standard.list("StandardCorporationCard<Selecting>").elements.first())
    standard.doTask("StandardCorporationCard<Class<$standardFace>, Hand FROM Selecting>")
    val projectFace = face(standard.list("ProjectCard<Selecting>").elements.first())
    standard.doTask("ProjectCard<Class<$projectFace>, Hand FROM Selecting>")
    repeat(9) { standard.doTask("Ok") }

    val beginnerFace = face(beginner.list("BeginnerCorporationCard<Hand>").elements.single())
    beginner.count("BeginnerCorporationCard<Class<$beginnerFace>, Hand>") shouldBe 1
    beginner.list("ProjectCard<Hand>").elements.map(::face).toSet().size shouldBe 10
    beginner.count("StandardCorporationCard") shouldBe 0
    beginner.count("ProjectCard<Class<$projectFace>, Hand>") shouldBe 0
    standard.count("StandardCorporationCard<Class<$standardFace>, Hand>") shouldBe 1
    standard.count("StandardCorporationCard<Selecting>") shouldBe 0
    standard.count("ProjectCard<Class<$projectFace>, Hand>") shouldBe 1
    standard.count("ProjectCard<Hand>") shouldBe 1
    standard.count("ProjectCard<Selecting>") shouldBe 0
    beginner.count("DeckSpent") shouldBe 1 + 10 + 2 + 10
  }

  @Test
  internal fun `research buys from four concrete project offers`() {
    val world = Engine.newGame(canonicalPremise(players = 1))
    val admin = world.testTfm(ADMIN)
    val player = world.testTfm(PLAYER1)
    player.autoExecPolicy = CONCRETE
    player.runOperation("6 MC")

    admin.phase("Research") {
      val offeredFaces =
          player.reader
              .getComponents(player.resolve("ProjectCard<Selecting>"))
              .elements
              .map { back ->
                back.typeDependencies
                    .mapNotNull { it.boundType.representedClass }
                    .single()
                    .className
              }
              .toSet()
      offeredFaces.size shouldBe 4
      player.buyCards(2)
      val boughtFaces =
          player.reader.getComponents(player.resolve("ProjectCard<Hand>")).elements.map { back ->
            back.typeDependencies.mapNotNull { it.boundType.representedClass }.single().className
          }
      boughtFaces.size shouldBe 2
      boughtFaces.all(offeredFaces::contains) shouldBe true
    }

    player.count("ProjectCard<Hand>") shouldBe 2
    player.count("ProjectCard<Selecting>") shouldBe 0
    player.count("MC") shouldBe 0
    player.count("DeckSpent") shouldBe 4
  }
}
