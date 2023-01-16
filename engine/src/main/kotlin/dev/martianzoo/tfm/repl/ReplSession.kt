package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType

class ReplSession(val output: (String) -> Unit) {
  private var game: GameState = NullGame

  fun replCommand(command: String, args: String?) {
    when (command) {
      "newgame" -> {
        val bundles = args ?: "BRM"
        game = Engine.newGame(Canon, 2, bundles.asSequence().map { "$it" }.toList())
        output("New game created with bundles: $bundles")
      }
      "list" -> {
        val realGame = game as Game
        val type: PetType = realGame.resolve(args ?: "Component")
        var subs = type.petClass.directSubclasses.sortedBy { it.name }
        if (subs.isEmpty()) subs = listOf(type.petClass)
        for (sub in subs) {
          val count = realGame.count(sub.baseType)
          if (count > 0) {
            output("$count".padEnd(4) + "${sub.name}")
          }
        }
      }
      "count" -> {
        println(game.count(args!!))
      }
      "map" -> MapToText(game).map().forEach(output)
      "help" -> println("Help will go here.")
      else -> {
        val script = Parsing.parseScript("$command $args")
        script.execute(game)
      }
    }
  }

  object NullGame : GameState {
    override fun applyChange(
        count: Int,
        gaining: GenericTypeExpression?,
        removing: GenericTypeExpression?,
        cause: Cause?,
    ) = TODO("Not yet implemented")

    override val setup = GameSetup(Canon, 2, listOf("M", "B"))

    override fun resolve(typeText: String) = throe()
    override fun resolve(type: TypeExpression) = throe()
    override fun count(typeText: String) = throe()
    override fun count(type: TypeExpression) = throe()
    override fun getAll(type: TypeExpression) = throe()
    override fun getAll(type: ClassLiteral) = throe()
    override fun getAll(typeText: String) = throe()
    override fun isMet(requirement: Requirement) = throe()

    private fun throe(): Nothing = throw RuntimeException("no game has been started")
  }
}
