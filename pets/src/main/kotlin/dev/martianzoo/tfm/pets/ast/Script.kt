package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.genericType

data class Script(val lines: List<ScriptLine>) : PetNode() {
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

  sealed class ScriptLine : PetNode() {
    abstract fun doIt(game: GameState): Any
    override val kind = "ScriptLine"
  }

  data class ScriptCommand(val command: Instruction, val ownedBy: TypeExpression? = null) :
      ScriptLine() {
    override fun doIt(game: GameState) {
      command.execute(game)
    }
  }

  data class ScriptRequirement(val req: Requirement) : ScriptLine() {
    override fun doIt(game: GameState) {
      if (!req.evaluate(game)) throw PetException("Requirement failed: $req")
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

  companion object : PetParser() {
    fun parser(): Parser<ScriptLine> {
      return parser {
        val command: Parser<ScriptCommand> =
            skip(_exec) and
            Instruction.parser() and
            optional(skip(_by) and genericType) map { (instr, by) ->
              ScriptCommand(instr, by)
            }

        val req: Parser<ScriptRequirement> =
            skip(_require) and Requirement.parser() map ::ScriptRequirement

        val counter: Parser<ScriptCounter> =
            skip(_count) and
            TypeParsers.typeExpression and
            skip(_arrow) and
            _lowerCamelRE map { (type, key) ->
              ScriptCounter(key.text, type)
            }

        val player: Parser<ScriptPragmaPlayer> =
            skip(_become) and genericType map ::ScriptPragmaPlayer

        skip(zeroOrMore(char('\n'))) and (command or req or counter or player)
      }
    }
  }
}
