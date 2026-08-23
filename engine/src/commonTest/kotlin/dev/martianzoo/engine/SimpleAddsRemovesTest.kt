package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.canonicalPremise
import dev.martianzoo.types.te
import dev.martianzoo.util.toStrings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SimpleAddsRemovesTest {
  @Test
  internal fun manualDefersAnAbstractInitialInstructionForTheBodyToNarrow() {
    val game = Engine.newGame(canonicalPremise())
    val p2 = game.tfm(PLAYER2).godMode()

    p2.manual("StandardResource") { doTask("Plant") }

    p2.count("Plant<Player2>") shouldBe 1
  }

  @Test
  internal fun manualStillRejectsAnImpossibleConcreteInitialInstruction() {
    val p2 = Engine.newGame(canonicalPremise()).tfm(PLAYER2).godMode()

    shouldThrow<LimitsException> { p2.manual("-Plant") }
  }

  @Test
  internal fun manualPreservesTasksThatWereAlreadyPending() {
    val game = Engine.newGame(canonicalPremise())
    val p2 = game.tfm(PLAYER2).godMode()
    val pendingTask = p2.addTasks("StandardResource").single()

    p2.manual("Heat")

    p2.count("Heat") shouldBe 1
    (pendingTask in game.tasks) shouldBe true
  }

  @Test
  internal fun manualRejectsAPreparedTask() {
    val game = Engine.newGame(canonicalPremise())
    val p2 = game.tfm(PLAYER2).godMode()
    val pendingTask = p2.addTasks("StandardResource").single()
    p2.prepareTask(pendingTask)

    shouldThrow<TaskException> { p2.manual("Heat") }
  }

  @Test
  internal fun basicByApi() {
    val game = Engine.newGame(canonicalPremise())

    val checkpoint = game.timeline.checkpoint()

    val eng = game.gameplay(ENGINE)
    eng.count("Heat") shouldBe 0

    val p2 = game.tfm(PLAYER2).godMode()

    p2.manual("5 Heat<Player2>!")
    p2.manual("10 Heat<Player1>!")

    eng.count("Heat") shouldBe 15

    p2.manual("-4 Heat")
    eng.has("Heat<Player2>") shouldBe true
    eng.has("=1 Heat<Player2>") shouldBe true
    eng.has("MAX 1 Heat<Player2>") shouldBe true
    eng.has("2 Heat<Player2>") shouldBe false
    eng.count("StandardResource") shouldBe 11
    eng.count("StandardResource<Player1>") shouldBe 10

    p2.manual("3 Steel<Player1> FROM Heat<Player1>!")
    eng.count("StandardResource<Player1>") shouldBe 10
    eng.count("Steel") shouldBe 3

    p2.manual("2 Heat<Player2> FROM Heat<Player1>!")
    eng.has("=3 Heat<Player2>") shouldBe true
    eng.has("=5 Heat<Player1>") shouldBe true

    val changes = game.events.changesSince(checkpoint)
    changes
        .map { it.change }
        .shouldContainExactly(
            StateChange(5, gaining = te("Heat<Player2>")),
            StateChange(10, gaining = te("Heat<Player1>")),
            StateChange(4, removing = te("Heat<Player2>")),
            StateChange(3, gaining = te("Steel<Player1>"), removing = te("Heat<Player1>")),
            StateChange(2, gaining = te("Heat<Player2>"), removing = te("Heat<Player1>")),
        )

    strip(changes.toStrings().map { it.replace(Regex("^\\d+"), "") })
        .shouldContainExactly(
            ": +5 Heat<Player2> BY Player2 (manual)",
            ": +10 Heat<Player1> BY Player2 (manual)",
            ": -4 Heat<Player2> BY Player2 (manual)",
            ": +3 Steel<Player1> FROM Heat<Player1> BY Player2 (manual)",
            ": +2 Heat<Player2> FROM Heat<Player1> BY Player2 (manual)",
        )
  }

  private fun strip(strings: Iterable<String>): List<String> {
    return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
  }

  private val startRegex = Regex("^[^:]+: ")
  private val endRegex = Regex(" BECAUSE.*")
}
