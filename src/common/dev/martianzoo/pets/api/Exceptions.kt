package dev.martianzoo.pets.api

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Type

public object Exceptions {

  // FACTORIES

  internal fun classNotFound(className: ClassName) =
      ExpressionException(
          "No class named `$className` in current game (check bundles, check spelling)",
      )

  internal fun badExpression(specExpression: Expression, deps: String) =
      ExpressionException("can't match `$specExpression` to any of: `$deps`")

  public fun abstractComponent(
      type: Type,
      change: Change? = null,
  ): NotFullySpecifiedException =
      NotFullySpecifiedException(
          buildString {
            append("${type.expression} is abstract")
            change?.let { append(" in: `$it`") }
          },
      )

  public fun abstractInstruction(instr: InstructionTree): NotFullySpecifiedException =
      NotFullySpecifiedException("instruction is abstract: $instr")

  public fun orWithoutChoice(orInstruction: Or): NotFullySpecifiedException =
      NotFullySpecifiedException("choice required in: `$orInstruction`")

  public fun requirementNotMet(reqt: Requirement, message: String? = null): RequirementException =
      RequirementException("requirement not met: `$reqt` / $message")

  public fun requirementsNotMetInChoices(
      failures: Collection<RequirementException>
  ): RequirementException {
    require(failures.isNotEmpty())
    return RequirementException(
        "requirements not met in every choice: " + failures.joinToString { it.message.orEmpty() }
    )
  }

  internal fun refinementNotMet(reqt: Requirement) =
      NarrowingException("requirement not met: `$reqt`")

  public fun invalidPetDefinition(
      message: String,
      cause: Throwable? = null,
  ): InvalidPetDefinitionException = InvalidPetDefinitionException(message, cause)

  // TOP-LEVEL EXCEPTIONS

  /** A problem in authored Pets or the definitions assembled from it. */
  public sealed class PetException(
      message: String,
      cause: Throwable? = null,
  ) : Exception(message, cause)

  /** A concrete gameplay attempt was understood and correctly rejected by the game model. */
  public open class GameplayException(
      message: String,
      cause: Throwable? = null,
  ) : Exception(message, cause)

  /** A custom Kotlin implementation failed while evaluating otherwise valid Pets input. */
  public class CustomCodeException(message: String, cause: Throwable? = null) :
      Exception(message, cause)

  public open class TaskException(message: String, cause: Throwable? = null) :
      GameplayException(message, cause)

  public open class DeadEndException(message: String, cause: Throwable? = null) :
      GameplayException(message, cause) {
    public constructor(cause: Throwable) : this(cause.message ?: "", cause)
  }

  /** A concrete gameplay attempt is unavailable in the current World. */
  public open class NotNowException(
      message: String,
      cause: Throwable? = null,
  ) : GameplayException(message, cause)

  /** Something is not a valid narrowing of something else. */
  public class NarrowingException(message: String, cause: Throwable? = null) :
      GameplayException(message, cause)

  /** The request did not specify a concrete component, instruction, or choice to evaluate. */
  public class NotFullySpecifiedException(message: String) : Exception(message)

  // Subtypes (catchable)

  public class PetSyntaxException(message: String, cause: Throwable? = null) :
      PetException(message, cause)

  /** Syntactically valid Pets cannot be interpreted under the active Catalog. */
  public class ExpressionException(message: String, cause: Throwable? = null) :
      PetException(message, cause)

  /** Authored definitions cannot form a valid Catalog. */
  public class InvalidPetDefinitionException(message: String, cause: Throwable? = null) :
      PetException(message, cause)

  /** A structurally understandable game configuration cannot produce the requested premise. */
  public class InvalidGameConfigException(message: String, cause: Throwable? = null) :
      Exception(message, cause)

  public class ExistingDependentsException(public val dependents: Collection<Type>) :
      NotNowException("Existing dependents: ${dependents.joinToString { "${it.expression}" }}")

  /** Something needed a requirement to be met and it was not. */
  public class RequirementException internal constructor(message: String) : NotNowException(message)

  public class DependencyException(public val dependencies: Collection<Type>) :
      NotNowException(
          "Missing dependencies: ${dependencies.joinToString { "${it.expressionFull}" } }"
      )

  public class LimitsException(message: String) : NotNowException(message)
}
