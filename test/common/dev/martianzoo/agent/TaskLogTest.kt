package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

internal class TaskLogTest {
  private fun start(agents: Agents) {
    agents.taskLog.start()
    agents.taskLog.gameStarted()
  }

  private fun replay(source: Agents, imported: Agents) {
    TaskLog.replay(source.taskLog.inputs(), imported, startGame = {})
  }

  @Test
  internal fun decliningAFormKeepsTheSelectionThatIdentifiesItsTask() {
    val premise = testGamePremise("CLASS Coin\nCLASS Marker")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[PLAYER1].addTasks("Coin?, Marker?")
        }
    val source = newAgents()
    start(source)
    val player = source[PLAYER1]
    val coinTask = player.tasks.ids().first()
    player.selectTask(coinTask)
    player.fillInTask(coinTask).apply {
      narrow("Ok")
      commit()
    }

    val imported = newAgents()
    replay(source, imported)

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
    start(source)
    val player = source[PLAYER1]
    player.fillInTask(player.tasks.ids().single()).apply {
      narrow("Coin OR Marker")
      narrow("Coin")
      source.taskLog.inputs().size shouldBe 1
      commit()
    }
    source.taskLog.inputs().size shouldBe 2

    val imported = newAgents()
    replay(source, imported)

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
      start(source)
      source[PLAYER1].doTask(instruction)

      val imported = newAgents()
      replay(source, imported)

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
      start(source)
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
      replay(source, imported)

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
      val selection = if (useForm) "Coin! OR Marker!" else "Coin! OR Marker! OR Prize!"
      source.taskLog.inputs().drop(1).map {
        (it as RecordedInput.Command).copy(taskSelector = null)
      } shouldBe
          listOf(
              RecordedInput.Command(PLAYER1, RecordedInput.Kind.CHOOSE, selection),
              RecordedInput.Command(PLAYER1, RecordedInput.Kind.CHOOSE, "Marker!"),
          )
      val completed = newAgents()
      replay(source, completed)
      completed[PLAYER1].list("Component") shouldBe player.list("Component")
      completed.world.tasks.isEmpty() shouldBe true

      // Exporting an earlier completed position retains its still-pending choice.
      source.world.timeline.seek(partial)
      val restored = newAgents()
      replay(source, restored)
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
      start(source)
      if (tryTask) source[PLAYER1].tryTask("Coin, Marker")
      else source[PLAYER1].doTask("Coin, Marker")
      source[PLAYER1].count("Coin") shouldBe 1
      source[PLAYER1].count("Marker") shouldBe 1

      val imported = newAgents()
      replay(source, imported)

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
    start(source)
    source[PLAYER1].autoExecPolicy = EAGER
    source[ADMIN].count("Reward") shouldBe 1

    val imported = newAgents()
    replay(source, imported)

    imported[ADMIN].count("Reward") shouldBe 1
    imported.world.tasks.isEmpty() shouldBe true
    imported[PLAYER1].autoExecPolicy shouldBe NONE
    imported[ADMIN].autoExecPolicy shouldBe NONE
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
          TaskLog.replay(
              listOf(
                  RecordedInput.StartGame,
                  RecordedInput.Command(PLAYER1, RecordedInput.Kind.CHOOSE, "Coin"),
              ),
              agents,
              startGame = {},
          )
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
          it[PLAYER1].addTasks("2! Coin, Marker!")
        }
    val source = newAgents()
    start(source)
    source[PLAYER1].autoExecPolicy = EAGER

    val imported = newAgents()
    replay(source, imported)

    imported[PLAYER1].list("Component") shouldBe source[PLAYER1].list("Component")
    imported.world.tasks.isEmpty() shouldBe true
    imported[PLAYER1].autoExecPolicy shouldBe NONE
  }

  @Test
  internal fun disablingPoliciesBeforeStartDoesNotExecuteExistingTasks() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker")))
    agents[PLAYER1].addTasks("Coin")
    agents[ADMIN].addTasks("Marker")

    TaskLog.replay(listOf(RecordedInput.StartGame), agents, startGame = {})

    agents[PLAYER1].count("Coin") shouldBe 0
    agents[ADMIN].count("Marker") shouldBe 0
    agents.world.tasks.extract { it }.size shouldBe 2
  }

  @Test
  internal fun idOnlyTaskCommandCanHandWorkToAnotherActor() {
    val premise = testGamePremise("CLASS Coin")
    fun newAgents(): Agents =
        Agents(Engine.newGame(premise)).also {
          it[PLAYER1].autoExecPolicy = NONE
          it[ADMIN].autoExecPolicy = NONE
          it[PLAYER1].addTasks("Coin BY Admin")
        }
    val source = newAgents()
    start(source)
    source[PLAYER1].doTask(source[PLAYER1].tasks.ids().single())

    val imported = newAgents()
    replay(source, imported)

    source.taskLog.inputs().last().let { it as RecordedInput.Command }.kind shouldBe
        RecordedInput.Kind.DO_TASK_ID
    imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
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
    start(source)
    source[PLAYER1].fillInTask(source[PLAYER1].tasks.ids().single()).apply {
      narrow("Coin, Marker")
      commit()
    }
    source[PLAYER1].doTasks("Marker")

    val imported = newAgents()
    replay(source, imported)

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
    start(source)
    source[PLAYER1].tryTask("Coin?")

    val imported = newAgents()
    imported[PLAYER1].autoExecPolicy = NONE
    replay(source, imported)

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
    start(source)
    source[PLAYER1].doTasks("5 Coin")

    val imported = newAgents()
    replay(source, imported)

    imported[PLAYER1].count("Coin") shouldBe 5
    imported.world.tasks.extract { it } shouldBe source.world.tasks.extract { it }
    imported.world.events.entriesSinceSetup() shouldBe source.world.events.entriesSinceSetup()
  }

  @Test
  internal fun omitsFailedBatchesAndRolledBackCommandsEvenWhenOrdinalsAreReused() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker")))
    val player = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("Coin OR Marker")
    start(agents)

    shouldThrow<TaskException> { player.doTasks("Coin", "Marker") }
    agents.taskLog.inputs().size shouldBe 1

    val before = agents.world.timeline.checkpoint()
    player.doTasks("Coin")
    agents.world.timeline.seek(before)
    agents.world.timeline.truncate()
    player.doTasks("Marker")

    agents.taskLog.inputs().drop(1) shouldBe
        listOf(RecordedInput.Command(PLAYER1, RecordedInput.Kind.DO_TASKS, "Marker"))
    player.count("Coin") shouldBe 0
    player.count("Marker") shouldBe 1
  }
}
