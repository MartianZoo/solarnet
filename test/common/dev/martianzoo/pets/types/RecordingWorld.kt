package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement

/** A world that records every requirement it is asked about, in order. */
internal class RecordingWorld(private val answer: Boolean = true) : TypeInfo {
  override val classTable: ClassTable
    get() = error("unused by resolved type judgments")

  val questions: MutableList<String> = mutableListOf()

  override fun isAbstract(e: Expression): Boolean = error("unused by the type system")

  override fun ensureNarrows(wide: Expression, narrow: Expression): Unit =
      error("unused by the type system")

  override fun ensureSelectionNarrows(wide: Expression, narrow: Expression): Unit = error("unused")

  override fun has(requirement: Requirement): Boolean {
    questions += "$requirement"
    return answer
  }
}
