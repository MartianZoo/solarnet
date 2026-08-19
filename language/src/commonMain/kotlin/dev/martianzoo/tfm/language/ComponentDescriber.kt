package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression

/** Sparse English-language facts declared for one component Class. */
public data class ComponentDescriber(
    public val noun: Noun? = null,
    public val standardResource: Boolean? = null,
    public val cardResource: CardResource? = null,
    public val cardResourceHolder: String? = null,
    public val metricLocation: String? = null,
    public val tag: Tag? = null,
    public val track: Track? = null,
    public val placement: Placement? = null,
    public val requirement: Requirement? = null,
    public val directGain: DirectGain? = null,
    public val score: Score? = null,
    public val endTrigger: Boolean? = null,
    public val playTrigger: PlayTrigger? = null,
    public val playedCard: Boolean? = null,
    public val spentResourceTrigger: Boolean? = null,
    public val owedPayment: Boolean? = null,
) {
  public sealed interface Noun {
    public data object ClassName : Noun

    public data class Fixed(public val text: String) : Noun

    public data class Counted(public val singular: String, public val plural: String) : Noun
  }

  public enum class CardResource {
    ORDINARY,
    SUFFIXED,
  }

  public enum class Tag {
    ORDINARY,
    PLANET,
  }

  public data class Track(public val subject: String)

  public data class Placement(
      public val article: String,
      public val singular: String,
      public val plural: String,
      public val consequence: String? = null,
      public val allowsMultiple: Boolean = true,
      public val unqualifiedMetricOwner: MetricOwner? = null,
      public val anyoneMetricOwner: MetricOwner? = null,
  )

  public enum class MetricOwner {
    YOU,
    ANY_PLAYER,
  }

  public interface Requirement {
    public fun renderMinimum(expression: Expression, target: Int): String?

    public fun renderMaximum(expression: Expression, target: Int): String? = null

    public fun renderOwnedCount(target: Int): String? = null
  }

  public data class DirectGain(public val noun: String, public val count: Int)

  public data class Score(public val singular: String, public val plural: String)

  public enum class PlayTrigger {
    CARD,
    TAG,
  }
}
