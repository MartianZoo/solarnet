package dev.martianzoo.tfm.engine

public class Exceptions {
  public class RequirementException(message: String) : RuntimeException(message)

  public class DependencyException(message: String) : RuntimeException(message) {
    companion object {
      fun gaining(dependencies: Collection<Component>) =
          DependencyException("Missing dependencies: ${dependencies.joinToString()}")

      fun removing(dependents: Collection<Component>) =
          DependencyException("Existing dependents: ${dependents.joinToString()}")
    }
  }

  public class InvalidExpressionException(message: String) : RuntimeException(message)
}
