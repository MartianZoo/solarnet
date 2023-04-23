package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.pets.ast.Instruction

// TODO the purpose of this exception type is not well thought out
open class InteractiveException(message: String) : UserException(message) {
  companion object {
    fun badSneak(instruction: Instruction) =
        InteractiveException("can only sneak simple changes, not: `$instruction`")

    fun mustClearTasks() = InteractiveException("In blue mode, must clear your task queue first")
  }
}
