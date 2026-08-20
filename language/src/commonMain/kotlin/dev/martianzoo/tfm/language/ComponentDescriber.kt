package dev.martianzoo.tfm.language

/** Sparse English-language facts declared for one component Class. */
public data class ComponentDescriber(
    public val noun: Noun? = null,
    public val discardable: Boolean? = null,
    public val standardResource: Boolean? = null,
    public val cardResource: CardResource? = null,
    public val cardResourceHolder: String? = null,
    public val metricLocation: String? = null,
    public val tag: Tag? = null,
    public val track: Track? = null,
    public val placement: Placement? = null,
    public val placementSite: PlacementSite? = null,
    public val spatialRelation: SpatialRelation? = null,
    /** Whether a structurally empty direct subclass adds no English text of its own. */
    public val textNeutralSubclasses: Boolean = false,
    public val production: Boolean? = null,
    public val requirement: Requirement? = null,
    public val directGain: DirectGain? = null,
    public val draw: Boolean? = null,
    public val score: Score? = null,
    public val endTrigger: Boolean? = null,
    public val playTrigger: PlayTrigger? = null,
    public val playedCard: Boolean? = null,
    public val playedTagPhrase: String? = null,
    public val operationTrigger: String? = null,
    public val usedActionTrigger: Boolean? = null,
    public val actionUse: ActionUse? = null,
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

  public data class PlacementSite(
      public val noun: Noun,
      public val article: String? = null,
  )

  public data class SpatialRelation(
      public val phrase: String,
      public val defaultTarget: Noun.Counted,
  )

  public enum class MetricOwner {
    YOU,
    ANY_PLAYER,
  }

  public data class Requirement(
      public val minimum: Bound? = null,
      public val maximum: Bound? = null,
      public val ownedCount: Noun? = null,
  ) {
    public sealed interface Bound {
      public data class Threshold(
          public val subject: String,
          public val value: Value = Value.PLAIN,
          public val syntax: ThresholdSyntax,
      ) : Bound

      public data class Count(
          public val noun: Noun.Counted,
          public val syntax: CountSyntax,
          public val anyoneSyntax: CountSyntax? = null,
      ) : Bound
    }

    public enum class Value {
      PLAIN,
      PERCENT,
      DOUBLE_PERCENT,
      TEMPERATURE,
    }

    public enum class ThresholdSyntax {
      REQUIRES_VALUE_SUBJECT,
      REQUIRES_SUBJECT_VALUE,
      REQUIRES_VALUE_OR_WARMER,
      REQUIRES_HAVE_SUBJECT_OF_VALUE_OR_MORE,
      SUBJECT_MUST_BE_VALUE_OR_LESS,
      SUBJECT_MUST_BE_VALUE_OR_COLDER,
    }

    public enum class CountSyntax {
      REQUIRES_COUNT,
      REQUIRES_OWNED_COUNT,
      THERE_MUST_BE_COUNT_OR_FEWER,
      YOU_MUST_HAVE_NO_MORE_THAN_COUNT,
    }
  }

  public data class DirectGain(public val noun: String, public val count: Int)

  public data class Score(public val singular: String, public val plural: String)

  public data class ActionUse(
      public val objectPhrase: String,
      public val refundDiscountPredicate: String? = null,
  )

  public enum class PlayTrigger {
    CARD,
    TAG,
  }
}
