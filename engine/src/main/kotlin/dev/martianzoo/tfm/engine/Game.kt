package dev.martianzoo.tfm.engine

import com.google.common.collect.HashMultiset
import com.google.common.collect.ImmutableMultiset
import com.google.common.collect.Multiset
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetClassTable
import dev.martianzoo.tfm.types.PetType
import dev.martianzoo.util.toSetStrict

class Game(
    override val setup: GameSetup,
    val components: ComponentGraph,
    val classTable: PetClassTable,
) : GameState {
  // val tasks = mutableListOf<Task>()

  // TODO maybe have `beginChangeLogging` instead of passing in a prebuilt multiset
  val changeLog by components::changeLog

  override fun resolve(type: TypeExpression): PetType = classTable.resolve(type)
  override fun resolve(typeText: String) = resolve(TypeExpression.from(typeText))

  fun count(type: PetType) = components.count(type)

  fun execute(instr: String) = execute(Instruction.from(instr))
  fun execute(instr: Instruction) = instr.execute(this)

  override fun count(type: TypeExpression) = count(resolve(type))
  override fun count(typeText: String) = count(resolve(typeText))

  fun getAll(type: PetType): Multiset<Component> = components.getAll(type)

  override fun getAll(type: TypeExpression): Multiset<TypeExpression> {
    val all: Multiset<Component> = getAll(resolve(type))
    val result: Multiset<TypeExpression> = HashMultiset.create()
    all.forEachEntry {
      component, count -> result.add(component.asTypeExpression, count)
    }
    return ImmutableMultiset.copyOf(result)
  }

  override fun getAll(type: GenericTypeExpression): Multiset<GenericTypeExpression> {
    val result = HashMultiset.create<GenericTypeExpression>()
    getAll(resolve(type)).entrySet()
        .forEach { result.add(it.element.asTypeExpression.asGeneric(), it.count) }
    return result
  }

  override fun getAll(type: ClassLiteral): Set<ClassLiteral> {
    return getAll(resolve(type))
        .map { it.asTypeExpression as ClassLiteral }
        .toSetStrict()
  }

  override fun getAll(typeText: String) = getAll(TypeExpression.from(typeText))

  override fun isMet(requirement: Requirement) = requirement.evaluate(this)
  fun isMet(requirementText: String) = isMet(Requirement.from(requirementText))

  override fun applyChange(
      count: Int,
      gaining: GenericTypeExpression?,
      removing: GenericTypeExpression?,
      cause: Cause?,
  ) {
    val g = gaining?.let { Component(classTable.resolve(it)) }
    val r = removing?.let { Component(classTable.resolve(it)) }
    components.applyChange(count, g, r, cause)
  }
}
