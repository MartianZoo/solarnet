package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.UserException

public object Exceptions {
  public class DependencyException(val dependencies: Collection<Component>) :
      UserException("Missing dependencies: ${dependencies.joinToString()}")

  public class ExistingDependentsException(val dependents: Collection<Component>) :
      UserException("Existing dependents: ${dependents.joinToString()}")

  public class LimitsException(message: String) : UserException(message)
}
