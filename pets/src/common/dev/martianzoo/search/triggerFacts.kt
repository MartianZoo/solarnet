package dev.martianzoo.search

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger

/** Classifies observed events, retaining conditions, actors and transforms in the original tree. */
internal fun isSelfGainOnly(trigger: Trigger): Boolean =
    when (trigger) {
      Trigger.WhenGain -> true
      is Trigger.Or -> trigger.triggers.all(::isSelfGainOnly)
      is Trigger.WrappingTrigger -> isSelfGainOnly(trigger.inner)
      is Trigger.OnGainOf,
      is Trigger.OnRemoveOf,
      Trigger.WhenRemove -> false
    }

internal fun isEndOnly(trigger: Trigger): Boolean =
    when (trigger) {
      is Trigger.OnGainOf -> trigger.expression.className == END
      is Trigger.Or -> trigger.triggers.all(::isEndOnly)
      is Trigger.WrappingTrigger -> isEndOnly(trigger.inner)
      is Trigger.OnRemoveOf,
      Trigger.WhenGain,
      Trigger.WhenRemove -> false
    }

private val END = cn("End")
