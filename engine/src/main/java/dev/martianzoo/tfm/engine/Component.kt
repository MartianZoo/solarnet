package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.engine.Component.ActiveTrigger.Companion.from
import dev.martianzoo.tfm.engine.Game.AbstractInstructionException
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasExpression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType

/**
 * An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [MType] instead.
 */
public data class Component private constructor(val mtype: MType) : HasExpression by mtype {

  companion object {
    public fun ofType(mtype: MType): Component = Component(mtype)
  }

  init {
    if (mtype.abstract) {
      throw AbstractInstructionException("Component of abstract type: ${mtype.expressionFull}")
    }
  }

  /**
   * Whether this type is categorically a subtype of [thatType] for any possible game state. (In the
   * absence of refinements, this is an ordinary subtype check.)
   */
  public fun hasType(thatType: MType) = mtype.isSubtypeOf(thatType)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [MClass.dependencies].
   */
  public val dependencyComponents = mtype.dependencies.asSet.map { ofType(it.boundType) }

  /**
   * This component's effects; while the component exists in a game state, the effects are active.
   */
  public fun effects(): List<Effect> {
    return mtype.mclass.classEffects.map { decl ->
      // val linkages = decl.linkages
      // Transform for some "linkages" (TODO the rest, and do in more principled way)
      val effect = mtype.mclass.loader.transformer
          .deprodify(decl.effect) // ridiculoso
          .replaceAll(THIS.classExpression(), expression.className.classExpression())
          .replaceAll(THIS.expr, expressionFull)
      owner()?.let { effect.replaceAll(OWNER, it) } ?: effect
    }
  }

  val activeEffects: List<ActiveEffect> by lazy {
    effects().map {
      try {
        ActiveEffect(this, from(it.trigger), it.automatic, it.instruction)
      } catch (e: Exception) {
        println(it)
        throw e
      }
    }
  }

  data class ActiveEffect(
      val contextComponent: Component,
      val trigger: ActiveTrigger,
      val automatic: Boolean,
      val instruction: Instruction
  ) {
    fun getInstruction(record: ChangeRecord, game: Game): Instruction? {
      val hit = trigger.match(record, game) ?: return null
      return hit.fixer(instruction) * hit.count
    }
    fun onSelfChange(record: ChangeRecord, game: Game): Instruction? {
      val hit = trigger.matchSelf(record, game) ?: return null
      return hit.fixer(instruction) * hit.count
    }
  }

  sealed class ActiveTrigger {
    companion object {
      fun from(trigger: Trigger): ActiveTrigger {
        return when (trigger) {
          is Trigger.ByTrigger -> ByDoer(from(trigger.inner), trigger.by)
          is Trigger.WhenGain -> Self(true)
          is Trigger.WhenRemove -> Self(false)
          is Trigger.OnGainOf -> OnChange(trigger.expression, gaining = true)
          is Trigger.OnRemoveOf -> OnChange(trigger.expression, gaining = false)
          is Trigger.Transform -> error("should have been transformed by now")
        }
      }
    }

    data class Hit(val count: Int, val fixer: (Instruction) -> Instruction = { it })

    abstract fun match(record: ChangeRecord, game: Game): Hit?
    abstract fun matchSelf(record: ChangeRecord, game: Game): Hit?

    data class ByDoer(val inner: ActiveTrigger, val by: ClassName) : ActiveTrigger() {
      override fun match(record: ChangeRecord, game: Game): Hit? {
        val contextP: ClassName? =
            record.cause?.let { game.toComponent(it.contextComponent).owner() }
        if (isPlayerSpecific() && contextP != by) return null

        val hit = inner.match(record, game) ?: return null

        return if (by == OWNER && contextP != null) {
          hit.copy { hit.fixer(it).replaceAll(OWNER, contextP) }
        } else {
          hit
        }
      }

      override fun matchSelf(record: ChangeRecord, game: Game): Hit? {
        val contextP: ClassName? = record.cause?.doer
        if (isPlayerSpecific() && contextP != by) return null

        val hit = inner.matchSelf(record, game) ?: return null

        return if (by == OWNER && contextP != null) {
          hit.copy { hit.fixer(it).replaceAll(OWNER, contextP) }
        } else {
          hit
        }
      }

      fun isPlayerSpecific() = by.toString().startsWith("Player")
    }

    data class OnChange(val match: Expression, val gaining: Boolean) : ActiveTrigger() {
      override fun match(record: ChangeRecord, game: Game): Hit? {
        val expr: Expression? = if (gaining) record.change.gaining else record.change.removing
        return expr?.let {
          if (game.resolve(it).isSubtypeOf(game.resolve(match))) {
            Hit(record.change.count)
          } else null
        }
      }

      // It should not need to match itself since it will already be included in the sweep
      override fun matchSelf(record: ChangeRecord, game: Game) = null
    }

    data class Self(val gaining: Boolean) : ActiveTrigger() {
      // This never matches, because an *existing* Foo would trigger on *another* Foo
      override fun match(record: ChangeRecord, game: Game) = null

      override fun matchSelf(record: ChangeRecord, game: Game): Hit? {
        if ((record.change.gaining != null) != gaining) return null
        return Hit(record.change.count) // important because Mons's special trigger will return 1
      }
    }
  }

  public fun owner(): ClassName? = mtype.dependencies.getIfPresent(Key(OWNED, 0))?.className

  override fun toString() = "[${mtype.expressionFull}]"
}
