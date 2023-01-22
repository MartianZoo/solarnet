package dev.martianzoo.tfm.repl

import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.PetNodeVisitor
import dev.martianzoo.tfm.pets.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.tfm.types.PetType.PetGenericType

class InteractiveSession {
  internal var game: Game? = null // TODO private?
  private var defaultPlayer: ClassName? = null

  fun newGame(setup: GameSetup) {
    game = Engine.newGame(setup)
    defaultPlayer = null
  }

  fun becomePlayer(player: Int) {
    defaultPlayer = cn("Player$player")
  }
  fun becomeNoOne() {
    defaultPlayer = null
  }

  fun count(type: TypeExpression) = game!!.count(fixTypes(type))

  fun list(type: TypeExpression): Multiset<TypeExpression> {
    // "list Heat" means my own unless I say "list Heat<Anyone>"
    val typeToList = game!!.resolve(fixTypes(type))
    val theStuff = game!!.getAll(typeToList)

    // figure out how to break it down
    val petClass = typeToList.petClass
    var subs = petClass.directSubclasses.sortedBy { it.name }
    if (subs.isEmpty()) subs = listOf(petClass)

    subs.mapNotNull { sub ->
      val thatType: PetGenericType = sub.baseType
      val count = theStuff.entrySet().filter { it.element.hasType(thatType) }.sumOf { it.count }
      if (count > 0) {
        "$count".padEnd(4) + thatType
      } else {
        null
      }
    }
    return LinkedHashMultiset.create()
  }

  fun has(requirement: Requirement) = game!!.isMet(fixTypes(requirement))

  fun execute(instruction: Instruction): Instruction {
    val instr = fixTypes(instruction)
    game!!.execute(instr)
    return instr
  }

  fun <P : PetNode> fixTypes(node: P): P {
    if (defaultPlayer == null) {
      return node
    }
    val g = game!!
    val owned = g.resolve(OWNED.type)
    val anyone = g.resolve(ANYONE.type)

    class Fixer : PetNodeVisitor() {
      override fun <P : PetNode?> transform(node: P): P {
        if (node is GenericTypeExpression) {
          if (g.resolve(node).isSubtypeOf(owned)) {
            val hasPlayer = node.args.any { g.resolve(it).isSubtypeOf(anyone) }
            if (!hasPlayer) {
              return node.addArgs(listOf(defaultPlayer!!.type)) as P
            }
          }
          return node
        }
        return super.transform(node) as P
      }
    }
    val fixt = Fixer().transform(node)

    // TODO hmm
    return deprodify(fixt, (game!!.classTable as PetClassLoader).resourceNames())
  }
}
