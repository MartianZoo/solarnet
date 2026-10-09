package dev.martianzoo.engine

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import kotlin.test.Test

internal class DomainExceptionContractTest {
  private fun agent() = Engine.newGame(canonicalPremise()).testAgent(PLAYER1)

  @Test
  internal fun unhandledTransformsAreExpressionFailures() {
    val agent = agent()

    shouldThrow<ExpressionException> { agent.count("WAT[Plant]") }
    shouldThrow<ExpressionException> { agent.has("WAT[Plant]") }
    shouldThrow<ExpressionException> { agent.runOperation("WAT[Plant]") }
    shouldThrow<ExpressionException> { agent.runOperation("PROD[PROD[Plant]]") }
  }

  @Test
  internal fun preprocessingKindChangesAreProgrammerErrors() {
    shouldThrow<IllegalStateException> { agent().parse<Instruction>("2 OxygenStep!") }
  }

  @Test
  internal fun ownerLocalClassesAreParsedBeforeTheFrozenClassTableRejectsThem() {
    val agent = agent()

    shouldThrow<PetSyntaxException> {
      agent.runOperation("RequiredAction { -> 3 ProjectCard }")
    }
    shouldThrow<PetSyntaxException> { agent.runOperation("RequiredAction { -> }") }
  }

  @Test
  internal fun directChangesRejectAbstractAndNonChangeInstructionsWithDomainExceptions() {
    val agent = agent()

    val incomplete = shouldThrow<NotFullySpecifiedException> { agent.sneak("Plant OR Heat") }
    incomplete.message!!.startsWith("instruction is abstract:") shouldBe true
    shouldThrow<NotFullySpecifiedException> { agent.sneak("X Plant") }
    shouldThrow<ExpressionException> { agent.sneak("Plant: Heat") }
  }

  @Test
  internal fun abstractPetsInputReachesTaskExecutionAndCompletionBoundaries() {
    val agent = agent()
    agent.beginOperation("X Plant")

    val execution = shouldThrow<NotFullySpecifiedException> { agent.doTask("X Plant") }
    execution.message!!.startsWith("instruction is abstract:") shouldBe true

    val completion = shouldThrow<NotFullySpecifiedException> { agent.completeOperation() }
    completion.message!!.startsWith("pending abstract tasks:") shouldBe true
  }

  @Test
  internal fun taskFailuresUseTaskOrDeadEndExceptions() {
    val agent = agent()

    val selection: Exception = shouldThrow<TaskException> { agent.selectTask("Plant") }
    val deadEnd: Exception = shouldThrow<DeadEndException> { agent.addTasks("Die THEN Plant") }

    selection.shouldBeInstanceOf<GameplayException>()
    deadEnd.shouldBeInstanceOf<GameplayException>()
    selection.shouldNotBeInstanceOf<PetException>()
    deadEnd.shouldNotBeInstanceOf<PetException>()
  }

  @Test
  internal fun gameplayFailuresAreNegativeResultsButIncompleteRequestsAreNot() {
    val narrowingAgent = agent()
    val task = narrowingAgent.addTasks("Plant OR Heat").single()
    narrowingAgent.selectTask(task)

    shouldThrow<NarrowingException> { narrowingAgent.narrowTask("Steel") }
        .shouldBeInstanceOf<GameplayException>()
    shouldThrow<NotNowException> { agent().runOperation("-Plant") }
        .shouldBeInstanceOf<GameplayException>()
    val incomplete: Exception = shouldThrow<NotFullySpecifiedException> { agent().sneak("X Plant") }
    (incomplete is GameplayException) shouldBe false
    incomplete.shouldNotBeInstanceOf<PetException>()
  }
}
