package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.TypeLinking
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.util.iff

/**
 * A triggered effect, like `CityTile: 2`. Any existing component in a world can have some number of
 * these, which are all active until the component is removed.
 */
public data class Effect(
    val trigger: Trigger,
    val instruction: InstructionTree,
    val automatic: Boolean = false,
) : PetElement() {
  init {
    trigger.unqualifiedBroadSubscription()?.let {
      throw PetSyntaxException("$it trigger requires IF or BY")
    }
  }

  public val linkedTypeSources: Set<Expression>
    get() = recordedLinkedTypeSources

  override val kind: kotlin.reflect.KClass<out PetNode> = Effect::class

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(trigger, instruction)

  override fun toString(): String =
      "$trigger:${":".iff(automatic)} " +
          if (instruction is Gated) "($instruction)" else "$instruction"

  /** The left-hand side of a triggered effect; the kind of event being subscribed to. */
  public sealed class Trigger : PetNode() {
    override val kind: kotlin.reflect.KClass<out PetNode> = Trigger::class

    /**
     * An unmodified gain-or-removal selector: either a self event or a subscription to a component
     * type. Basic triggers are the operands accepted by scalar and transform wrappers. They do not
     * themselves include `OR`, `IF`, or `BY`.
     */
    public sealed class BasicTrigger : Trigger()

    /**
     * A gain or removal of the concrete component carrying this effect, spelled `This` or `-This`.
     * This is not a subscription to that component's type: changing N copies scales this effect's
     * instruction by N once, regardless of how many other copies of the component already exist.
     */
    public sealed class SelfTrigger : BasicTrigger()

    /**
     * A subscription to gains or removals matching an authored component expression. Each active
     * copy of the effect-bearing component owns this subscription, so its multiplicity affects how
     * many times a matching change triggers the effect.
     */
    public sealed class SubscribedTrigger : BasicTrigger()

    public data class Or(val triggers: List<Trigger>) : Trigger() {
      init {
        require(triggers.size >= 2)
        if (triggers.map { it.selfMode() }.distinct().size != 1) {
          throw PetSyntaxException("OR trigger cannot mix This with subscribed triggers")
        }
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(triggers)

      override fun toString(): String = triggers.joinToString(" OR ") { groupPartIfNeeded(it) }

      override fun precedence(): Int = 30
    }

    public object WhenGain : SelfTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = Unit

      override fun toString(): String = "This"
    }

    public object WhenRemove : SelfTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = Unit

      override fun toString(): String = "-This"
    }

    @ConsistentCopyVisibility
    public data class OnGainOf private constructor(val expression: Expression) :
        SubscribedTrigger() {
      public companion object {
        public fun create(expression: Expression): BasicTrigger {
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
    public data class OnRemoveOf private constructor(val expression: Expression) :
        SubscribedTrigger() {
      public companion object {
        public fun create(expression: Expression): BasicTrigger {
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

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, by)

      override fun toString(): String = "${groupPartIfNeeded(inner)} BY $by"

      override fun precedence(): Int = 20
    }

    public data class IfTrigger(override val inner: Trigger, val condition: Requirement) :
        WrappingTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, condition)

      override fun toString(): String =
          "${groupPartIfNeeded(inner)} IF ${groupPartIfNeeded(condition)}"

      override fun precedence(): Int = 10
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

    private fun selfMode(): Boolean =
        when (this) {
          is SelfTrigger -> true
          is SubscribedTrigger -> false
          is Or -> triggers.first().selfMode()
          is WrappingTrigger -> inner.selfMode()
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
          val unmodified = transform or atom
          val primary = unmodified or group(parser())
          val alternatives =
              separatedTerms(primary, _or) map { if (it.size == 1) it.first() else Or(it) }
          val byClause: Parser<Expression> = skip(_by) and Expression.parser()
          val byTrigger =
              alternatives and
                  optional(byClause) map
                  { (inner, by) ->
                    if (by == null) inner else ByTrigger(inner, by)
                  }
          val ifClause: Parser<Requirement> = skip(_if) and Requirement.parser()

          byTrigger and
              optional(ifClause) map
              { (inner, condition) ->
                if (condition == null) inner else IfTrigger(inner, condition)
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
          maybeGroup(InstructionTree.parser()) map
          { (trig, immed, instr) ->
            val effect =
                Effect(
                    trigger = trig,
                    automatic = immed,
                    instruction = instr,
                )
            effect.withLinkedTypeSources(TypeLinking.sourcesAcrossRegions(effect))
          }
    }
  }
}

private fun Effect.Trigger.unqualifiedBroadSubscription(qualified: Boolean = false): Expression? =
    when (this) {
      is Effect.Trigger.OnGainOf -> expression.takeIf { !qualified && it.className == COMPONENT }
      is Effect.Trigger.OnRemoveOf -> expression.takeIf { !qualified && it.className == COMPONENT }
      is Effect.Trigger.SelfTrigger -> null
      is Effect.Trigger.Or ->
          triggers.firstNotNullOfOrNull { it.unqualifiedBroadSubscription(qualified) }
      is Effect.Trigger.ByTrigger,
      is Effect.Trigger.IfTrigger -> inner.unqualifiedBroadSubscription(qualified = true)
      is Effect.Trigger.XTrigger,
      is Effect.Trigger.Transform -> inner.unqualifiedBroadSubscription(qualified)
    }
