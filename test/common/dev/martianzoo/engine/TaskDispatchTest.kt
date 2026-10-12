package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskDispatchTest {
  @Test
  internal fun `a concrete task instruction admits an abstract choice for its recipient`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    val blue = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    admin.beginOperation("TASK Player2 { Choice }")

    val choice = game.tasks.extract { it }.single()
    choice.assignee shouldBe PLAYER2
    choice.instruction.isAbstract(game.reader) shouldBe true
    game.tasks.selectedTask() shouldBe null
    blue.doTask("ChoiceA<Player2>")
    blue.count("ChoiceA<Player2>") shouldBe 1
  }

  @Test
  internal fun `dispatch cannot choose the recipient's abstract body`() {
    val game = game()
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val task = admin.addTasks("TASK Player2 { Choice }").single()

    shouldThrow<TaskException> { admin.selectTask("TASK Player2 { ChoiceA }") }
    admin.tasks.ids().toList() shouldBe listOf(task)
  }

  @Test
  internal fun `a dispatch can finish its operation while the recipients choice remains pending`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    val blue = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    admin.beginOperation("TASK Player2 { Choice<Player2> }")

    admin.tasks.isEmpty() shouldBe true
    blue.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player2>!")
  }

  @Test
  internal fun `a continuation after dispatch is available while the recipient has pending work`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    val blue = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    admin.beginOperation("TASK Player2 { Choice<Player2> } THEN Marker")

    admin.tasks.isEmpty() shouldBe true
    admin.count("Marker") shouldBe 1
    blue.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player2>!")
  }

  @Test
  internal fun `fanout dispatchers can give each player an independent choice`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    val first = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val second = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    admin.beginOperation("EACH Me@Player { TASK { Choice } }")
    admin.tasks.isEmpty() shouldBe true

    first.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player1>!")
    second.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player2>!")
  }

  @Test
  internal fun `a task recipient can choose how much of an optional removal to perform`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    val blue = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    blue.runOperation("10 Token<Player2>")

    admin.beginOperation("TASK Player2 { -10 Token<Player2>? }")

    blue.tasks.extract { it.instruction.toString() } shouldBe listOf("-10 Token<Player2>?")
    blue.doTask("-3 Token<Player2>")
    blue.count("Token<Player2>") shouldBe 7
  }

  @Test
  internal fun `an explicit EACH recipient supplies Me in the dispatched body`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    val first = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val second = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    admin.beginOperation("EACH @Player { TASK @Player { Choice } }")

    first.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player1>!")
    second.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player2>!")
  }

  @Test
  internal fun `submitted and owned rules supply the default recipient`() {
    val game = game()
    val first = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val second = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    first.addTasks("TASK { Choice }")
    first.doTask("TASK { Choice }")
    first.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player1>!")
    first.doTask("ChoiceA<Player1>")

    second.beginOperation("OwnedDispatch<Player1>")
    first.doTask("TASK Player1 { Choice }")
    first.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player1>!")
  }

  @Test
  internal fun `an unowned rule without lexical Me dispatches to Admin`() {
    val game = game()
    val first = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)

    first.beginOperation("UnownedDispatch")
    first.doTask("TASK Admin { Marker }")

    first.tasks.isEmpty() shouldBe true
    admin.tasks.isEmpty() shouldBe true
    admin.count("Marker") shouldBe 1
  }

  @Test
  internal fun `trigger BY marker can supply a fixed recipient`() {
    val game = game()
    val first = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    first.beginOperation("TriggerDispatch")
    first.doTask("TASK Player1 { Choice }")

    first.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player1>!")
  }

  @Test
  internal fun `an open recipient stays invalid when a predicate has one current match`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    admin.runOperation("StartToken<Player1>")

    val task = admin.addTasks("TASK Player(HAS StartToken) { Choice }").single()
    shouldThrow<ExpressionException> { admin.selectTask(task) }
    admin.tasks.ids().toList() shouldBe listOf(task)
  }

  @Test
  internal fun `EACH binds the forced match before dispatch`() {
    val game = game()
    val admin = game.testAgent(ADMIN)
    val first = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    admin.runOperation("StartToken<Player1>")

    admin.beginOperation("EACH @Player(HAS StartToken) { TASK @Player { Choice } }")

    first.tasks.extract { it.instruction.toString() } shouldBe listOf("Choice<Player1>!")
  }

  @Test
  internal fun `an automatic effect cannot dispatch`() {
    shouldThrow<PetSyntaxException> { parse<Effect>("This:: TASK Player1 { Marker }") }
  }

  private fun game() =
      Engine.newGame(
          testGamePremise(
              """
              ABSTRACT CLASS Choice : Owned<Player> {
                CLASS ChoiceA
                CLASS ChoiceB
              }
              CLASS Token : Owned<Player>
              CLASS Marker
              CLASS StartToken : Owned<Player>
              CLASS OwnedDispatch : Owned<Player> { This: TASK { Choice } }
              CLASS UnownedDispatch { This: TASK { Marker } }
              CLASS TriggerDispatch { This BY @Player: TASK @Player { Choice } }
              """,
              players = 2,
          )
      )
}
