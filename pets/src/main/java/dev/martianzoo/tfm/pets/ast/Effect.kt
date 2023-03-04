package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.util.iff

public data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val automatic: Boolean = false,
) : PetNode(), Comparable<Effect> {

  override val kind = Effect::class.simpleName!!

  override fun visitChildren(visitor: Visitor) = visitor.visit(trigger, instruction)

  override fun toString() =
      "$trigger:${":".iff(automatic)} " +
          if (instruction is Gated) "($instruction)" else "$instruction"

  override fun compareTo(other: Effect): Int = effectComparator.compare(this, other)

  sealed class Trigger : PetNode() {
    override val kind = Trigger::class.simpleName!!

    data class ByTrigger(val inner: Trigger, val by: ClassName) : Trigger() {
      init {
        if (inner is ByTrigger) {
          throw PetException("by the by")
        }
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(inner, by)
      override fun toString() = "$inner BY $by"
    }

    object WhenGain : Trigger() {
      override fun visitChildren(visitor: Visitor) = Unit
      override fun toString() = "This"
    }

    object WhenRemove : Trigger() {
      override fun visitChildren(visitor: Visitor) = Unit
      override fun toString() = "-This"
    }

    data class OnGainOf private constructor(val typeExpr: TypeExpr) : Trigger() {
      companion object {
        fun create(typeExpr: TypeExpr): Trigger {
          return if (typeExpr == THIS.type) {
            WhenGain
          } else {
            OnGainOf(typeExpr)
          }
        }
      }
      init {
        require(typeExpr != THIS.type)
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(typeExpr)
      override fun toString() = "$typeExpr"
    }

    data class OnRemoveOf private constructor(val typeExpr: TypeExpr) : Trigger() {
      companion object {
        fun create(typeExpr: TypeExpr): Trigger {
          return if (typeExpr == THIS.type) {
            WhenRemove
          } else {
            OnRemoveOf(typeExpr)
          }
        }
      }
      init {
        require(typeExpr != THIS.type)
      }

      override fun visitChildren(visitor: Visitor) = visitor.visit(typeExpr)
      override fun toString() = "-$typeExpr"
    }

    data class Transform(val trigger: Trigger, override val transformKind: String) :
        Trigger(), GenericTransform<Trigger> {
      override fun visitChildren(visitor: Visitor) = visitor.visit(trigger)
      override fun toString() = "$transformKind[$trigger]"

      init {
        if (trigger !is OnGainOf && trigger !is OnRemoveOf) {
          throw PetException("only gain/remove trigger can go in transform block")
        }
      }

      override fun extract() = trigger
    }

    companion object : PetParser() {
      fun trigger(text: String): Trigger = Parsing.parse(parser(), text)

      fun parser(): Parser<Trigger> {
        val onGainOf = TypeExpr.parser() map OnGainOf::create
        val onRemoveOf = skipChar('-') and TypeExpr.parser() map OnRemoveOf::create
        val atom = onGainOf or onRemoveOf
        val transform =
            transform(atom) map { (node, transformName) -> Transform(node, transformName) }
        return (transform or atom) and
            optional(skip(_by) and ClassName.Parsing.className) map { (trig, by) ->
              if (by == null) trig else ByTrigger(trig, by)
            }
      }
    }
  }

  companion object : PetParser() {
    fun effect(text: String): Effect = Parsing.parse(parser(), text)

    fun parser(): Parser<Effect> {
      val colons = _doubleColon or char(':') map { it.text == "::" }

      return Trigger.parser() and
          colons and
          maybeGroup(Instruction.parser()) map
          { (trig, immed, instr) ->
            Effect(trigger = trig, automatic = immed, instruction = instr)
          }
    }

    // TODO really a trigger comparator
    private val effectComparator: Comparator<Effect> =
        compareBy(
            {
              val t = it.trigger
              when {
                t == WhenGain -> if (it.automatic) -1 else 0
                t == WhenRemove -> if (it.automatic) 1 else 2
                t is OnGainOf && "${t.typeExpr.className}".startsWith("${SpecialClassNames.USE_ACTION}") -> 4
                t == OnGainOf.create(SpecialClassNames.END.type) -> 5
                else -> 3
              }
            },
            { it.trigger.toString() },
        )
  }
}
