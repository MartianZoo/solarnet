package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.api.GameState

data class Script(val lines: List<ScriptLine>) : PetsNode() {
  fun execute(game: GameState): Map<String, Int> {
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

  override val kind = "Script"

  sealed class ScriptLine: PetsNode() {
    abstract fun doIt(game: GameState): Any
    override val kind = "ScriptLine"
  }

  data class ScriptCommand(
      val command: Instruction,
      val ownedBy: TypeExpression? = null,
  ) : ScriptLine() {
    override fun doIt(game: GameState) {
      command.execute(game)
    }
  }

  data class ScriptRequirement(val req: Requirement) : ScriptLine() {
    override fun doIt(game: GameState) {
      if (!req.evaluate(game)) throw PetsAbortException("Requirement failed: $req")
    }
  }

  data class ScriptCounter(val key: String, val type: TypeExpression) : ScriptLine() {
    override fun doIt(game: GameState): Pair<String, Int> {
      return key to game.count(type)
    }
  }

  data class ScriptPragmaPlayer(val player: TypeExpression) : ScriptLine() { // also mode
    override fun doIt(game: GameState): Any {
      TODO("Not yet implemented")
    }
  }

  class PetsAbortException(message: String? = null) : RuntimeException(message)
}
