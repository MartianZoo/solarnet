package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.pets.util.toStrings
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SimpleAddsRemovesTest {
  @Test
  internal fun listReturnsExactComponentTypesAndMultiplicities() {
    val game =
        Engine.newGame(
            testGamePremise("ABSTRACT CLASS Token { ABSTRACT CLASS Color { CLASS Red, Blue } }")
        )
    val p1 = game.agent(PLAYER1)

    p1.manual("Red, Red, Blue")

    p1.list("Token").toStrings().shouldContainExactlyInAnyOrder("Red", "Red", "Blue")
  }

  @Test
  internal fun loggedTypesUseCanonicalPrefixesWithoutBreakingSelfEffects() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token
                CLASS Card : Owned { HAS MAX 1 This }
                ABSTRACT CLASS Linked<Card<Owner>> : Owned<Owner>
                CLASS Holder : Linked {
                  HAS MAX 1 This
                  This:: Token
                }
                """
                    .trimIndent()
            )
        )
    val p1 = game.agent(PLAYER1)
    p1.runOperation("Card<Player1>")
    val checkpoint = game.timeline.checkpoint()

    p1.runOperation("Holder<Player1, Card<Player1>>")

    game.events.changesSince(checkpoint).first().change.gaining shouldBe
        parse<Expression>("Holder<Player1, Card<Player1>>")
    p1.count("Token") shouldBe 1
  }

  @Test
  internal fun manualDefersAnAbstractInitialInstructionForTheBodyToNarrow() {
    val game = Engine.newGame(canonicalPremise())
    val p2 = game.tfm(PLAYER2)

    p2.runOperation("StandardResource") { doTask("Plant") }

    p2.count("Plant<Player2>") shouldBe 1
  }

  @Test
  internal fun manualStillRejectsAnImpossibleConcreteInitialInstruction() {
    val p2 = Engine.newGame(canonicalPremise()).tfm(PLAYER2)

    shouldThrow<LimitsException> { p2.runOperation("-Plant") }
  }

  @Test
  internal fun manualPreservesTasksThatWereAlreadyPending() {
    val game = Engine.newGame(canonicalPremise())
    val p2 = game.tfm(PLAYER2)
    val pendingTask = p2.addTasks("StandardResource?").single()

    p2.runOperation("Heat")

    p2.count("Heat") shouldBe 1
    (pendingTask in game.tasks) shouldBe true
  }

  @Test
  internal fun manualRejectsASelectedTask() {
    val game = Engine.newGame(canonicalPremise())
    val p2 = game.tfm(PLAYER2)
    val pendingTask = p2.addTasks("StandardResource?").single()
    p2.selectTask(pendingTask)

    shouldThrow<TaskException> { p2.runOperation("Heat") }
  }

  @Test
  internal fun basicByApi() {
    val game = Engine.newGame(canonicalPremise())

    val checkpoint = game.timeline.checkpoint()

    val admin = game.agent(ADMIN)
    admin.count("Heat") shouldBe 0

    val p2 = game.tfm(PLAYER2)

    p2.runOperation("5 Heat<Player2>!")
    p2.runOperation("10 Heat<Player1>!")

    admin.count("Heat") shouldBe 15

    p2.runOperation("-4 Heat")
    admin.has("Heat<Player2>") shouldBe true
    admin.has("=1 Heat<Player2>") shouldBe true
    admin.has("MAX 1 Heat<Player2>") shouldBe true
    admin.has("2 Heat<Player2>") shouldBe false
    admin.count("StandardResource") shouldBe 11
    admin.count("StandardResource<Player1>") shouldBe 10

    p2.runOperation("3 Steel<Player1> FROM Heat<Player1>!")
    admin.count("StandardResource<Player1>") shouldBe 10
    admin.count("Steel") shouldBe 3

    p2.runOperation("2 Heat<Player2> FROM Heat<Player1>!")
    admin.has("=3 Heat<Player2>") shouldBe true
    admin.has("=5 Heat<Player1>") shouldBe true

    val changes = game.events.changesSince(checkpoint)
    changes
        .map { it.change }
        .shouldContainExactly(
            StateChange(5, gaining = parse<Expression>("Heat<Player2>")),
            StateChange(10, gaining = parse<Expression>("Heat<Player1>")),
            StateChange(4, removing = parse<Expression>("Heat<Player2>")),
            StateChange(
                3,
                gaining = parse<Expression>("Steel<Player1>"),
                removing = parse<Expression>("Heat<Player1>"),
            ),
            StateChange(
                2,
                gaining = parse<Expression>("Heat<Player2>"),
                removing = parse<Expression>("Heat<Player1>"),
            ),
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
