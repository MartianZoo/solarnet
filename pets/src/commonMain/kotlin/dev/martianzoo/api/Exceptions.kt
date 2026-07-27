package dev.martianzoo.api

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Type

public object Exceptions {

  // FACTORIES

  internal fun classNotFound(className: ClassName) =
      ExpressionException(
          "No class with name or id `$className` in current game (check bundles, check spelling)",
      )

  internal fun badExpression(specExpression: Expression, deps: String) =
      ExpressionException("can't match `$specExpression` to any of: `$deps`")

  public fun abstractComponent(type: Type, change: Change? = null): AbstractException =
      AbstractException(
          buildString {
            append("${type.expression} is abstract")
            change?.let { append(" in: `$it`") }
          },
      )

  public fun abstractInstruction(instr: Instruction): AbstractException =
      AbstractException("instruction is abstract: $instr")

  public fun orWithoutChoice(orInstruction: Or): AbstractException =
      AbstractException("choice required in: `$orInstruction`")

  public fun requirementNotMet(reqt: Requirement, message: String? = null): RequirementException =
      RequirementException("requirement not met: `$reqt` / $message")

  internal fun refinementNotMet(reqt: Requirement) =
      NarrowingException("requirement not met: `$reqt`")

  // TOP-LEVEL EXCEPTIONS

  /** A problem with Pets... stuff. */
  public open class PetException internal constructor(message: String, cause: Throwable? = null) :
      Exception(message, cause)

  /** Something is not a valid narrowing of something else. */
  public class NarrowingException(message: String, cause: Throwable? = null) :
      Exception(message, cause)

  public open class RecoverableException(message: String) : Exception(message)

  public open class TaskException(message: String) : Exception(message)

  public open class DeadEndException(message: String, cause: Throwable? = null) :
      Exception(message, cause) {
    public constructor(cause: Throwable) : this(cause.message ?: "", cause)
  }

  /**
   * An attempt was made to execute an instruction that was not fully-specified. This should be
   * rectifiable by reifying the instruction.
   */
  public class AbstractException(message: String) : RecoverableException(message)

  /**
   * Someone tried to do something that can't work against *this* game state, but could potentially
   * work later as far as we know.
   */
  public open class NotNowException(message: String) : RecoverableException(message)

  // Subtypes (catchable)

  public class PetSyntaxException(message: String, cause: Throwable? = null) :
      PetException(message, cause)

  public class ExistingDependentsException(public val dependents: Collection<Type>) :
      NotNowException("Existing dependents: ${dependents.joinToString { "${it.expression}" }}")

  /** A string does not represent a valid expression. */
  public class ExpressionException(message: String, cause: Throwable? = null) :
      PetException(message, cause)

  /** Something needed a requirement to be met and it was not. */
  public class RequirementException internal constructor(message: String) : NotNowException(message)

  public class DependencyException(public val dependencies: Collection<Type>) :
      NotNowException(
          "Missing dependencies: ${dependencies.joinToString { "${it.expressionFull}" } }"
      )

  public class LimitsException(message: String) : NotNowException(message)
}
