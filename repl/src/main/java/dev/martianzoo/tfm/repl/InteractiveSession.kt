package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

/** A programmatic entry point to a REPL session that is less textual than [ReplSession]. */
class InteractiveSession {
  internal var game: Game? = null // TODO private?
  internal var gameNumber: Int = 0
  internal var defaultPlayer: ClassName? = null

  fun newGame(setup: GameSetup) {
    game = Engine.newGame(setup)
    gameNumber++
    defaultPlayer = null
  }

  fun becomePlayer(player: Int) {
    val p = cn("Player$player")
    game!!.resolveType(p.expr)
    defaultPlayer = p
  }

  fun becomeNoOne() {
    defaultPlayer = null
  }

  fun count(metric: Metric) = game!!.count(fixTypes(metric))

  fun list(expression: Expression): Multiset<Expression> {
    val typeToList: PType = game!!.resolveType(fixTypes(expression))
    val allComponents: Multiset<Component> = game!!.getComponents(typeToList)

    // BIGTODO decide more intelligently how to break it down
    val pclass = typeToList.pclass

    // ugh capital tile TODO
    val subs: Set<PClass> = pclass.directSubclasses.ifEmpty { setOf(pclass) }

    val result = HashMultiset<Expression>()
    subs.forEach { sub ->
      val matches = allComponents.filter { it.alwaysHasType(sub.baseType) }
      if (matches.any()) {
        val elements: Set<Component> = matches.elements
        var lub: PType? = null
        for (element in elements) {
          val ptype = element.type as PType
          lub = lub?.lub(ptype) ?: ptype
        }
        lub?.let { result.add(it.expression, matches.size) }
      }
    }
    return result
  }

  fun has(requirement: Requirement) = game!!.evaluate(fixTypes(requirement))

  fun execute(instruction: Instruction): Unit = game!!.execute(fixTypes(instruction))

  fun rollBackToBefore(ordinal: Int) = game!!.rollBack(ordinal)

  // TODO somehow do this with Type not Expression?
  // TODO Let game take care of this itself?
  fun <P : PetNode> fixTypes(node: P): P {
    val xer = game!!.loader.transformer
    var result = node
    result = xer.insertDefaults(result)
    if (defaultPlayer != null) {
      result = result.replaceAll(OWNER.expr, defaultPlayer!!.expr) // TODO
    }
    result = xer.deprodify(result)
    return result
  }
}
