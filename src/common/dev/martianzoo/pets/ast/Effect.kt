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
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.util.iff

/**
 * A rule attached to a class, like `CityTile: 2 MC`, as defined by
 * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects).
 * Every component of that class carries the rule for as long as it exists.
 *
 * An effect is a trigger, a colon, and an instruction ([rule
 * L8-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)):
 * the trigger says which event the rule is about, and the instruction says how the state after that
 * event relates to the state before it.
 *
 * An effect round-trips, and rendering parenthesizes a gated instruction after the colon so that
 * the effect's own colon stays unambiguous ([rule
 * L8-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)).
 */
public data class Effect(
    /** The event this rule is about. */
    val trigger: Trigger,

    /** How the state after the triggering event relates to the state before it. */
    val instruction: InstructionTree,

    /**
     * Whether this effect was written `::` rather than `:`, marking a consequence that carries no
     * choice and that the rule intends to be inseparable from the event causing it ([rule
     * L8-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)).
     * When that distinction matters is `SEQUENCING.md`'s subject.
     */
    val automatic: Boolean = false,
) : PetElement() {
  init {
    // A bare Component subscription watches everything and states nothing; rule L8-9 requires it to
    // say what it is actually watching for.
    trigger.unqualifiedBroadSubscription()?.let {
      throw PetSyntaxException("$it trigger requires IF or BY")
    }
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Effect::class

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(trigger, instruction)

  override fun toString(): String =
      "$trigger:${":".iff(automatic)} " +
          if (instruction is Gated) "($instruction)" else "$instruction"

  /**
   * The left-hand side of an [Effect]; the kind of event the rule is about. There are two kinds: a
   * [SelfTrigger] about this very component, and a [SubscribedTrigger] about matching changes
   * anywhere ([rule
   * L8-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)).
   */
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

    /**
     * Fires when any of [triggers] does. Self and subscribed triggers may not mix ([rule
     * L8-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)):
     * `This OR -This` is fine, but `This OR Plant` is not, because one is about this component and
     * the other about the world. `OR` binds most tightly of the trigger operators ([rule
     * L8-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)).
     */
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

    /** The gain of the component carrying this effect, spelled `This`. */
    public object WhenGain : SelfTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = Unit

      override fun toString(): String = "This"
    }

    /** The removal of the component carrying this effect, spelled `-This`. */
    public object WhenRemove : SelfTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = Unit

      override fun toString(): String = "-This"
    }

    /** A subscription to gains of components matching [expression]. */
    @ConsistentCopyVisibility
    public data class OnGainOf private constructor(val expression: Expression) :
        SubscribedTrigger() {
      public companion object {
        /**
         * Returns the trigger for gains of [expression], which is [WhenGain] when [expression] is
         * the bare `This` placeholder however its empty argument list was written ([rules
         * L8-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)
         * and
         * [L3-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)).
         *
         * @throws PetSyntaxException if [expression] is a class literal, which nothing ever gains
         *   ([rule
         *   L8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects),
         *   [rule T4-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals))
         */
        public fun create(expression: Expression): BasicTrigger {
          if (expression.className == CLASS) {
            throw PetSyntaxException("Class types cannot be used as effect triggers: $expression")
          }
          return if (expression.isBare(THIS)) {
            WhenGain
          } else {
            OnGainOf(expression)
          }
        }
      }

      init {
        require(!expression.isBare(THIS))
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

      override fun toString(): String = "$expression"
    }

    /** A subscription to removals of components matching [expression]. */
    @ConsistentCopyVisibility
    public data class OnRemoveOf private constructor(val expression: Expression) :
        SubscribedTrigger() {
      public companion object {
        /**
         * Returns the trigger for removals of [expression], which is [WhenRemove] when [expression]
         * is the bare `This` placeholder however its empty argument list was written ([rules
         * L8-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)
         * and
         * [L3-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)).
         *
         * @throws PetSyntaxException if [expression] is a class literal, which nothing ever removes
         *   ([rule
         *   L8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects),
         *   [rule T4-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals))
         */
        public fun create(expression: Expression): BasicTrigger {
          if (expression.className == CLASS) {
            throw PetSyntaxException("Class types cannot be used as effect triggers: -$expression")
          }
          return if (expression.isBare(THIS)) {
            WhenRemove
          } else {
            OnRemoveOf(expression)
          }
        }
      }

      init {
        require(!expression.isBare(THIS))
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

      override fun toString(): String = "-$expression"
    }

    /** A trigger that qualifies or marks another one. */
    public sealed class WrappingTrigger : Trigger() {
      /** The trigger being qualified or marked. */
      public abstract val inner: Trigger

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)
    }

    /**
     * Restricts [inner] to events performed by an actor matching [by] ([rule
     * L8-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)).
     * Because the selector is an expression, `BY Player(NOT Owner)` filters while `BY Player` may
     * declare an actor variable ([rule
     * T13-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
     * It binds less tightly than `OR` and more tightly than `IF`.
     */
    public data class ByTrigger(override val inner: Trigger, val by: Expression) :
        WrappingTrigger() {
      /** Restricts [inner] to the actor class [by], with no arguments or refinement. */
      public constructor(inner: Trigger, by: ClassName) : this(inner, by.expression)

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, by)

      override fun toString(): String = "${groupPartIfNeeded(inner)} BY $by"

      override fun precedence(): Int = 20
    }

    /**
     * Restricts [inner] to events occurring while [condition] holds ([rule
     * L8-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects))
     * — a question about state, where a [ByTrigger] asks who acted. It binds least tightly of the
     * trigger operators.
     */
    public data class IfTrigger(override val inner: Trigger, val condition: Requirement) :
        WrappingTrigger() {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, condition)

      override fun toString(): String =
          "${groupPartIfNeeded(inner)} IF ${groupPartIfNeeded(condition)}"

      override fun precedence(): Int = 10
    }

    /**
     * Binds the size of the change [inner] watches for, so that `X Plant: X Heat` reacts to a gain
     * of any number of plants with the same number of heat ([rule
     * L8-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)).
     * A removal is written `-X Plant`.
     */
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

    /**
     * An [inner] trigger marked for rewriting by the handler named by [transformKind]. Per
     * [rule L10-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)
     * the mark applies to the event being watched, so it wraps only a gain or removal — never `OR`,
     * `BY` or `IF`.
     */
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
            Effect(
                trigger = trig,
                automatic = immed,
                instruction = instr,
            )
          }
    }
  }
}

/**
 * Returns a `Component` subscription reached without passing through an `IF` or `BY`, or null if
 * there is none.
 * [Rule L8-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-effects)
 * rejects such an unqualified universe-wide watcher.
 */
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
