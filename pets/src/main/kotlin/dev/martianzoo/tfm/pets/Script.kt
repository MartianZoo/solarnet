package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

data class Script(val lines: List<ScriptLine>) {
  fun execute(game: GameApi): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    for (line in lines) {
      when (line) {
        is ScriptCommand -> line.doIt(game)
        is ScriptRequirement -> line.doIt(game)
        is ScriptCounter -> map.putAll(listOf(line.doIt(game)))
        else -> TODO()
      }
    }
    return map
  }

  sealed interface ScriptLine {
    fun doIt(game: GameApi): Any
  }

  data class ScriptCommand(
      val command: Instruction,
      val ownedBy: TypeExpression? = null,
  ) : ScriptLine {
    override fun doIt(game: GameApi) {
      command.execute(game)
    }
  }

  data class ScriptRequirement(val req: Requirement) : ScriptLine {
    override fun doIt(game: GameApi) {
      if (!req.evaluate(game)) throw PetsAbortException()
    }
  }

  data class ScriptCounter(val key: String, val type: TypeExpression) : ScriptLine {
    override fun doIt(game: GameApi): Pair<String, Int> {
      return key to game.count(type)
    }
  }

  data class ScriptPragmaPlayer(val player: TypeExpression) : ScriptLine { // also mode
    override fun doIt(game: GameApi): Any {
      TODO("Not yet implemented")
    }
  }

  class PetsAbortException : RuntimeException()
}
