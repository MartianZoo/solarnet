package dev.martianzoo.tfm.web.playground

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.text.EnglishInstructionTextRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

internal class RecordingStoreTest {
  @Test
  fun newLiveGameSavesARecoverablePosition() = runTest {
    val store = RecordingStore()
    val config = GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")
    val seed = 8264
    assertTrue(store.play(config, Canon.gamePremise(config), seed), store.error)
    val position = store.eventOrdinal

    var saved: PlaySession.Recovery? = null
    for (attempt in 1..40) {
      saved = savedRecovery()?.takeIf { it.seed == seed }
      if (saved != null) break
      delay(25)
    }
    val recovery = assertNotNull(saved, store.error ?: "No live recovery was saved")
    assertEquals(
        position,
        GameFileJson.parse(recovery.gameFile).open(Canon).positions.last().ordinal,
    )
    assertTrue(store.recoveryAvailable)
  }

  @Test
  fun undoReturnsToTheCurrentSetupDiscardThenThePreviousStart() {
    val store = RecordingStore()
    val config = GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")
    assertTrue(store.play(config, Canon.gamePremise(config), seed = 42), store.error)
    store.setSafeAutoexec(false)
    val gameStart = store.eventOrdinal
    repeat(20) {
      if (store.prompt?.cards != null) return@repeat
      val next = assertNotNull(store.prompt).options.first { it.enabled }
      store.choose(next)
      assertNull(store.problem)
    }
    val discard = assertNotNull(assertNotNull(store.prompt).cards)
    val discardStart = store.eventOrdinal
    assertTrue(discardStart > gameStart)
    store.toggleCard(discard.available.first())
    assertTrue(store.undo(), store.error)
    assertEquals(discardStart, store.eventOrdinal)
    assertNull(store.prompt?.cards?.chosen?.firstOrNull())
    assertTrue(store.undo(), store.error)
    assertTrue(store.eventOrdinal < discardStart)
    assertNull(store.error)
  }

  @Test
  fun undoReturnsToTheResearchDiscardAfterPassingTheRound() {
    val store = RecordingStore()
    val config = GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")
    assertTrue(store.play(config, Canon.gamePremise(config), seed = 42), store.error)
    for (attempt in 1..80) {
      val prompt = assertNotNull(store.prompt)
      val phase = assertNotNull(store.specimen).global.phase
      if (phase == "Research" && prompt.cards?.drawing == false) break
      val cards = prompt.cards
      if (cards != null) {
        if (!cards.canConfirm) store.toggleCard(cards.available.first())
        store.confirmCards()
      } else {
        val pass =
            if (phase == "Action")
                PlayChoices(prompt, store::choose, store::chooseTaskArm).action("PassBaton")
            else null
        if (pass != null) {
          pass()
        } else store.choose(prompt.options.first { it.enabled })
      }
      assertNull(store.problem, "At $phase: $prompt")
    }
    val research = assertNotNull(store.prompt)
    assertEquals("Research", assertNotNull(store.specimen).global.phase)
    val cards = assertNotNull(research.cards)
    assertFalse(cards.drawing)
    val start = store.eventOrdinal
    store.toggleCard(cards.available.first())
    assertTrue(store.undo(), store.error)
    assertEquals(start, store.eventOrdinal)
    assertTrue(store.undo(), store.error)
    assertEquals("Action", assertNotNull(store.specimen).global.phase)
  }

  @Test
  fun undoSkipsIntermediateChoicesWithinTheSameAction() {
    val store = RecordingStore()
    val config =
        GameConfig("TharsisMap, SoloMode, CorporateEraExpansion, VenusNextExpansion", "Green")
    val premise = Canon.gamePremise(config)
    // Offer the world's terraforming choice during setup to isolate undo from a full generation.
    val green = premise.premiseClassDeclarations.single { it.className == cn("Green") }
    val withTerraforming =
        premise.copy(
            premiseClassDeclarations =
                premise.premiseClassDeclarations - green +
                    green.copy(
                        authoredEffects =
                            green.authoredEffects +
                                parse<Effect>(
                                    "SetupPhase: GlobalParameter(NOT OceanTile<LandArea>)! BY Admin"
                                )
                    )
        )
    assertTrue(store.play(config, withTerraforming, seed = 42), store.error)
    store.setSafeAutoexec(false)
    val beforeTask = assertNotNull(store.prompt)
    val beforeTaskOrdinal = store.eventOrdinal
    store.choose(beforeTask.options.single { "GlobalParameter" in it.label })
    val beforeOcean = assertNotNull(store.prompt)
    store.choose(beforeOcean.options.single { it.label == "OceanTile" })
    val ocean = assertNotNull(store.prompt)
    assertTrue(ocean.options.isNotEmpty(), ocean.toString())
    store.choose(ocean.options.first())
    assertNull(store.problem)

    assertTrue(store.undo(), store.error)
    assertTrue(store.eventOrdinal < beforeTaskOrdinal)
    assertTrue(store.prompt != beforeOcean && store.prompt != ocean)
    assertNull(store.error)
    assertFalse(store.historyMode)
  }

  @Test
  fun undoStillUsesActionStartsAfterTruncation() {
    val store = RecordingStore()
    val config = GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")
    assertTrue(store.play(config, Canon.gamePremise(config), seed = 42))
    store.choose(assertNotNull(store.prompt).options.first())
    store.truncate()
    assertTrue(store.undo(), store.error)
    assertFalse(store.historyMode)
    assertNull(store.error)
    assertNotNull(store.prompt)
  }

