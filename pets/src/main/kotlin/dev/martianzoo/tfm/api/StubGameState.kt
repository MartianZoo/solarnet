package dev.martianzoo.tfm.api

import com.google.common.collect.Multiset
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.typeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

open class StubGameState(val auth: Authority = Authority.Minimal()) : GameState {
  override fun applyChange(
      count: Int,
      gaining: GenericTypeExpression?,
      removing: GenericTypeExpression?,
      cause: Cause?,
      amap: Boolean,
  ): Unit = throe()

  override val setup = GameSetup(auth, "BM", 2)

  override fun resolve(type: TypeExpression): TypeInfo = throe()

  override fun count(type: TypeExpression): Int = throe()

  override fun getAll(type: TypeExpression): Multiset<TypeExpression> = throe()
  override fun getAll(type: GenericTypeExpression): Multiset<GenericTypeExpression> = throe()
  override fun getAll(type: ClassLiteral): Set<ClassLiteral> = throe()
  override fun getAll(typeText: String): Multiset<TypeExpression> = throe()

  override fun isMet(requirement: Requirement): Boolean = throe()

  private fun throe(): Nothing = throw RuntimeException("this is just a stub")
}
