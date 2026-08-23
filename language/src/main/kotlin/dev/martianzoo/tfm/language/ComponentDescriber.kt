package dev.martianzoo.tfm.language

/** Sparse English-language facts declared for one component Class. */
public data class ComponentDescriber(
    public val noun: Noun? = null,
    public val changeFrame: ChangeFrame? = null,
    public val cardResourceHolder: Noun.Counted? = null,
    public val metricLocation: String? = null,
    public val placementSite: PlacementSite? = null,
    public val placementBonus: PlacementBonus? = null,
    public val spatialRelation: SpatialRelation? = null,
    public val productionSelection: String? = null,
    public val requirement: Requirement? = null,
    public val purchase: Purchase? = null,
    public val score: Score? = null,
    public val deadEndSignal: Boolean? = null,
    public val playTrigger: PlayTrigger? = null,
    public val playedCard: PlayedCard? = null,
    public val playedTagPhrase: String? = null,
    public val presenceCondition: String? = null,
    public val usedActionTrigger: Boolean? = null,
    public val actionUse: ActionUse? = null,
    public val spentResourceTrigger: Boolean? = null,
    public val paymentRole: PaymentRole? = null,
    public val implicitPaymentResource: Noun? = null,
    public val requirementShortfall: Boolean? = null,
    public val requirementKind: String? = null,
    public val distinctKinds: Noun.Counted? = null,
    public val countNoun: Noun.Counted? = null,
    public val metricCount: MetricCount? = null,
) {
  public sealed interface Noun {
    public data object ClassName : Noun

    public data class Fixed(public val text: String) : Noun

    public data class Counted(public val singular: String, public val plural: String) : Noun
  }

  /** The English construction used to describe a change to this component. */
  public sealed interface ChangeFrame {
    public data object Countable : ChangeFrame

    public data object Held : ChangeFrame

    public data class Scale(public val subject: String) : ChangeFrame

    public data class Positioned(
        public val article: String,
        public val singular: String,
        public val plural: String,
        public val unqualifiedMetricOwner: MetricOwner? = null,
        public val anyoneMetricOwner: MetricOwner? = null,
    ) : ChangeFrame

    public data object Deck : ChangeFrame

    public data class Procedure(
        public val verb: String,
        public val objectPhrase: String? = null,
    ) : ChangeFrame

    public data class Wrapper(public val preface: String) : ChangeFrame

    public data object Play : ChangeFrame
  }

  public data class PlacementSite(
      public val noun: Noun,
      public val article: String? = null,
      public val forSubclasses: Boolean = true,
  )

  public data class PlacementBonus(public val noun: Noun.Counted)

  public data class MetricCount(
      public val noun: Noun.Counted,
      public val unqualifiedSuffix: String? = null,
      public val anyoneSuffix: String? = null,
  )

  public data class SpatialRelation(
      public val phrase: String,
      public val defaultTarget: Noun.Counted? = null,
      public val countedPair: Boolean = false,
      public val eventNoun: String? = null,
  ) {
    init {
      require((defaultTarget != null) != countedPair) {
        "A spatial relation must describe either an implicit target or a counted pair"
      }
      require(eventNoun == null || countedPair) {
        "Only a relation between two explicit participants can describe its creation"
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
      ) : Bound

      public data class Count(public val noun: Noun.Counted) : Bound
    }

    public enum class Value {
      PLAIN,
      PERCENT,
      DOUBLE_PERCENT,
      TEMPERATURE,
    }
  }

  public data class Score(public val singular: String, public val plural: String)

  public data class Purchase(public val noun: Noun.Counted)

  public data class ActionUse(
      public val objectPhrase: String,
      public val refundDiscountPredicate: String? = null,
      public val refundDiscountNoun: Noun.Counted? = null,
      public val minimumProperties: Map<String, MinimumProperty.Threshold> = emptyMap(),
  )

  public data class PlayedCard(
      public val minimumProperties: Map<String, MinimumProperty> = emptyMap(),
  )

  public sealed interface MinimumProperty {
    public data class Threshold(public val noun: String, public val unit: String? = null) :
        MinimumProperty

    public data class Presence(public val noun: String) : MinimumProperty
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
