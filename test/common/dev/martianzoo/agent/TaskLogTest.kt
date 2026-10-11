package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

internal class TaskLogTest {

  @Test
  internal fun decliningAFormKeepsTheSelectionThatIdentifiesItsTask() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("Coin?, Marker?")
        }
    val source = newAgents()
    source.taskLog.start()
    val player = source[PLAYER1]
    val coinTask = player.tasks.ids().first()
    player.selectTask(coinTask)
    player.fillInTask(coinTask).apply {
      narrow("Ok")
      commit()
    }

    val imported = newAgents()
    TaskLog.replay(source.taskLog.text(), imported)

    imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
    imported[PLAYER1].count("Coin") shouldBe 0
    imported[PLAYER1].doTasks("Marker")
    imported[PLAYER1].count("Marker") shouldBe 1
    imported.world.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun formEditsRecordOnlyTheInstructionCommittedAtTheEnd() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker\nCLASS Prize")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("Coin OR Marker OR Prize")
        }
    val source = newAgents()
    source.taskLog.start()
    val player = source[PLAYER1]
    player.fillInTask(player.tasks.ids().single()).apply {
      narrow("Coin OR Marker")
      narrow("Coin")
      source.taskLog.text() shouldBe ""
      commit()
    }
    source.taskLog.text().lineSequence().filter { it.isNotBlank() }.count() shouldBe 1

    val imported = newAgents()
    TaskLog.replay(source.taskLog.text(), imported)

    imported[PLAYER1].count("Coin") shouldBe 1
    imported.world.tasks.isEmpty() shouldBe true
    imported.world.events.entriesSinceSetup() shouldBe source.world.events.entriesSinceSetup()
  }

  @Test
  internal fun singleTaskExecutionPreservesImplicitExpansionAndContinuations() {
    val premise = testGamePremise("CLASS Rating : Atomized\nCLASS Coin\nCLASS Marker")
    for (instruction in
        listOf("2 Rating", "2 Coin THEN Marker", "MAX 0 Marker: 2 Rating", "Rating / 2")) {
      fun newAgents(): Agents =
          Agents(Engine.newGame(premise)).also {
            it[PLAYER1].autoExecPolicy = NONE
            it[PLAYER1].addTasks("($instruction) OR Marker")
          }
      val source = newAgents()
      source.taskLog.start()
      source[PLAYER1].doTask(instruction)

      val imported = newAgents()
      TaskLog.replay(source.taskLog.text(), imported)

      imported[PLAYER1].list("Component") shouldBe source[PLAYER1].list("Component")
      imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
      imported.world.events.entriesSinceSetup() shouldBe source.world.events.entriesSinceSetup()
    }
  }

  @Test
  internal fun laterFormCommitsReplaceInconsequentialPartialChoices() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker\nCLASS Prize")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("Coin OR Marker OR Prize")
        }
    for (useForm in listOf(true, false)) {
      val source = newAgents()
      source.taskLog.start()
      val player = source[PLAYER1]
      if (useForm) {
        player.fillInTask(player.tasks.ids().single()).apply {
          narrow("Coin OR Marker")
          commit()
        }
      } else {
        player.selectTask(player.tasks.ids().single())
        player.narrowTask("Coin OR Marker")
      }

      val imported = newAgents()
      TaskLog.replay(source.taskLog.text(), imported)

      imported[PLAYER1].count("Coin") shouldBe 0
      imported[PLAYER1].count("Marker") shouldBe 0
      imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
      // The imported choice remains usable, including its selection lock.
      imported[PLAYER1].doTask("Marker")
      imported[PLAYER1].count("Marker") shouldBe 1
      imported.world.tasks.isEmpty() shouldBe true

      // Retain the step that selected the task, including any choices committed with selection.
      val partial = source.world.timeline.checkpoint()
      if (useForm) {
        player.fillInTask(player.tasks.ids().single()).apply {
          narrow("Marker")
          commit()
        }
      } else {
        player.narrowTask("Marker")
      }
      val selection =
          if (useForm) "$PLAYER1: CHOOSE Coin! OR Marker!\n"
          else "$PLAYER1: CHOOSE Coin! OR Marker! OR Prize!\n"
      source.taskLog.text() shouldBe "${selection}$PLAYER1: CHOOSE Marker!\n"
      val completed = newAgents()
      TaskLog.replay(source.taskLog.text(), completed)
      completed[PLAYER1].list("Component") shouldBe player.list("Component")
      completed.world.tasks.isEmpty() shouldBe true

      // A rolled-back completion must not erase the still-pending choice from an export.
      source.world.timeline.rollBack(partial)
      val restored = newAgents()
      TaskLog.replay(source.taskLog.text(), restored)
      restored.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
    }
  }

  @Test
  internal fun executingAGroupAlsoExecutesItsChildrenOnImport() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker\nCLASS Prize")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("(Coin, Marker) OR Prize")
        }
    for (tryTask in listOf(true, false)) {
      val source = newAgents()
      source.taskLog.start()
      if (tryTask) source[PLAYER1].tryTask("Coin, Marker")
      else source[PLAYER1].doTask("Coin, Marker")
      source[PLAYER1].count("Coin") shouldBe 1
      source[PLAYER1].count("Marker") shouldBe 1

      val imported = newAgents()
      TaskLog.replay(source.taskLog.text(), imported)

      imported[PLAYER1].count("Coin") shouldBe 1
      imported[PLAYER1].count("Marker") shouldBe 1
      imported.world.tasks.isEmpty() shouldBe true
      imported.world.events.entriesSinceSetup() shouldBe source.world.events.entriesSinceSetup()
    }
  }

  @Test
  internal fun adminWaitsForTheNextRecordedPlayerTask() {
    val premise = testGamePremise("CLASS Coin\nCLASS Token\nCLASS Reward : System")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("Coin!, Token!")
          it[ADMIN].addTasks("Token: Reward!")
        }
    val source = newAgents()
    source.taskLog.start()
    source[PLAYER1].autoExecPolicy = EAGER
    source[ADMIN].count("Reward") shouldBe 1

    val imported = newAgents()
    TaskLog.replay(source.taskLog.text(), imported)

    imported[ADMIN].count("Reward") shouldBe 1
    imported.world.tasks.isEmpty() shouldBe true
    imported[PLAYER1].autoExecPolicy shouldBe NONE
    imported[ADMIN].autoExecPolicy shouldBe EAGER
    imported.world.events.entriesSinceSetup() shouldBe source.world.events.entriesSinceSetup()
  }

  @Test
  internal fun ambiguousSelectionFailsWithoutTryingAlternativeExecutions() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker\nCLASS Prize")))
    val player = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("Coin OR Marker, Coin OR Prize")
    val checkpoint = agents.world.timeline.checkpoint()

    val failure =
        shouldThrow<IllegalArgumentException> {
          TaskLog.replay("$PLAYER1: CHOOSE Coin", agents)
        }

    failure.cause.shouldBeInstanceOf<TaskException>().message.shouldContain("ambiguous")
    agents.world.timeline.checkpoint() shouldBe checkpoint
    player.count("Coin") shouldBe 0
  }

  @Test
  internal fun automaticPlayerWorkReplaysWithPlayerAutoexecOff() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("2 Coin!, Marker!")
        }
    val source = newAgents()
    source.taskLog.start()
    source[PLAYER1].autoExecPolicy = EAGER

    val imported = newAgents()
    TaskLog.replay(source.taskLog.text(), imported)

    imported[PLAYER1].list("Component") shouldBe source[PLAYER1].list("Component")
    imported.world.tasks.isEmpty() shouldBe true
    imported[PLAYER1].autoExecPolicy shouldBe NONE
  }

  @Test
  internal fun aFormCanChooseAGroupBeforeItsChildrenAreExecuted() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker\nCLASS Prize")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("(Coin, Marker) OR Prize")
        }
    val source = newAgents()
    source.taskLog.start()
    source[PLAYER1].fillInTask(source[PLAYER1].tasks.ids().single()).apply {
      narrow("Coin, Marker")
      commit()
    }
    source[PLAYER1].doTasks("Marker")

    val imported = newAgents()
    TaskLog.replay(source.taskLog.text(), imported)

    imported[PLAYER1].count("Coin") shouldBe 0
    imported[PLAYER1].count("Marker") shouldBe 1
    imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
    imported.world.events.entriesSinceSetup() shouldBe source.world.events.entriesSinceSetup()
  }

  @Test
  internal fun aFailedTryDoesNotRecordTheAutomaticWorkItAllowsAfterward() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].addTasks("Coin?, Marker!")
        }
    val source = newAgents()
    source.taskLog.start()
    source[PLAYER1].tryTask("Coin?")

    val imported = newAgents()
    imported[PLAYER1].autoExecPolicy = NONE
    TaskLog.replay(source.taskLog.text(), imported)

    imported[PLAYER1].count("Coin") shouldBe 0
    imported[PLAYER1].count("Marker") shouldBe 1
    imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
  }

  @Test
  internal fun replaysCombinedAmountsAndKeepsPendingWork() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("2 Coin, 3 Coin, Marker")
        }
    val source = newAgents()
    source.taskLog.start()
    source[PLAYER1].doTasks("5 Coin")

    val imported = newAgents()
    TaskLog.replay(source.taskLog.text(), imported)

    imported[PLAYER1].count("Coin") shouldBe 5
    imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
    imported.world.events.entriesSinceSetup() shouldBe source.world.events.entriesSinceSetup()
  }

  @Test
  internal fun omitsFailedBatchesAndRolledBackCommandsEvenWhenOrdinalsAreReused() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker")))
    val player = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("Coin OR Marker")
    agents.taskLog.start()

    shouldThrow<TaskException> { player.doTasks("Coin", "Marker") }
    agents.taskLog.text() shouldBe ""

    val before = agents.world.timeline.checkpoint()
    player.doTasks("Coin")
    agents.world.timeline.rollBack(before)
    player.doTasks("Marker")

    agents.taskLog.text() shouldBe "$PLAYER1: Marker\n"
    player.count("Coin") shouldBe 0
    player.count("Marker") shouldBe 1
  }
}
