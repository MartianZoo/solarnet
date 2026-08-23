package dev.martianzoo.tfm.language

/** Sparse English-language facts declared for one component Class. */
internal data class ComponentDescriber(
    internal val noun: Noun? = null,
    internal val discardable: Boolean? = null,
    internal val standardResource: Boolean? = null,
    internal val cardResource: CardResource? = null,
    internal val cardResourceHolder: String? = null,
    internal val metricLocation: String? = null,
    internal val tag: Tag? = null,
    internal val track: Track? = null,
    internal val placement: Placement? = null,
    internal val production: Boolean? = null,
    public val requirement: Requirement? = null,
    internal val directGain: DirectGain? = null,
    internal val score: Score? = null,
    internal val endTrigger: Boolean? = null,
    internal val playTrigger: PlayTrigger? = null,
    internal val playedCard: Boolean? = null,
    internal val playedTagPhrase: String? = null,
    internal val operationTrigger: String? = null,
    internal val usedActionTrigger: Boolean? = null,
    internal val actionUse: ActionUse? = null,
    internal val spentResourceTrigger: Boolean? = null,
    internal val owedPayment: Boolean? = null,
) {
  internal sealed interface Noun {
    public data object ClassName : Noun

    public data class Fixed(public val text: String) : Noun

    public data class Counted(public val singular: String, public val plural: String) : Noun
  }

  internal enum class CardResource {
    ORDINARY,
    SUFFIXED,
  }

  internal enum class Tag {
    ORDINARY,
    PLANET,
  }

  internal data class Track(public val subject: String)

  internal data class Placement(
      internal val article: String,
      internal val singular: String,
      internal val plural: String,
      internal val consequence: String? = null,
      internal val allowsMultiple: Boolean = true,
      internal val unqualifiedMetricOwner: MetricOwner? = null,
      internal val anyoneMetricOwner: MetricOwner? = null,
  )

  internal enum class MetricOwner {
    YOU,
    ANY_PLAYER,
  }

  public data class Requirement(
      internal val minimum: Bound? = null,
      internal val maximum: Bound? = null,
      internal val ownedCount: Noun? = null,
  ) {
    internal sealed interface Bound {
      public data class Threshold(
          internal val subject: String,
          internal val value: Value = Value.PLAIN,
          internal val syntax: ThresholdSyntax,
      ) : Bound

      public data class Count(
          internal val noun: Noun.Counted,
          internal val syntax: CountSyntax,
          internal val anyoneSyntax: CountSyntax? = null,
      ) : Bound
    }

    internal enum class Value {
      PLAIN,
      PERCENT,
      DOUBLE_PERCENT,
      TEMPERATURE,
    }

    internal enum class ThresholdSyntax {
      REQUIRES_VALUE_SUBJECT,
      REQUIRES_SUBJECT_VALUE,
      REQUIRES_VALUE_OR_WARMER,
      REQUIRES_HAVE_SUBJECT_OF_VALUE_OR_MORE,
      SUBJECT_MUST_BE_VALUE_OR_LESS,
      SUBJECT_MUST_BE_VALUE_OR_COLDER,
    }

    internal enum class CountSyntax {
      REQUIRES_COUNT,
      REQUIRES_OWNED_COUNT,
      THERE_MUST_BE_COUNT_OR_FEWER,
      YOU_MUST_HAVE_NO_MORE_THAN_COUNT,
    }
  }

  internal data class DirectGain(public val noun: String, public val count: Int)

  internal data class Score(public val singular: String, public val plural: String)

  internal data class ActionUse(
      internal val objectPhrase: String,
      internal val refundDiscountPredicate: String? = null,
  )

  internal enum class PlayTrigger {
    CARD,
    TAG,
  }
}
