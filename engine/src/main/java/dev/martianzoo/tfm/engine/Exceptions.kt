package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.UserException

public class Exceptions {
  public class DependencyException(val dependencies: Collection<Component>) :
      UserException("Missing dependencies: ${dependencies.joinToString()}")

  public class ExistingDependentsException(val dependents: Collection<Component>) :
      UserException("Existing dependents: ${dependents.joinToString()}")

  public class InvalidExpressionException(message: String) : UserException(message)

  public class LimitsException(message: String) : UserException(message)
}
