package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Requirement

/**
 * Exception type for user-facing problems that, you know, probably need to be communicated well.
 * The companion object has various factory functions for common sorts.
 */
public open class UserException(override val message: String) : RuntimeException(message) {
  companion object {
    fun classNotFound(className: ClassName) =
        ExpressionException(
            "No class with name or id `$className` in current game (check bundles, check spelling)")

    fun badClassExpression(specs: List<Expression>) =
        ExpressionException("must contain a single class name: `Class<${specs.joinToString()}>`")

    fun badExpression(specExpression: Expression, deps: String) =
        ExpressionException("can't match `$specExpression` to any of: `$deps`")

    fun customInstructionNotFound(functionName: String) =
        UserException("Custom instruction `@$functionName` not found")

    fun abstractComponent(type: Type, change: Change? = null) =
        AbstractInstructionException(
            buildString {
              append("[${type.expressionFull}] is abstract")
              change?.let { append(" in: `$it`") }
            })

    fun optionalAmount(change: Change) =
        AbstractInstructionException("amount is optional in: `$change`")

    fun orWithoutChoice(orInstruction: Or) =
        AbstractInstructionException("choice required in: `$orInstruction`")

    fun unresolvedX(change: Change) =
        AbstractInstructionException("value for X needed in: `$change`")

    fun abstractArguments(abstractArgs: Iterable<Type>, custom: Custom) =
        AbstractInstructionException(
            "abstract components ${abstractArgs.joinToString("")} in: `$custom`")

    fun requirementNotMet(reqt: Requirement) = RequirementException("requirement not met: `$reqt`")

    fun die(cause: Cause?) = DeadEndException("`Die` instruction was reached: $cause")
  }

  /** A problem with Pets syntax. */
  public class PetsSyntaxException internal constructor(message: String) : UserException(message)

  /**
   * An attempt was made to execute an instruction that was not fully-specified. This should be
   * rectifiable by reifying the instruction.
   */
  public class AbstractInstructionException internal constructor(message: String) :
      UserException(message)

  /**
   * An instruction has been reached that will be impossible to complete (rollback is the only
   * option).
   */
  public class DeadEndException internal constructor(message: String) : UserException(message)

  /** A string does not represent a valid expression. */
  public class ExpressionException internal constructor(message: String) : UserException(message)

  /** Something needed a requirement to be met and it was not. */
  public class RequirementException internal constructor(message: String) : UserException(message)

  // TODO: internal constructor
  /** Something tried to pretend it reified something else and we're not having it. */
  public class InvalidReificationException(message: String) : UserException(message)
}