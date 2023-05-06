package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Requirement

/**
 * Exception type for user-facing problems that, you know, probably need to be communicated well.
 * The companion object has various factory functions for common sorts.
 */
public object UserException { // TODO rename Exceptions

  // FACTORIES

  fun classNotFound(className: ClassName) =
      ExpressionException(
          "No class with name or id `$className` in current game (check bundles, check spelling)",
      )

  fun badClassExpression(specs: List<Expression>) =
      ExpressionException("must contain a single class name: `Class<${specs.joinToString()}>`")

  fun badExpression(specExpression: Expression, deps: String) =
      ExpressionException("can't match `$specExpression` to any of: `$deps`")

  fun customInstructionNotFound(functionName: String) =
      PetException("Custom instruction `@$functionName` not found")

  fun abstractComponent(type: Type, change: Change? = null) =
      AbstractException(
          buildString {
            append("[${type.expressionFull}] is abstract")
            change?.let { append(" in: `$it`") }
          },
      )

  fun abstractInstruction(instr: Instruction) = AbstractException("instruction is abstract: $instr")

  fun orWithoutChoice(orInstruction: Or) = AbstractException("choice required in: `$orInstruction`")

  fun requirementNotMet(reqt: Requirement) = RequirementException("requirement not met: `$reqt`")

  fun refinementNotMet(reqt: Requirement) =
      InvalidReificationException("requirement not met: `$reqt`")

  fun badSneak(instruction: Instruction) =
      PetException("can only sneak simple changes, not: `$instruction`")

  fun mustClearTasks() = NotNowException("you have tasks")

  // TOP-LEVEL EXCEPTIONS

  /** A problem with Pets syntax. */
  public open class PetException internal constructor(message: String) : Exception(message)

  /**
   * An attempt was made to execute an instruction that was not fully-specified. This should be
   * rectifiable by reifying the instruction.
   */
  public class AbstractException(message: String) : Exception(message)

  /** Something tried to pretend it reified something else and we're not having it. */
  public class InvalidReificationException(message: String) : Exception(message)

  /**
   * Someone tried to do something that can't work against *this* game state, but could potentially
   * work later as far as we know.
   */
  public open class NotNowException(message: String) : Exception(message)

  // Subtypes (catchable)

  public class PetSyntaxException internal constructor(message: String) : PetException(message)

  public class ExistingDependentsException(val dependents: Collection<Type>) :
      NotNowException("Existing dependents: ${dependents.joinToString()}")

  // TODO should just be factories

  /** A string does not represent a valid expression. */
  public class ExpressionException internal constructor(message: String) : PetException(message)

  /** Something needed a requirement to be met and it was not. */
  public class RequirementException internal constructor(message: String) :
      NotNowException(message)

  public class DependencyException(val dependencies: Collection<Type>) :
      NotNowException("Missing dependencies: ${dependencies.joinToString()}")

  public class LimitsException(message: String) : NotNowException(message)
}
