package dev.martianzoo.tfm.engine

import com.google.common.collect.Multiset
import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.GameApi
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Script
import dev.martianzoo.tfm.pets.ast.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetClass
import dev.martianzoo.tfm.types.PetClassTable
import dev.martianzoo.tfm.types.PetType

internal class Game(
    override val authority: Authority,
    val components: ComponentGraph,
    val classTable: PetClassTable,
) : GameApi {
  // val tasks = mutableListOf<Task>()

  fun resolve(type: TypeExpression) = classTable.resolve(type)
  fun resolve(typeText: String) = resolve(parse(typeText))

  fun count(type: PetType): Int {
    return if (type is PetClass) {
      type.allSubclasses.count { !it.abstract }
    } else {
      components.count(type)
    }
  }

  fun execute(instr: String) = execute(parse<Instruction>(instr))
  fun execute(instr: Instruction) = instr.execute(this)

  fun execute(script: Script): Map<String, Int> = script.execute(this)

  override fun count(type: TypeExpression) = count(resolve(type))
  fun count(typeText: String) = count(resolve(typeText))

  fun getAll(type: PetType): Multiset<Component> = components.getAll(type)
  fun getAll(type: TypeExpression) = getAll(resolve(type))
  fun getAll(typeText: String) = getAll(resolve(typeText))

  override fun isMet(requirement: Requirement) = requirement.evaluate(this)
  fun isMet(requirementText: String) = isMet(parse(requirementText))

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
