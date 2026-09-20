package dev.martianzoo.pets.api

import dev.martianzoo.pets.types.Type

/** Domain exception types shared across Pets-based APIs. */
public object Exceptions {
  // Authored Pets failures

  /** A problem in authored Pets source or the definitions assembled from it. */
  public sealed class PetException(
      message: String,
      cause: Throwable? = null,
  ) : Exception(message, cause)

  /** A Pets tree violates the language's syntactic or structural rules. */
  public class PetSyntaxException(
      message: String,
      cause: Throwable? = null,
  ) : PetException(message, cause)

  /** Syntactically valid Pets cannot be interpreted under the active Catalog. */
  public class ExpressionException(
      message: String,
      cause: Throwable? = null,
  ) : PetException(message, cause)

  /** Authored definitions cannot form a valid Catalog. */
  public class InvalidPetDefinitionException(
      message: String,
      cause: Throwable? = null,
  ) : PetException(message, cause)

  // Gameplay rejections

  /** A concrete gameplay attempt was understood and correctly rejected by the game model. */
  public sealed class GameplayException(
      message: String,
      cause: Throwable? = null,
  ) : Exception(message, cause)

  /** A requested task operation is incompatible with the current task state. */
  public class TaskException(
      message: String,
      cause: Throwable? = null,
  ) : GameplayException(message, cause)

  /** A gameplay path cannot produce a legal outcome. */
  public class DeadEndException(
      message: String,
      cause: Throwable? = null,
  ) : GameplayException(message, cause) {
    public constructor(cause: Throwable) : this(cause.message.orEmpty(), cause)
  }

  /** A concrete gameplay attempt is unavailable in the current World. */
  public open class NotNowException(
      message: String,
      cause: Throwable? = null,
  ) : GameplayException(message, cause)

  /** A component cannot be removed while other components depend on it. */
  public class ExistingDependentsException(
      public val dependents: Collection<Type>,
  ) : NotNowException("existing dependents: ${dependents.joinToString { "`${it.expression}`" }}")

  /** A requirement for the attempted gameplay is not met. */
  public class RequirementException(message: String) : NotNowException(message)

  /** Components required by the attempted gameplay are missing. */
  public class DependencyException(
      public val dependencies: Collection<Type>,
  ) :
      NotNowException(
          "missing dependencies: ${dependencies.joinToString { "`${it.expressionFull}`" } }"
      )

  /** A quantity or capacity limit makes the attempted gameplay unavailable. */
  public class LimitsException(message: String) : NotNowException(message)

  /** A proposed value is not a valid narrowing of the original value. */
  public class NarrowingException(
      message: String,
      cause: Throwable? = null,
  ) : GameplayException(message, cause)

  // Other domain failures

  /** A structurally understandable game configuration cannot produce the requested premise. */
  public class InvalidGameConfigException(
      message: String,
      cause: Throwable? = null,
  ) : Exception(message, cause)

  /** The request does not specify a concrete component, instruction, or choice to evaluate. */
  public class NotFullySpecifiedException(message: String) : Exception(message)

  /** A custom Kotlin implementation failed while evaluating otherwise valid Pets input. */
  public class CustomCodeException(
      message: String,
      cause: Throwable? = null,
  ) : Exception(message, cause)
}
