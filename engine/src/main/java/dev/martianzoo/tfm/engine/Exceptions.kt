package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.UserException

public class Exceptions {

  public class DependencyException(message: String) : UserException(message) {
    companion object {
      fun gaining(dependencies: Collection<Component>) =
          DependencyException("Missing dependencies: ${dependencies.joinToString()}")

      fun removing(dependents: Collection<Component>) =
          DependencyException("Existing dependents: ${dependents.joinToString()}")
    }
  }

  public class InvalidExpressionException(message: String) : UserException(message)
}
