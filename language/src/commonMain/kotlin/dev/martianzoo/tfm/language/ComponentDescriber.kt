package dev.martianzoo.tfm.language

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

  public enum class Requirement {
    CITY_TILES,
    COLONIES,
    GREENERY_TILES,
    OCEAN_TILES,
    OXYGEN_PERCENT,
    TEMPERATURE,
    TERRAFORM_RATING,
    VENUS_PERCENT,
  }

  public data class DirectGain(public val noun: String, public val count: Int)

  public data class Score(public val singular: String, public val plural: String)
}
