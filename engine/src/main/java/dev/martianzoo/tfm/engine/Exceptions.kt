package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.pets.ast.Instruction

/** Some exception types and factories. */
public object Exceptions {
  public class DependencyException(val dependencies: Collection<Type>) :
      UserException("Missing dependencies: ${dependencies.joinToString()}")

  public class ExistingDependentsException(val dependents: Collection<Component>) :
      UserException("Existing dependents: ${dependents.joinToString()}")

  public class LimitsException(message: String) : UserException(message)

  open class InteractiveException(message: String) : UserException(message) {
    companion object {
      fun badSneak(instruction: Instruction) =
          InteractiveException("can only sneak simple changes, not: `$instruction`")

      fun mustClearTasks() = InteractiveException("In blue mode, must clear your task queue first")
    }
  }
}
