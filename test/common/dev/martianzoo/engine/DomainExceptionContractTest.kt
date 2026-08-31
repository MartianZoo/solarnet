package dev.martianzoo.engine

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.KindException
import dev.martianzoo.pets.api.Exceptions.NoNewClassDeclarationsException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class DomainExceptionContractTest {
  private fun agent() = Engine.newGame(canonicalPremise()).agent(PLAYER1)

  @Test
  internal fun unhandledTransformsAreExpressionFailures() {
    val agent = agent()

    shouldThrow<ExpressionException> { agent.count("WAT[Plant]") }
    shouldThrow<ExpressionException> { agent.has("WAT[Plant]") }
    shouldThrow<ExpressionException> { agent.manual("WAT[Plant]") }
    shouldThrow<PetSyntaxException> { agent.manual("PROD[PROD[Plant]]") }
  }

  @Test
  internal fun preprocessingKindChangesUseKindExceptions() {
    shouldThrow<KindException> { agent().parse<Instruction>("2 OxygenStep!") }
  }

  @Test
  internal fun ownerLocalClassesAreParsedBeforeTheFrozenClassTableRejectsThem() {
    val agent = agent()

    shouldThrow<NoNewClassDeclarationsException> { agent.manual("Mandate { -> 3 ProjectCard }") }
    shouldThrow<PetSyntaxException> { agent.manual("Mandate { -> }") }
  }

  @Test
  internal fun directChangesRejectAbstractAndNonChangeInstructionsWithDomainExceptions() {
    val agent = agent()

    shouldThrow<AbstractException> { agent.sneak("Plant OR Heat") }
    shouldThrow<AbstractException> { agent.sneak("X Plant") }
    shouldThrow<ExpressionException> { agent.sneak("Plant: Heat") }
  }

  @Test
  internal fun taskFailuresUseTaskOrDeadEndExceptions() {
    val agent = agent()

    shouldThrow<TaskException> { agent.selectTask("Plant") }
    shouldThrow<DeadEndException> { agent.addTasks("Die THEN Plant") }
  }
}
