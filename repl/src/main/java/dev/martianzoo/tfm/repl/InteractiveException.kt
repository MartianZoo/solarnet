package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.pets.ast.Instruction

open class InteractiveException(message: String) : UserException(message) {
  companion object {
    fun badSneak(instruction: Instruction) =
        InteractiveException("can only sneak simple changes, not: `$instruction`")

    fun mustClearTasks() = InteractiveException("In blue mode, must clear your task queue first")
  }
}
