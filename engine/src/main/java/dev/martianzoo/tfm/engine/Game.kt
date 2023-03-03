package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.Multiset

/** A game in progress. */
public class Game(
    override val setup: GameSetup,
    internal val components: ComponentGraph,
    public val loader: PClassLoader, // TODO not public
) : GameState {
  // val tasks = mutableListOf<Task>()

  fun changeLog(): List<ChangeRecord> = components.changeLog()

  fun changeLogFull(): List<ChangeRecord> = components.changeLogFull()

  override fun resolveType(typeExpr: TypeExpr): PType = loader.resolveType(typeExpr)

  fun resolveType(type: Type): PType = loader.resolveType(type)

  override fun evaluate(requirement: Requirement) = LiveNodes.from(requirement, this).evaluate(this)

  override fun countComponents(type: Type): Int = components.count(resolveType(type))

  fun countComponents(typeExpr: TypeExpr): Int = countComponents(resolveType(typeExpr))

  override fun getComponents(type: Type): Multiset<Type> =
      components.getAll(resolveType(type)).map { it.type }

  fun getComponents(type: PType): Multiset<Component> = components.getAll(type)

  fun execute(instr: Instruction) = LiveNodes.from(instr, this).execute(this)

  override fun applyChange(
      count: Int,
      removing: Type?,
      gaining: Type?,
      amap: Boolean,
      cause: Cause?,
  ) {
    components.applyChange(
        count,
        removing = component(removing),
        gaining = component(gaining),
        amap = amap,
        cause = cause,
        hidden = false)
  }

  public fun component(type: Type?): Component? = type?.let { Component(loader.resolveType(it)) }

  public fun component(type: TypeExpr?): Component? =
      type?.let { Component(loader.resolveType(it)) }

  public fun rollBack(ordinal: Int) {
    val log = changeLogFull()
    val ct = log.size
    require(ordinal <= ct)
    if (ordinal == ct) return
    require(!log[ordinal].hidden)

    val subList = components.changeLog.subList(ordinal, ct)
    for (entry in subList.asReversed()) {
      val change = entry.change.inverse()
      components.updateMultiset(
          change.count,
          gaining = component(change.gaining),
          removing = component(change.removing),
      )
    }
    subList.clear()
  }
}
