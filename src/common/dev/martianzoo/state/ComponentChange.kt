package dev.martianzoo.state

/** One fully resolved change to the component state of a game. */
public sealed class ComponentChange {
  public abstract val count: Int
  public abstract val gaining: Component?
  public abstract val removing: Component?

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

  public data class Transmute(
      override val count: Int = 1,
      override val gaining: Component,
      override val removing: Component,
  ) : ComponentChange() {
    init {
      require(count > 0)
      require(gaining != removing) { "both gaining and removing $gaining" }
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
