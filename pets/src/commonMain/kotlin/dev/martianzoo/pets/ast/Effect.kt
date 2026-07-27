package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.util.iff

/**
 * A triggered effect, like `CityTile: 2`. Any existing component in a game state can have some
 * number of these, which are all active until the component is removed.
 */
public data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val automatic: Boolean = false,
) : PetElement() {

  override val kind: kotlin.reflect.KClass<out PetNode> = Effect::class

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(trigger, instruction)

  override fun toString(): String =
      "$trigger:${":".iff(automatic)} " +
          if (instruction is Gated) "($instruction)" else "$instruction"

  /** The left-hand side of a triggered effect; the kind of event being subscribed to. */
  public sealed class Trigger : PetNode() {
    override val kind: kotlin.reflect.KClass<out PetNode> = Trigger::class

    public sealed class BasicTrigger : Trigger()

    public object WhenGain : BasicTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = Unit

      override fun toString(): String = "This"
    }

    public object WhenRemove : BasicTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = Unit

      override fun toString(): String = "-This"
    }

    @ConsistentCopyVisibility
    public data class OnGainOf private constructor(val expression: Expression) : BasicTrigger() {
      internal companion object {
        internal fun create(expression: Expression): BasicTrigger {
          if (expression.className == CLASS) {
            throw PetSyntaxException("Class types cannot be used as effect triggers: $expression")
          }
          return if (expression == THIS.expression) {
            WhenGain
          } else {
            OnGainOf(expression)
          }
        }
      }

      init {
        require(expression != THIS.expression)
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

      override fun toString(): String = "$expression"
    }

    @ConsistentCopyVisibility
    public data class OnRemoveOf private constructor(val expression: Expression) : BasicTrigger() {
      internal companion object {
        internal fun create(expression: Expression): BasicTrigger {
          if (expression.className == CLASS) {
            throw PetSyntaxException("Class types cannot be used as effect triggers: -$expression")
          }
          return if (expression == THIS.expression) {
            WhenRemove
          } else {
            OnRemoveOf(expression)
          }
        }
      }

      init {
        require(expression != THIS.expression)
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

      override fun toString(): String = "-$expression"
    }

    public sealed class WrappingTrigger : Trigger() {
      public abstract val inner: Trigger

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)
    }

    public data class ByTrigger(override val inner: Trigger, val by: Expression) :
        WrappingTrigger() {
      public constructor(inner: Trigger, by: ClassName) : this(inner, by.expression)

      init {
        if (inner is ByTrigger) throw PetSyntaxException("by the by")
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, by)

      override fun toString(): String = "$inner BY $by"
    }

    public data class IfTrigger(override val inner: Trigger, val condition: Requirement) :
        WrappingTrigger() {
      init {
        if (inner is ByTrigger) throw PetSyntaxException("if the by")
        if (inner is IfTrigger) throw PetSyntaxException("if the if")
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, condition)

      override fun toString(): String = "$inner IF ${groupPartIfNeeded(condition)}"
    }

    public data class XTrigger(override val inner: BasicTrigger) : WrappingTrigger() {
      override fun toString(): String {
        return when (inner) {
          is OnGainOf,
          is WhenGain -> "X $inner"
          is OnRemoveOf,
          is WhenRemove -> "-X ${inner.toString().substring(1)}"
        }
      }
    }

    public data class Transform(override val inner: Trigger, override val transformKind: String) :
        WrappingTrigger(), TransformNode<Trigger> {
      override fun toString(): String = "$transformKind[$inner]"

      init {
        if (inner !is OnGainOf && inner !is OnRemoveOf && inner !is XTrigger) {
          throw PetSyntaxException("only gain/remove trigger can go in transform block")
        }
      }

      override fun extract(): Trigger = inner
    }

    internal companion object : PetTokenizer() {
      fun parser(): Parser<Trigger> {
        return parser {
          val onGainOf: Parser<BasicTrigger> = Expression.parser() map OnGainOf.Companion::create

          val exxedGain: Parser<XTrigger> = skip(_x) and onGainOf map Trigger::XTrigger

          val onRemoveOf: Parser<BasicTrigger> =
              skipChar('-') and Expression.parser() map OnRemoveOf.Companion::create

          val exxedRemove: Parser<XTrigger> =
              skipChar('-') and
                  skip(_x) and
                  Expression.parser() map
                  OnRemoveOf.Companion::create map
                  Trigger::XTrigger

          val atom: Parser<Trigger> = exxedGain or exxedRemove or onGainOf or onRemoveOf
          val transform = transform(atom) map { (node, name) -> Transform(node, name) }
          val ifClause: Parser<Requirement> = skip(_if) and Requirement.atomParser()
          val byClause: Parser<Expression> = skip(_by) and Expression.parser()

          (transform or atom) and
              optional(ifClause) and
              optional(byClause) map
              { (inTrigger, `if`, by) ->
                var trig = inTrigger
                if (`if` != null) trig = IfTrigger(trig, `if`)
                if (by != null) trig = ByTrigger(trig, by)
                trig
              }
        }
      }
    }
  }

  internal companion object : PetTokenizer() {
    fun parser(): Parser<Effect> {
      val colons = _doubleColon or char(':') map { it.text == "::" }

      return Trigger.parser() and
          colons and
          maybeGroup(Instruction.parser()) map
          { (trig, immed, instr) ->
            Effect(trigger = trig, automatic = immed, instruction = instr)
          }
    }
  }
}
