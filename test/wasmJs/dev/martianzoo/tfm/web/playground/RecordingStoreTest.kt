package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.InstructionTree
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
