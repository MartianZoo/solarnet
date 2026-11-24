package dev.martianzoo.tfm.tests.archive

import dev.martianzoo.engine.recording
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.tfm.engine.GameArchiveExporter
import dev.martianzoo.tfm.engine.GameArchiveImporter
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.replays.OtbGame20261111Test
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Opt-in local prototype test; run :tfm-tests:archiveDraftTest. */
internal class ArchiveDraftTest {
  private fun importer(source: String) = GameArchiveImporter(source, ::canonicalCatalog)

  private data class Snapshot(val components: Map<String, Int>, val tasks: List<String>)

  @Test
  fun comparesEveryCompletedGeneration() {
    val importer = importer(File(requireNotNull(System.getProperty("archive.file"))).readText())
    val baseline =
        OtbGame20261111Test().apply {
          commonSetup()
          game20261111()
        }
    assertSameGame(baseline.game.recording(), importer)
  }

  private fun assertSameGame(source: GameRecording, importer: GameArchiveImporter) {
    val recording = source.open()
    val expected = linkedMapOf<Int, Snapshot>()
    recording.positions.indices.forEach { position ->
      recording.seek(position)
      val world = recording.world
      val generation = world.reader.count(world.reader.resolve(parse<Expression>("Generation")))
      if (
          generation > 1 &&
              generation !in expected &&
              world.reader.has(parse("ResearchPhase")) &&
              world.tasks
                  .extract { it }
                  .none { it.assignee == ADMIN && it.deferral == world.tasks.earliestDeferral }
      ) {
        expected[generation] = snapshot(world.reader, world.tasks)
      }
    }
    val final = snapshot(recording.world.reader, recording.world.tasks)
    assertTrue(final.tasks.isEmpty(), "Baseline leaves unfinished tasks")
    val checked = linkedSetOf<Int>()
    importer.run {
      val world = importer.agents.world
      val generation = world.reader.count(world.reader.resolve(parse<Expression>("Generation")))
      if (generation > 1 && generation !in checked && world.reader.has(parse("ResearchPhase"))) {
        assertEquals(
            expected.getValue(generation),
            snapshot(world.reader, world.tasks),
            "After generation ${generation - 1}",
        )
        checked += generation
        println("Archive matches after generation ${generation - 1}")
      }
    }
    val world = importer.agents.world
    assertTrue(world.reader.has(parse("End")), "Archive stopped before final scoring")
    assertEquals(final, snapshot(world.reader, world.tasks), "Final game")
    assertEquals((2..9).toSet(), checked)
    println("Archive matches final game, including all components and pending tasks.")
  }

  @Test
  fun exportsProgrammaticGameWithoutTaskLogAndReimportsIt() {
    val baseline =
        OtbGame20261111Test().apply {
          commonSetup()
          game20261111()
        }
    val recording = baseline.game.recording()
    val text = GameArchiveExporter(recording, baseline.config).export()
    File(requireNotNull(System.getProperty("archive.output")))
        .also { it.parentFile.mkdirs() }
        .writeText(text)
    val imported = importer(text)
    assertSameGame(recording, imported)
    fun changes(recording: GameRecording) =
        recording.events
            .filterIsInstance<GameEvent.ChangeEvent>()
            .map { it.change.toString() }
            .groupingBy { it }
            .eachCount()
    assertEquals(
        changes(recording),
        changes(imported.agents.world.recording()),
        "All inventory changes",
    )
    assertTrue("//" !in text && "AUTO" !in text)
    val action =
        Regex(
            "^(Green|Pink): (playCard|useAction[12]|useStandardProject|action|pass|doRequiredActions)(\\(.*\\))?( \\{)?$"
        )
    fun actions(source: String) =
        source
            .lineSequence()
            .map { it.substringBefore("//").trimEnd() }
            .filter { action.matches(it) }
            .toList()
    val fixture = importer(File(requireNotNull(System.getProperty("archive.file"))).readText())
    fixture.run()
    val canonicalFixture =
        GameArchiveExporter(fixture.agents.world.recording(), baseline.config).export()
    assertEquals(actions(canonicalFixture), actions(text), "Action order and choices")
    assertEquals(
        text,
        GameArchiveExporter(imported.agents.world.recording(), baseline.config).export(),
        "Stable export after import",
    )
  }

