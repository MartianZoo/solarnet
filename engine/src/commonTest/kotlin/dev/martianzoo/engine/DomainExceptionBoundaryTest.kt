package dev.martianzoo.engine

import dev.martianzoo.engine.Gameplay.Companion.parse
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

internal class DomainExceptionBoundaryTest {
  private fun gameplay() = Engine.newGame(canonicalPremise()).gameplay(PLAYER1).godMode()

  @Test
  internal fun unhandledTransformsAreExpressionFailures() {
    val gameplay = gameplay()

    shouldThrow<ExpressionException> { gameplay.count("WAT[Plant]") }
    shouldThrow<ExpressionException> { gameplay.has("WAT[Plant]") }
    shouldThrow<ExpressionException> { gameplay.manual("WAT[Plant]") }
    shouldThrow<PetSyntaxException> { gameplay.manual("PROD[PROD[Plant]]") }
  }

  @Test
  internal fun preprocessingKindChangesUseKindExceptions() {
    shouldThrow<KindException> { gameplay().parse<Instruction>("2 OxygenStep!") }
  }

  @Test
  internal fun ownerLocalClassesAreParsedBeforeTheFrozenClassTableRejectsThem() {
    val gameplay = gameplay()

    shouldThrow<NoNewClassDeclarationsException> {
      gameplay.manual("Mandate { -> 3 ProjectCard }")
    }
    shouldThrow<PetSyntaxException> { gameplay.manual("Mandate { -> }") }
  }

  @Test
  internal fun directChangesRejectAbstractAndNonChangeInstructionsWithDomainExceptions() {
    val gameplay = gameplay()

    shouldThrow<AbstractException> { gameplay.sneak("Plant OR Heat") }
    shouldThrow<AbstractException> { gameplay.sneak("X Plant") }
    shouldThrow<ExpressionException> { gameplay.sneak("Plant: Heat") }
  }

  @Test
  internal fun taskBoundaryFailuresUseTaskOrDeadEndExceptions() {
    val gameplay = gameplay()

    shouldThrow<TaskException> { gameplay.prepareTask("Plant") }
    shouldThrow<DeadEndException> { gameplay.addTasks("Die THEN Plant") }
  }
}
