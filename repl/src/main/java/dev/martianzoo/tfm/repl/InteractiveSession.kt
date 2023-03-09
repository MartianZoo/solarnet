package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset

/**
 * A convenient interface for functional tests; basically, [ReplSession] is just a more texty
 * version of this.
 */
class InteractiveSession {
  internal var game: Game? = null // TODO private?
  internal var gameNumber: Int = 0
  internal var defaultPlayer: ClassName? = null
  internal var effectsOn: Boolean = false

  fun newGame(setup: GameSetup) {
    game = Engine.newGame(setup)
    gameNumber++
    becomeNoOne()
  }

  fun becomePlayer(player: ClassName) {
    val p = game!!.resolve(player.expr)
    require(!p.abstract)
    require(p.isSubtypeOf(game!!.resolve(ANYONE.expr)))
    defaultPlayer = p.mclass.className
  }

  fun becomeNoOne() {
    defaultPlayer = null
  }

  fun count(metric: Metric) = game!!.count(fixTypes(metric))

  fun list(expression: Expression): Multiset<Expression> {
    val typeToList: MType = game!!.resolve(fixTypes(expression))
    val allComponents: Multiset<Component> = game!!.getComponents(typeToList)

    // BIGTODO decide more intelligently how to break it down
    val mclass = typeToList.mclass

    // ugh capital tile TODO
    val subs: Set<MClass> = mclass.directSubclasses.ifEmpty { setOf(mclass) }

    val result = HashMultiset<Expression>()
    subs.forEach { sub ->
      val matches = allComponents.filter { it.hasType(sub.baseType) }
      if (matches.any()) {
        val types = matches.elements.map { it.mtype }
        result.add(lub(types)!!.expression, matches.size)
      }
    }
    return result
  }

  fun has(requirement: Requirement) = game!!.evaluate(fixTypes(requirement))

  fun execute(instruction: Instruction): List<ChangeRecord> {
    val context = defaultPlayer ?: GAME
    return game!!.execute(
        fixTypes(instruction),
        withEffects = effectsOn,
        initialCause = Cause(null, context.expr, context))
  }

  fun rollBackToBefore(ordinal: Int) = game!!.rollBack(ordinal)

  // TODO somehow do this with Type not Expression?
  // TODO Let game take care of this itself?
  fun <P : PetNode> fixTypes(node: P): P {
    val xer = game!!.loader.transformer
    var result = node
    result = xer.spellOutClassNames(result)
    result = xer.insertDefaults(result)
    if (defaultPlayer != null) {
      result = result.replaceAll(OWNER.expr, defaultPlayer!!.expr) // TODO
    }
    result = xer.deprodify(result)
    return result
  }
}
