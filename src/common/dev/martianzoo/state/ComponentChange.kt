package dev.martianzoo.state

/**
 * A fully resolved change to a world's component multiset. [count] is positive; [gaining] and
 * [removing] identify the exact component types affected, with null for an absent side.
 */
public sealed class ComponentChange private constructor() {
  /** Positive number of component instances affected. */
  public abstract val count: Int
  /** Component type gained, or null for a pure removal. */
  public abstract val gaining: Component?
  /** Component type removed, or null for a pure gain. */
  public abstract val removing: Component?

  /** Gains [count] copies of [component], with [count] greater than zero. */
  public data class Gain(
      override val count: Int = 1,
      public val component: Component,
  ) : ComponentChange() {
    init {
      require(count > 0)
    }

    override val gaining: Component
      get() = component

    override val removing: Component?
      get() = null
  }

  /** Removes [count] copies of [component], with [count] greater than zero. */
  public data class Remove(
      override val count: Int = 1,
      public val component: Component,
  ) : ComponentChange() {
    init {
      require(count > 0)
    }

    override val gaining: Component?
      get() = null

    override val removing: Component
      get() = component
  }

  /** Replaces [count] copies of [removing] with [gaining]; [count] must be positive. */
  public data class Transmute(
      override val count: Int = 1,
      override val gaining: Component,
      override val removing: Component,
  ) : ComponentChange() {
    init {
      require(count > 0)
    }
  }

  internal fun reversed(): ComponentChange =
      when (this) {
        is Gain -> Remove(count, component)
        is Remove -> Gain(count, component)
        is Transmute -> Transmute(count, gaining = removing, removing = gaining)
      }

  final override fun toString(): String {
    val countText = if (count == 1) "" else "$count "
    return when (this) {
      is Gain -> "+$countText$component"
      is Remove -> "-$countText$component"
      is Transmute -> "+$countText$gaining FROM $removing"
    }
  }
}
