package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.FakeAuthority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.PetNodeVisitor
import dev.martianzoo.tfm.pets.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType
import dev.martianzoo.tfm.types.PetType.PetGenericType

class ReplSession(val output: (String) -> Unit) {
  private var game: Game? = null
  private var defaultPlayer: TypeExpression? = null

  fun newgame(args: String?): List<String> {
    val bundles = args ?: "BRM"
    game = Engine.newGame(Canon, 2, bundles.asSequence().map { "$it" }.toList())
    defaultPlayer = null
    return listOf("New game created with bundles: $bundles")
  }

  fun count(args: String): List<String> {
    val fixedType = setDefaultPlayer(parsePets<TypeExpression>(args))
    val count = game!!.count(fixedType)
    return listOf("$count $fixedType")
  }

  // private fun setDefaultPlayer(type: PetType): PetType {
  //   if (defaultPlayer != null) {
  //     val key = Dependency.Key(game!!.classTable[OWNED], 0)
  //     val owner: PetClass? = type.dependencies[key]?.type?.petClass
  //     if (owner?.name in setOf(PLAYER, ANYONE)) {
  //       return (type as PetGenericType).specialize(listOf(defaultPlayer!!))
  //     }
  //   }
  //   return type
  // }

  fun list(args: String?): List<String> {
    val g = game as Game

    // "list Heat" means my own unless I say "list Heat<Anyone>"
    val typeToList = g.resolve(setDefaultPlayer(parsePets(args ?: COMPONENT.asString)))

    val theStuff = g.getAll(typeToList)

    // figure out how to break it down
    val petClass = typeToList.petClass
    var subs = petClass.directSubclasses.sortedBy { it.name }
    if (subs.isEmpty()) subs = listOf(petClass)
    return subs.mapNotNull {sub ->
      val thatType: PetGenericType = sub.baseType
      val count = theStuff.entrySet()
          .filter { it.element.hasType(thatType) }
          .sumOf { it.count }
      if (count > 0) {
        "$count".padEnd(4) + thatType
      } else {
        null
      }
    }
  }

  fun has(args: String): List<String> {
    val fixed = setDefaultPlayer(parsePets<Requirement>(args))
    val result = game!!.isMet(args)
    return listOf("$result: $fixed")
  }

  fun map() = MapToText(game!!).map()

  fun history() = game!!.changeLog.map { it.toString() }

  fun exec(args: String): List<String> {
    val instr = setDefaultPlayer(parsePets<Instruction>(args))
    game!!.execute(instr)
    return listOf("Ok: $instr")
  }

  fun become(args: String): List<String> {
    val type: PetType = game!!.resolve(args)
    require(!type.abstract && type.isSubtypeOf(game!!.classTable["Player"].baseType))
    defaultPlayer = type.toTypeExpressionFull()
    return listOf("Hi, $defaultPlayer")
  }

  fun replCommand(command: String, args: String?) {
    when (command) {
      "newgame" -> newgame(args).forEach(output)
      "count" -> count(args!!).forEach(output)
      "list" -> list(args).forEach(output)
      "has" -> has(args!!).forEach(output)
      "map" -> map().forEach(output)
      "history" -> history().forEach(output)
      "exec" -> exec(args!!).forEach(output)
      "become" -> become(args!!).forEach(output)
      "help" -> println("Help will go here.")
      else -> {
        val script = Parsing.parseScript("$command $args")
        script.execute(game!!)
      }
    }
  }

  private fun <P : PetNode> setDefaultPlayer(node: P): P {
    if (defaultPlayer == null) {
      return node
    }
    println("DEBUG: original is $node")
    val g = game!!
    val owned = g.resolve(OWNED.type)
    val anyone = g.resolve(ANYONE.type)

    class Fixer() : PetNodeVisitor() {
      override fun <P : PetNode?> transform(node: P): P {
        if (node is GenericTypeExpression) {
          if (g.resolve(node).isSubtypeOf(owned)) {
            val hasPlayer = node.specs.any { g.resolve(it).isSubtypeOf(anyone) }
            if (!hasPlayer) {
              return node.specialize(listOf(defaultPlayer!!)) as P
            }
          }
          return node
        }
        return super.transform(node) as P
      }
    }
    val fixt = Fixer().transform(node)
    println("DEBUG: fixed is $fixt")
    return fixt
  }

  object NullGame : GameState {
    override fun applyChange(
        count: Int,
        gaining: GenericTypeExpression?,
        removing: GenericTypeExpression?,
        cause: Cause?,
    ) = TODO("Not yet implemented")

    override val setup = GameSetup(FakeAuthority(), 2, listOf("M", "B"))

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
