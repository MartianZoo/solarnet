package dev.martianzoo.tfm.pets.ast

data class Script(val lines: List<ScriptLine>) {

  interface ScriptLine

  data class ScriptCommand(
      val command: Instruction,
      val ownedBy: TypeExpression? = null
  ) : ScriptLine

  data class ScriptRequirement(val req: Requirement) : ScriptLine
  data class ScriptCounter(val key: String, val type: TypeExpression): ScriptLine
  data class ScriptPragmaPlayer(val player: TypeExpression): ScriptLine // also mode
}