  @Test
  fun exportsACorrectionAddedAfterTheRecordedGame() {
    val baseline =
        OtbGame20261111Test().apply {
          commonSetup()
          game20261111()
        }
    val green = baseline.game.actors.filterIsInstance<Player>().first { it.toString() == "Green" }
    baseline.agents[green].exMachina("7 Plant, 2 Heat")
    baseline.agents[green].exMachina("-3 Plant")
    val source = GameArchiveExporter(baseline.game.recording(), baseline.config).export()
    val imported = importer(source)
    imported.run()
    assertEquals(
        snapshot(baseline.game.reader, baseline.game.tasks),
        snapshot(imported.agents.world.reader, imported.agents.world.tasks),
    )
    assertTrue(source.endsWith("Green: exMachina(7 Plant, 2 Heat)\n\nGreen: exMachina(-3 Plant)\n"))
  }

  @Test
  fun failedCorrectionRestoresItsWholeGroupAndPreservesEarlierCalls() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val imported =
        importer(
            source.substringBefore("Green: Setup") +
                "Green: exMachina(2 Plant)\nGreen: exMachina(3 Plant, -Heat)\n"
        )
    assertFailsWith<IllegalStateException> { imported.run() }
    val green = imported.agents[imported.agents.world.actors.filterIsInstance<Player>().first()]
    assertEquals(2, green.count("Plant"))
    assertEquals(0, green.count("Heat"))
  }

  @Test
  fun groupedRecipientCorrectionInsideAnActionRetainsItsActor() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val imported =
        importer(
            source.substringBefore("Green: playCard(ResearchOutpost)") +
                """
                Green: playCard(ResearchOutpost) {
                    pay(18 MC)
                    Pink: exMachina(3 MC, 2 Plant)
                    NormalCityTile<Vastitas_6_2>
                    2 ProjectCard
                }
                """
                    .trimIndent()
        )
    imported.run()
    val config = OtbGame20261111Test().apply { commonSetup() }.config
    val text = GameArchiveExporter(imported.agents.world.recording(), config).export()
    assertTrue("    Pink: exMachina(3 MC, 2 Plant)" in text, text)
    val roundTrip = importer(text).also { it.run() }
    assertEquals(
        snapshot(imported.agents.world.reader, imported.agents.world.tasks),
        snapshot(roundTrip.agents.world.reader, roundTrip.agents.world.tasks),
    )
    assertEquals(
        text,
        GameArchiveExporter(roundTrip.agents.world.recording(), config).export(),
    )
  }

  @Test
  fun nestedCardPlayLeavesPaymentAndEffectsInTheEnclosingBody() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val imported =
        importer(
            source.substringBefore("Green: playCard(OrbitalConstructionYard)") +
                """
                Green: playCard(ExcentricSponsor) {
                    playCard(SpaceElevator)
                    -25 Owed
                    pay(2 MC)
                    PROD[Titanium]
                    Titanium
                }
                """
                    .trimIndent()
        )
    imported.run()
    val green = imported.agents[imported.agents.world.actors.filterIsInstance<Player>().first()]
    assertEquals(1, green.count("ExcentricSponsor"))
    assertEquals(1, green.count("SpaceElevator"))
    assertEquals(18, green.count("MC"))
    assertEquals(1, green.count("Titanium"))
    assertTrue(green.has("MAX 0 ActionScope"))
  }

  @Test
  fun cardResourcesCanPayTheWholePriceWithoutAnExplicitZeroMCLine() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val prefix =
        source.substringBefore("Green: playCard(ResearchOutpost)") +
            """
            Green: exMachina(Psychrophiles, 5 Microbe<Psychrophiles>)
            Green: playCard(AdaptedLichen) {
                pay(5 Microbe<Psychrophiles>)
                PROD[Plant]
                Plant
            }
            """
                .trimIndent()
    val imported = importer(prefix)
    imported.run()
    val green = imported.agents[imported.agents.world.actors.filterIsInstance<Player>().first()]
    assertEquals(1, green.count("AdaptedLichen"))
    assertEquals(0, green.count("Microbe<Psychrophiles>"))
    assertEquals(23, green.count("MC"))
    assertEquals(1, green.count("Plant"))
    assertTrue(green.has("MAX 0 ActionScope"))

    val insufficient = importer(prefix.replace("pay(5 Microbe", "pay(4 Microbe"))
    assertFailsWith<IllegalStateException> { insufficient.run() }
    val unpaid =
        insufficient.agents[insufficient.agents.world.actors.filterIsInstance<Player>().first()]
    assertEquals(0, unpaid.count("AdaptedLichen"))
    assertEquals(5, unpaid.count("Microbe<Psychrophiles>"))
  }

  @Test
  fun floodingSelectionPreservesTheAttackWithoutExecutingEitherStage() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val throughSelection =
        source.substringBefore("Green: playCard(ResearchOutpost)") +
            """
            Pink: exMachina(NormalCityTile<Vastitas_5_2>)
            Green: playCard(Flooding) {
                pay(7 MC)
                selectTask(OceanTile<Vastitas_5_3>! THEN -3 MC<Pink>)
            """
                .trimIndent()
    val selected = importer(throughSelection + "\n}")
    val unfinished = assertFailsWith<IllegalStateException> { selected.run() }
    assertTrue("effects are unfinished" in unfinished.message.orEmpty(), unfinished.message)
    val greenActor =
        selected.agents.world.actors.filterIsInstance<Player>().first { it.toString() == "Green" }
    val pinkActor =
        selected.agents.world.actors.filterIsInstance<Player>().first { it.toString() == "Pink" }
    assertEquals(0, selected.agents[greenActor].count("OceanTile"))
    assertEquals(21, selected.agents[pinkActor].count("MC"))

    val completed =
        importer(
            throughSelection +
                "\n" +
                """
                    OceanTile<Vastitas_5_3>
                    -3 MC<Pink>
                    TerraformRating
                    Plant
                }
                """
                    .trimIndent()
        )
    completed.run()
    val world = completed.agents.world
    assertEquals(1, completed.agents[greenActor].count("OceanTile"))
    assertEquals(18, completed.agents[pinkActor].count("MC"))
    val config = OtbGame20261111Test().config
    val exported = GameArchiveExporter(world.recording(), config).export()
    assertTrue("selectTask(" in exported, exported)
    val reimported = importer(exported)
    reimported.run()
    assertEquals(
        snapshot(world.reader, world.tasks),
        snapshot(reimported.agents.world.reader, reimported.agents.world.tasks),
    )
    assertEquals(
        exported,
        GameArchiveExporter(reimported.agents.world.recording(), config).export(),
    )
  }

  @Test
  fun recipientPaymentHandlesAlternativesAndRollsBackAnIncompleteAllocation() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val beforePayment =
        source.substringBefore("Green: playCard(ResearchOutpost)") +
            """
            Pink: exMachina(NeptunianPowerConsultants, 5 Heat, 3 Steel)
            Green: playCard(Flooding) {
                pay(7 MC)
                OceanTile<Vastitas_5_3>
                Pink: UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>
            """
                .trimIndent() +
            "\n"
    val effects =
        """
            Pink: PROD[Energy]
            Pink: Hydroelectric<NeptunianPowerConsultants>
            TerraformRating
            Plant
        }
        """
            .trimIndent()
    for ((allocation, heat, cash) in
        listOf(Triple("3 Heat, 2 MC", 2, 19), Triple("5 Heat", 0, 21))) {
      val imported = importer(beforePayment + "Pink: pay($allocation)\n" + effects)
      imported.run()
      val pink =
          imported.agents[
                  imported.agents.world.actors.filterIsInstance<Player>().first {
                    it.toString() == "Pink"
                  }]
      assertEquals(heat, pink.count("Heat"))
      assertEquals(cash, pink.count("MC"))
      assertEquals(3, pink.count("Steel"))
      assertEquals(1, pink.count("Hydroelectric<NeptunianPowerConsultants>"))
      assertEquals(0, pink.count("Owed"))
    }
    val before = importer(beforePayment + "}")
    assertFailsWith<IllegalStateException> { before.run() }
    val insufficient = importer(beforePayment + "Pink: pay(3 Heat, 1 MC)\n" + effects)
    assertFailsWith<IllegalStateException> { insufficient.run() }
    assertEquals(
        snapshot(before.agents.world.reader, before.agents.world.tasks),
        snapshot(insufficient.agents.world.reader, insufficient.agents.world.tasks),
    )
  }

  @Test
  fun viaUsesTheNormalMatcherAndNeverFallsBackToAnotherSource() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val text =
        source.substringBefore("Green: playCard(ResearchOutpost)") +
            """
            Green: exMachina(ViralEnhancers)
            Green: playCard(Pets) {
                pay(10 MC)
                Animal<Pets> VIA Pets
                Animal<Pets> VIA ViralEnhancers
            }
            """
                .trimIndent()
    val imported = importer(text)
    imported.run()
    val world = imported.agents.world
    val greenActor = world.actors.filterIsInstance<Player>().first { it.toString() == "Green" }
    assertEquals(2, imported.agents[greenActor].count("Animal<Pets>"))
    val config = OtbGame20261111Test().config
    val exported = GameArchiveExporter(world.recording(), config).export()
    assertTrue("Animal<Pets> VIA Pets" in exported, exported)
    val roundTrip = importer(exported)
    roundTrip.run()
    assertEquals(
        snapshot(world.reader, world.tasks),
        snapshot(roundTrip.agents.world.reader, roundTrip.agents.world.tasks),
    )

    for (replacement in
        listOf("Animal<Pets>", "Animal<Pets> VIA ResearchOutpost", "Plant VIA Pets")) {
      val invalid = importer(text.replaceFirst("Animal<Pets> VIA Pets", replacement))
      assertFailsWith<IllegalStateException> { invalid.run() }
      assertEquals(0, invalid.agents[greenActor].count("Animal<Pets>"))
      assertEquals(0, invalid.agents[greenActor].count("Plant"))
    }
  }

  @Test
  fun preservesATrailingTurnDeclineAndInfersItBeforeTheNextPlayersAction() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val imported = importer(source.substringBefore("Pink: action(LobbyAction)"))
    imported.run()
    val world = imported.agents.world
    val green =
        imported.agents[world.actors.filterIsInstance<Player>().first { it.toString() == "Green" }]
    val pink =
        imported.agents[world.actors.filterIsInstance<Player>().first { it.toString() == "Pink" }]
    assertEquals(1, green.count("Baton"))
    green.doTask("Ok")
    assertEquals(1, pink.count("Baton"))

    val config = OtbGame20261111Test().config
    val atDecline = GameArchiveExporter(world.recording(), config).export()
    assertTrue(atDecline.endsWith("Green {\n    Ok\n}\n"), atDecline)
    val restored = importer(atDecline)
    restored.run()
    assertEquals(
        snapshot(world.reader, world.tasks),
        snapshot(restored.agents.world.reader, restored.agents.world.tasks),
    )

    pink.doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Kelvinists> FROM LobbyDelegate")
    val afterNextAction = GameArchiveExporter(world.recording(), config).export()
    assertTrue("Green {\n    Ok\n}" !in afterNextAction, afterNextAction)
    val continued = importer(afterNextAction)
    continued.run()
    assertEquals(
        snapshot(world.reader, world.tasks),
        snapshot(continued.agents.world.reader, continued.agents.world.tasks),
    )
  }

  @Test
  fun deferredPrioritiesSurviveExportAndStillBlockLaterWork() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    // This scenario adds pending end work, so victory must wait until those tasks finish.
    val withRules =
        source
            .replace(
                "Green: assert(=92 VictoryPoint, =1 Victory)",
                "Green: assert(=92 VictoryPoint, =0 Victory)",
            )
            .replaceFirst(
                "Green: Setup {",
                """
                SetupRules {
                  CLASS Green : Player {
                    End: LATER Green { ArchiveFirst<Green>, ArchiveRemaining<Green> }, FINALLY Green { ArchiveDone<Green> }
                  }
                  CLASS ArchiveFirst : Owned<Player> {
                    This: ArchiveCoin?
                  }
                  CLASS ArchiveCoin : Owned<Player>
                  CLASS ArchiveRemaining : Owned<Player>
                  CLASS ArchiveDone : Owned<Player>
                }
                Green: Setup {
                """
                    .trimIndent(),
            )
    val original = importer(withRules)
    original.run()
    val actor =
        original.agents.world.actors.filterIsInstance<Player>().first { it.toString() == "Green" }
    original.agents[actor].doTask("ArchiveFirst")
    val text =
        GameArchiveExporter(original.agents.world.recording(), OtbGame20261111Test().config)
            .export()
    val restored = importer(text)
    restored.run()
    val player = restored.agents[actor]
    assertEquals(
        snapshot(original.agents.world.reader, original.agents.world.tasks),
        snapshot(restored.agents.world.reader, restored.agents.world.tasks),
    )
    assertFailsWith<dev.martianzoo.pets.api.Exceptions.TaskException> {
      player.doTask("ArchiveRemaining")
    }
    player.doTask("Ok")
    assertFailsWith<dev.martianzoo.pets.api.Exceptions.TaskException> {
      player.doTask("ArchiveDone")
    }
    player.doTasks("ArchiveRemaining", "ArchiveDone")
    assertEquals(0, player.count("ArchiveCoin"))
    assertEquals(1, player.count("ArchiveRemaining"))
    assertEquals(1, player.count("ArchiveDone"))
    assertEquals(1, player.count("Victory"))
    assertTrue(restored.agents.world.tasks.isEmpty())
  }

  @Test
  fun missingEffectIsNotAutoexecuted() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val importer = importer(source.replaceFirst("    2 ProjectCard\n", ""))
    val failure = assertFailsWith<IllegalStateException> { importer.run() }
    assertTrue(failure.message!!.contains("effects are unfinished"), failure.message)
    val green = importer.agents[importer.agents.world.actors.filterIsInstance<Player>().first()]
    assertEquals(4, green.count("ProjectCard"))
  }

  @Test
  fun insufficientPaymentDoesNotSpendResources() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val importer = importer(source.replaceFirst("pay(18 MC)", "pay(17 MC)"))
    val failure = assertFailsWith<IllegalStateException> { importer.run() }
    assertTrue(failure.message!!.contains("pay(17 MC)"), failure.message)
    val green = importer.agents[importer.agents.world.actors.filterIsInstance<Player>().first()]
    assertEquals(23, green.count("MC"))
    assertEquals(0, green.count("ResearchOutpost"))
  }

  @Test
  fun zeroOfAnotherResourceDoesNotDeclineThePlantTask() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    assertTrue("-0 Plant<Anyone>" in source)
    val importer = importer(source.replaceFirst("-0 Plant<Anyone>", "-0 MC<Anyone>"))
    val failure = assertFailsWith<IllegalStateException> { importer.run() }
    assertTrue(failure.message!!.contains("-0 MC<Anyone>"), failure.message)
  }

  @Test
  fun rejectsUnexpectedArgumentsAndBodies() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val header = source.substringBefore("Green: Setup")
    val invalid =
        listOf(
            "pay(1 MC) VIA Pets",
            "selectTask",
            "selectTask(Plant) {\n}",
            "action(ConvertHeatAction, 1, extra)",
            "action()",
            "action(TradeAction, 3 Energy)",
            "action(LobbyAction, 0)",
            "action(PaidLobbyAction, -1)",
            "action(ClaimMilestoneAction, Builder)",
            "action(FundAwardAction, Scientist)",
            "pass(1)",
            "doRequiredActions(1)",
            "playCard(ResearchOutpost, Hand, extra)",
            "useAction1(AsteroidRights, extra)",
            "useAction2(AsteroidRights, extra)",
            "useStandardProject(AquiferProject, extra)",
            "pay(1 MC) {\n}",
            "pass {\n}",
            "Research",
            "Research {\n}",
            "Research(2) {\n    Ok\n}",
            "Setup",
            "Production",
            "WorldGovernment",
            "GlobalEvent",
            "exMachina",
            "exMachina()",
            "exMachina {\n    Plant\n}",
            "exMachina(Plant) {\n    Plant\n}",
            "Plant {\n}",
            "UnknownCommand {\n}",
            "playCard(ResearchOutpost,)",
            "playCard(,ResearchOutpost)",
            "playCard(ResearchOutpost, Class<Hand])",
        )
    for (entry in invalid) {
      val importer = importer(header + "Green: $entry\n")
      val failure = assertFailsWith<IllegalStateException>(entry) { importer.run() }
      assertTrue(failure.message!!.contains("Archive line"), failure.message)
      assertTrue(
          listOf(
                  "VIA qualifies a Pets task",
                  "Unexpected arguments",
                  "Action number must be",
                  "does not take a body",
                  "requires a nonempty body",
                  "Unknown block",
                  "Empty argument",
                  "Mismatched argument delimiter",
              )
              .any {
                it in failure.message!!
              },
          "$entry: ${failure.message}",
      )
      assertEquals(
          0,
          importer.agents[importer.agents.world.actors.filterIsInstance<Player>().first()].count(
              "MC"
          ),
      )
    }
  }

  @Test
  fun rejectsPaymentDetailsItCannotExecuteExactly() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    for (payment in
        listOf(
            "pay()",
            "pay(18? MC)",
            "pay(18* MC)",
            "pay(0 MC)",
            "pay(-18 MC)",
            "pay(18 MC, Ok)",
            "pay(18 MC,)",
            "pay(18 MC<Green>)",
        )) {
      val importer = importer(source.replaceFirst("pay(18 MC)", payment))
      val failure = assertFailsWith<IllegalStateException>(payment) { importer.run() }
      assertTrue(failure.message!!.contains(payment), failure.message)
      assertEquals(
          23,
          importer.agents[importer.agents.world.actors.filterIsInstance<Player>().first()].count(
              "MC"
          ),
      )
    }
  }

  @Test
  fun rejectsMalformedHeadersAndBlocks() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val invalid =
        listOf(
            "",
            source.replaceFirst("Config {", "Config"),
            source.replaceFirst("Green, Pink", "Green, Green"),
            source.replaceFirst("Green, Pink", "Green, Pink,"),
            source.replaceFirst("Green, Pink", "Green, Pink {\n}"),
            source.replaceFirst("ExploreFirstDirective", "ExploreFirstDirective {\n}"),
            source + "}\n",
            source + "Green: Research {\n",
        )
    for (text in invalid) {
      assertFailsWith<IllegalArgumentException> { importer(text) }
    }

    val nested =
        importer(
            source.substringBefore("Green: Setup") + "Green {\nPink {\n75 VictoryPoint\n}\n}\n"
        )
    val failure = assertFailsWith<IllegalStateException> { nested.run() }
    assertTrue(failure.message!!.contains("Player blocks must be at top level"), failure.message)
  }

  @Test
  fun scoringWaitsForEachPlayersExplicitTotal() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val importer = importer(source.substringBefore("Pink {\n    75 VictoryPoint"))
    importer.run()
    val green =
        importer.agents.world.actors.filterIsInstance<Player>().first { it.toString() == "Green" }
    val pink =
        importer.agents.world.actors.filterIsInstance<Player>().first { it.toString() == "Pink" }
    assertEquals(92, importer.agents[green].count("VictoryPoint"))
    assertEquals(0, importer.agents[pink].count("VictoryPoint"))
    assertEquals(0, importer.agents[green].count("Victory"))
    assertEquals(1, importer.agents[green].count("GameScope"))
  }

  @Test
  fun rejectsAnIncorrectScoreWithoutAwardingPoints() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val importer = importer(source.replace("92 VictoryPoint", "93 VictoryPoint"))
    val failure = assertFailsWith<IllegalStateException> { importer.run() }
    assertTrue(failure.message!!.contains("93 VictoryPoint"), failure.message)
    val green =
        importer.agents.world.actors.filterIsInstance<Player>().first { it.toString() == "Green" }
    assertEquals(0, importer.agents[green].count("VictoryPoint"))
  }

  @Test
  fun rejectsDuplicateEventsThroughTheRuntime() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    assertFailsWith<dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException> {
      importer(source.replaceFirst("HeatFirstPolicy", "ExploreFirstDirective"))
    }
  }

  @Test
  fun paymentOrderIsNotRewritten() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val before = importer(source.substringBefore("Green: playCard(VestaShipyard)"))
    before.run()
    val importer = importer(source.replaceFirst("pay(4 Titanium, 2 MC)", "pay(2 MC, 4 Titanium)"))
    val failure = assertFailsWith<IllegalStateException> { importer.run() }
    assertTrue(failure.message!!.contains("pay(2 MC, 4 Titanium)"), failure.message)
    val actor = importer.agents.world.actors.filterIsInstance<Player>().first()
    for (resource in listOf("MC", "Titanium")) {
      assertEquals(before.agents[actor].count(resource), importer.agents[actor].count(resource))
    }
    assertEquals(0, importer.agents[actor].count("VestaShipyard"))
  }

  @Test
  fun refusesToUseAnUnrecordedGlobalEvent() {
    val source = File(requireNotNull(System.getProperty("archive.file"))).readText()
    val importer = importer(source.replaceFirst("    MoralMovement\n", ""))
    val failure = assertFailsWith<IllegalStateException> { importer.run() }
    assertTrue(failure.message!!.contains("Global Event deck is exhausted"), failure.message)
  }

  private fun snapshot(reader: GameReader, tasks: TaskQueue): Snapshot =
      Snapshot(
          reader
              .getComponents(parse<Expression>("Component"))
              .entries
              .associate { (type, count) ->
                type.toString() to count
              }
              .toSortedMap(),
          tasks
              .extract {
                "${it.assignee}|${it.selected}|${it.instruction}|${it.then}|${it.deferral}"
              }
              .sorted(),
      )
}
