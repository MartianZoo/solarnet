package dev.martianzoo.tfm.language

/** Sparse English-language facts declared for one component Class. */
public data class ComponentDescriber(
    public val noun: Noun? = null,
    public val discardable: Boolean? = null,
    public val standardResource: Boolean? = null,
    public val cardResource: CardResource? = null,
    public val cardResourceHolder: Noun.Counted? = null,
    public val metricLocation: String? = null,
    public val tag: Tag? = null,
    public val track: Track? = null,
    public val placement: Placement? = null,
    public val placementSite: PlacementSite? = null,
    public val placementBonus: PlacementBonus? = null,
    public val spatialRelation: SpatialRelation? = null,
    /** Whether a structurally empty direct subclass adds no English text of its own. */
    public val textNeutralSubclasses: Boolean = false,
    /** Whether [directChange] interprets the declared behavior of concrete direct subclasses. */
    public val directChangeForSubclasses: Boolean = false,
    public val production: Boolean? = null,
    public val requirement: Requirement? = null,
    public val directChange: DirectChange? = null,
    public val draw: Boolean? = null,
    public val purchase: Purchase? = null,
    public val score: Score? = null,
    public val deadEndSignal: Boolean? = null,
    public val endTrigger: Boolean? = null,
    public val playTrigger: PlayTrigger? = null,
    public val playedCard: PlayedCard? = null,
    public val playedTagPhrase: String? = null,
    public val operationTrigger: String? = null,
    public val usedActionTrigger: Boolean? = null,
    public val actionNumber: Int? = null,
    public val actionUse: ActionUse? = null,
    public val spentResourceTrigger: Boolean? = null,
    public val paymentRole: PaymentRole? = null,
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
      public val forSubclasses: Boolean = true,
  )

  public data class PlacementBonus(public val noun: Noun.Counted)

  public data class SpatialRelation(
      public val phrase: String,
      public val defaultTarget: Noun.Counted? = null,
      public val countedPair: Boolean = false,
  ) {
    init {
      require((defaultTarget != null) != countedPair) {
        "A spatial relation must describe either an implicit target or a counted pair"
      }
    }
  }

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

  public sealed interface DirectChange {
    public data class Gain(public val noun: String, public val count: Int) : DirectChange

    /** Describes gaining one abstract component whose concrete subtype is chosen by the player. */
    public data class GainChoice(public val objectPhrase: String) : DirectChange

    /** Supplies an imperative construction for one otherwise-unmodeled concrete gain. */
    public data class Imperative(public val verb: String, public val objectPhrase: String) :
        DirectChange

    /** Describes a gained component that discounts the next card played. */
    public data object NextPlayedCardDiscount : DirectChange

    /** Describes copying the production box of the card selected by the gained expression. */
    public data object ProductionBoxCopy : DirectChange

    /** Describes a gained component whose one declared effect grants the player's first action. */
    public data object FirstAction : DirectChange

    /** Supplies the otherwise-unmodeled review, purchase, or discard procedure for the top card. */
    public data object TopCardPurchase : DirectChange
  }

  public data class Score(public val singular: String, public val plural: String)

  public data class Purchase(
      public val noun: Noun.Counted,
      public val destination: String? = null,
  )

  public data class ActionUse(
      public val objectPhrase: String,
      public val refundDiscountPredicate: String? = null,
      public val refundDiscountNoun: Noun.Counted? = null,
  )

  public data class PlayedCard(
      public val minimumProperties: Map<String, MinimumProperty> = emptyMap(),
  ) {
    public sealed interface MinimumProperty {
      public data class Threshold(public val noun: String, public val unit: String? = null) :
          MinimumProperty

      public data class Presence(public val noun: String) : MinimumProperty
    }
  }

  public enum class PlayTrigger {
    CARD,
    TAG,
  }

  public enum class PaymentRole {
    OWED,
    ACCEPTANCE,
    BARRIER,
  }
}
