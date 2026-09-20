package dev.martianzoo.engine

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
  internal fun directRemovalWithDependentsIsUnavailableGameplay() {
    val agent =
        Engine.newGame(
                testGamePremise(
                    """
                    CLASS Token { HAS MAX 1 This }
                    CLASS Holder<Token>
                    """
                        .trimIndent()
                )
            )
            .testAgent(PLAYER1)
    agent.sneak("Token!, Holder!")

    shouldThrow<NotNowException> { agent.sneak("-Token!") }

    agent.count("Token") shouldBe 1
    agent.count("Holder") shouldBe 1
  }

  @Test
  internal fun taskFailuresUseTaskOrDeadEndExceptions() {
    val agent = agent()

    val selection: Exception = shouldThrow<TaskException> { agent.selectTask("Plant") }
    val deadEnd: Exception = shouldThrow<DeadEndException> { agent.addTasks("Die THEN Plant") }

    selection.shouldBeInstanceOf<GameplayException>()
    deadEnd.shouldBeInstanceOf<GameplayException>()
    isPetException(selection) shouldBe false
    isPetException(deadEnd) shouldBe false
  }

  @Test
  internal fun gameplayFailuresAreNegativeResultsButIncompleteRequestsAreNot() {
    val agent = agent()
    val task = agent.addTasks("Plant OR Heat").single()

    shouldThrow<NarrowingException> { agent.narrowTask(task, "Steel") }
        .shouldBeInstanceOf<GameplayException>()
    shouldThrow<NotNowException> { agent.runOperation("-Plant") }
        .shouldBeInstanceOf<GameplayException>()
    val incomplete: Exception = shouldThrow<NotFullySpecifiedException> { agent.sneak("X Plant") }
    (incomplete is GameplayException) shouldBe false
    isPetException(incomplete) shouldBe false
  }

  @Test
  internal fun invalidEnginePremiseInputIsAConfigurationFailure() {
    val premise =
        testGamePremise("ABSTRACT CLASS NeverInitial", players = 0)
            .copy(initialComponentTypes = setOf(cn("NeverInitial").expression))

    shouldThrow<InvalidGameConfigException> { Engine.newGame(premise) }
  }

  private fun isPetException(exception: Exception): Boolean = exception is PetException
}