  @Test
  fun arrowsVisitDecisionsBeforeActionsAndCollapseHiddenDecisions() =
      runTest() {
        withContext(Dispatchers.Default) {
          val store = RecordingStore()
          store.discover()
          store.load("OtbGame20260818Test")
          assertNull(store.error)
          assertTrue(
              store.actionStops.any { it.generation == 1 && it.summary == "Green plays Pets" }
          )
          store.seekToEvent(store.generationStarts.first())
          // The first action is Green playing Pets. Navigation must arrive before that choice,
          // with Pets still unplayed and the action offer in the queue, rather than its payment.
          while (store.stepAction(1)) {
            delay(1)
            if (assertNotNull(store.specimen).global.phase == "Action") break
          }
          val first = assertNotNull(store.specimen)
          assertEquals("Green", first.global.playerOnTurn)
          assertTrue(first.tasks.any { "StandardAction" in it.instruction })
          assertFalse(first.tasks.any { "Accepting" in it.instruction })
          assertFalse(first.components.any { it.className == "Pets" })

          val visited = mutableListOf(store.eventOrdinal)
          var secondActions = 0
          repeat(20) {
            assertTrue(store.stepAction(1))
            delay(1)
            assertTrue(store.eventOrdinal in store.visibleStops.values)
            val view = assertNotNull(store.specimen)
            if (view.global.phase == "Action") {
              assertTrue(
                  view.tasks.any {
                    "StandardAction" in it.instruction || "DoRequiredActions" in it.instruction
                  },
                  "Unexpected turn queue at ${store.eventOrdinal}: ${view.tasks}",
              )
              assertFalse(view.tasks.any { "Accepting" in it.instruction })
              if (
                  view.tasks.any {
                    "StandardAction" in it.instruction && "PassBaton" in it.instruction
                  }
              ) {
                secondActions++
              }
            }
            visited += store.eventOrdinal
          }
          assertTrue(secondActions > 0)
          for (expected in visited.dropLast(1).asReversed()) {
            assertTrue(store.stepAction(-1))
            assertEquals(expected, store.eventOrdinal)
            delay(1)
          }

          for (imaginary in listOf(false, true)) {
            store.includeImaginary = imaginary
            store.seekToStart()
            val displayed = store.visibleStops.values.toList()
            assertTrue(displayed.size <= store.stopOrdinals.size)
            for (expected in displayed.drop(1).take(12)) {
              assertTrue(store.stepStop(1))
              assertEquals(expected, store.eventOrdinal)
              delay(1)
            }
            for (expected in displayed.take(12).asReversed()) {
              assertTrue(store.stepStop(-1))
              assertEquals(expected, store.eventOrdinal)
              delay(1)
            }
            assertFalse(store.stepStop(-1))
            store.seekToStop(displayed.last())
            assertFalse(store.stepStop(1))
            for (expected in displayed.dropLast(1).takeLast(12).asReversed()) {
              assertTrue(store.stepStop(-1))
              assertEquals(expected, store.eventOrdinal)
              delay(1)
            }
          }
        }
      }

  @Test
  fun rendersTaskEnglishThroughoutFourPlayerRecording() = runTest {
    withContext(Dispatchers.Default) {
      val store = RecordingStore()
      store.discover()
      store.load("ActiveVacuumCoreTest")
      assertNull(store.error)
      val english = EnglishInstructionTextRenderer(assertNotNull(store.classTable))
      for (ordinal in store.stopOrdinals) {
        store.seekToStop(ordinal)
        for (task in assertNotNull(store.specimen).tasks) {
          try {
            english.render(
                parse<InstructionTree>(task.fullInstruction),
                task.assignee.takeUnless { it == "Admin" }?.let(::cn),
            )
          } catch (failure: Exception) {
            throw AssertionError("Task at $ordinal: ${task.fullInstruction}", failure)
          }
        }
      }
    }
  }

  @Test
  fun opensThreePlayerTurmoilRecordingWithTaskEnglish() = runTest {
    withContext(Dispatchers.Default) {
      val store = RecordingStore()
      store.discover()
      assertNull(store.error)
      val preview = launch { store.load("OtbGame20260912Test") }
      // Resource loading is asynchronous in the browser; wait until the workspace is available.
      while (store.specimen == null && store.error == null) delay(50)
      assertTrue(store.loading)
      assertTrue(store.previewing)
      val start = store.eventOrdinal
      store.seekToStart()
      delay(1000)
      assertTrue(store.eventOrdinal > start)
      assertTrue(store.eventOrdinal < store.stopOrdinals.last())
      val during = store.eventOrdinal
      store.seekToStart()
      assertEquals(during, store.eventOrdinal)
      store.skipPreview()
      preview.join()
      assertFalse(store.loading)
      assertFalse(store.previewing)
      assertEquals(store.stopOrdinals.last(), store.eventOrdinal)
      assertTrue(assertNotNull(store.playbackMessage).contains("wins. Click \"Gen 1\""))
      store.seekToEvent(store.generationStarts.first())
      assertNull(store.playbackMessage)
      assertTrue(store.eventOrdinal < store.stopOrdinals.last())
      store.seekToStart()
      assertNull(store.error)
      assertEquals(3, assertNotNull(store.specimen).players.size)
      assertEquals("OtbGame20260912Test", store.gameName)
      val english = EnglishInstructionTextRenderer(assertNotNull(store.classTable))
      repeat(5) {
        for (task in assertNotNull(store.specimen).tasks) {
          english.render(
              parse<InstructionTree>(task.fullInstruction),
              task.assignee.takeUnless { it == "Admin" }?.let(::cn),
          )
        }
        store.stepStop(1)
        assertNull(store.error)
      }
    }
  }
}
