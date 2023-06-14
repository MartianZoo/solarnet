package dev.martianzoo.repl

import dev.martianzoo.repl.ReplSession.UsageException

internal abstract class ReplCommand(val name: String) {
  open val isReadOnly: Boolean = false // not currently used
  abstract val usage: String
  abstract val help: String
  open fun noArgs(): List<String> = throw UsageException()
  open fun withArgs(args: String): List<String> = throw UsageException()
}
