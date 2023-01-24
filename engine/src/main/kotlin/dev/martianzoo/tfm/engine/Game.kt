package dev.martianzoo.tfm.engine

import com.google.common.collect.HashMultiset
import com.google.common.collect.ImmutableMultiset
import com.google.common.collect.Multiset
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PClassTable
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.toSetStrict

public class Game(
    val setup: GameSetup,
    val components: ComponentGraph,
    val classTable: PClassTable,
) {
  // val tasks = mutableListOf<Task>()

  val authority by setup::authority

  // TODO maybe have `beginChangeLogging` instead of passing in a prebuilt multiset
  fun changeLog(): List<StateChange> = components.changeLog()

  fun resolve(type: TypeExpression): PType = classTable.resolve(type)

  fun isMet(requirement: Requirement) = LiveNodes.from(requirement, this).isMet(this)

  fun count(type: PType) = components.count(type)

  fun count(type: TypeExpression) = count(resolve(type))

  fun getAll(type: PType): Multiset<Component> = components.getAll(type)

  fun getAll(type: TypeExpression): Multiset<TypeExpression> {
    val all: Multiset<Component> = getAll(resolve(type))
    val result: Multiset<TypeExpression> = HashMultiset.create()
    all.forEachEntry { component, count -> result.add(component.asTypeExpression, count) }
    return ImmutableMultiset.copyOf(result)
  }

  fun getAll(type: GenericTypeExpression): Multiset<GenericTypeExpression> {
    val result = HashMultiset.create<GenericTypeExpression>()
    getAll(resolve(type)).entrySet().forEach {
      result.add(it.element.asTypeExpression.asGeneric(), it.count)
    }
    return result
  }

  fun getAll(type: ClassLiteral): Set<ClassLiteral> {
    return getAll(resolve(type)).map { it.asTypeExpression as ClassLiteral }.toSetStrict()
  }

  fun execute(instr: Instruction) = LiveNodes.from(instr, this).execute(this)

  fun applyChange(
      count: Int,
      removing: Component? = null,
      gaining: Component? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  ) {
    components.applyChange(
        count = count, removing = removing, gaining = gaining, amap = amap, cause = cause)
  }

  val asGameState: GameState by lazy {
    object : GameState {
      override fun applyChange(
          count: Int,
          removing: GenericTypeExpression?,
          gaining: GenericTypeExpression?,
          cause: Cause?,
          amap: Boolean,
      ) {
        // TODO order
        return this@Game.applyChange(
            count = count,
            removing = removing?.let { Component(resolve(it)) },
            gaining = gaining?.let { Component(resolve(it)) },
            amap = amap,
            cause = cause)
      }

      override val setup = this@Game.setup
      override val authority = this@Game.authority
      override val map = this@Game.setup.map

      override fun resolve(type: TypeExpression) = this@Game.resolve(type)

      override fun isMet(requirement: Requirement) = this@Game.isMet(requirement)

      override fun count(type: TypeExpression) = this@Game.count(type)

      override fun getAll(type: TypeExpression) = this@Game.getAll(type)
      override fun getAll(type: GenericTypeExpression) = this@Game.getAll(type)
      override fun getAll(type: ClassLiteral) = this@Game.getAll(type)
    }
  }
}
