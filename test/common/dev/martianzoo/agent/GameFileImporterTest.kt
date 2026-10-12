package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.recording
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.Player
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.state.actors
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

internal class GameFileImporterTest {
  @Test
  internal fun fullReplayChecksChangesAndRetainsCommentaryAndCorrection() = runTest {
    val premise = testGamePremise("CLASS Coin")
    val source = Agents(Engine.newGame(premise))
    source[PLAYER1].autoExecPolicy = NONE
    source.taskLog.start()
    source.taskLog.workflowStarted("test")
    source[PLAYER1].addTasks("Coin")
    source[PLAYER1].doTask("Coin")
    source.exMachina(PLAYER1, "Coin")
    source.world.events.changesSinceSetup().last().notes = "manual correction from paper log"
    val inputs = source.taskLog.inputs()
    inputs.last() shouldBe RecordedInput.Command(PLAYER1, RecordedInput.Kind.EX_MACHINA, "Coin")
    val document = GameFileJson.parse(GameFileJson.encode(source.world.recording(), inputs))

    val imported =
        GameFileImporter.replay(
            document,
            premise.catalog,
            newAgents = { Agents(Engine.newGame(it)) },
            startWorkflow = { agents, name ->
              name shouldBe "test"
              agents[PLAYER1].addTasks("Coin")
            },
        )

    imported[PLAYER1].count("Coin") shouldBe 2
    imported.world.events.changesSinceSetup().last().notes shouldBe
        "manual correction from paper log"
    imported[PLAYER1].autoExecPolicy shouldBe NONE
    imported[ADMIN].autoExecPolicy shouldBe NONE
  }

  @Test
  internal fun shortReplaySuppliesCardIdentitiesBeforeTheCommand() = runTest {
    val premise = testGamePremise("CLASS Coin")
    val source = Agents(Engine.newGame(premise))
    source[PLAYER1].autoExecPolicy = NONE
    source[PLAYER1].addTasks("Coin")
    source[PLAYER1].doTask("Coin")
    val player = premise.actors.filterIsInstance<Player>().single()
    val cardInput =
        RecordedInput.CardInput(
            player,
            "ProjectCard",
            RecordedInput.CardMovement.HAND_ARRIVAL,
            listOf("AcquiredCompany"),
        )
    val document =
        GameFileJson.parse(
            GameFileJson.encode(
                source.world.recording(),
                listOf(
                    RecordedInput.StartWorkflow("test"),
                    RecordedInput.Command(
                        player,
                        RecordedInput.Kind.DO_TASK,
                        "Coin",
                        listOf(cardInput),
                    ),
                ),
                short = true,
            )
        )
    var supplied = false

    val imported =
        GameFileImporter.replay(
            document,
            premise.catalog,
            newAgents = { Agents(Engine.newGame(it)) },
            startWorkflow = { agents, _ -> agents[PLAYER1].addTasks("Coin") },
            supplyCardInputs = { agents, inputs ->
              agents[PLAYER1].count("Coin") shouldBe 0
              inputs shouldBe listOf(cardInput)
              supplied = true
            },
        )
    supplied shouldBe true
    imported[PLAYER1].count("Coin") shouldBe 1
    shouldThrow<IllegalArgumentException> {
      GameFileImporter.replay(
          document,
          premise.catalog,
          newAgents = { Agents(Engine.newGame(it)) },
          startWorkflow = { agents, _ -> agents[PLAYER1].addTasks("Coin") },
      )
    }
  }
}
