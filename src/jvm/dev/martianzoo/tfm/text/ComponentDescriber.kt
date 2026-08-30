package dev.martianzoo.tfm.text

/** Sparse English-language facts declared for one component Class. */
internal data class ComponentDescriber(
    internal val noun: Noun? = null,
    internal val numericSingularChange: Boolean? = null,
    internal val changeFrame: ChangeFrame? = null,
    internal val cardResourceHolder: Noun.Counted? = null,
    internal val metricLocation: String? = null,
    internal val placementSite: PlacementSite? = null,
    internal val placementBonus: PlacementBonus? = null,
    internal val spatialRelation: SpatialRelation? = null,
    internal val productionSelection: String? = null,
    internal val requirement: Requirement? = null,
    internal val score: Score? = null,
    internal val deadEndSignal: Boolean? = null,
    internal val triggerFrame: TriggerFrame? = null,
    internal val presenceCondition: String? = null,
    internal val actionUse: ActionUse? = null,
    internal val paymentRole: PaymentRole? = null,
    internal val implicitPaymentResource: Noun? = null,
    internal val requirementShortfall: Boolean? = null,
    internal val requirementKind: String? = null,
    internal val distinctKinds: Noun.Counted? = null,
    internal val countNoun: Noun.Counted? = null,
    internal val metricCount: MetricCount? = null,
) {
  internal sealed interface Noun {
    public data object ClassName : Noun

    public data class Fixed(public val text: String) : Noun

    public data class Counted(public val singular: String, public val plural: String) : Noun
  }

  /** The English construction used to describe a change to this component. */
  internal sealed interface ChangeFrame {
    public data object Countable : ChangeFrame

    public data object Held : ChangeFrame

    public data class Scale(public val subject: String) : ChangeFrame

    public data class Positioned(
        internal val article: String,
        internal val singular: String,
        internal val plural: String,
        internal val referenceNoun: Noun.Counted? = null,
        internal val unqualifiedOwnership: OwnershipPhrase? = null,
        internal val anyoneOwnership: OwnershipPhrase? = null,
    ) : ChangeFrame

    public data object Deck : ChangeFrame

    public data class Procedure(
        internal val verb: String,
        internal val objectPhrase: String? = null,
    ) : ChangeFrame

    public data class Wrapper(public val preface: String) : ChangeFrame

    public data object Play : ChangeFrame
  }

  internal data class PlacementSite(
      internal val noun: Noun,
      internal val article: String? = null,
      internal val forSubclasses: Boolean = true,
  )

  internal data class PlacementBonus(internal val noun: Noun.Counted)

  internal data class MetricCount(
      internal val noun: Noun.Counted,
      internal val unqualifiedSuffix: String? = null,
      internal val anyoneSuffix: String? = null,
  )

  internal data class SpatialRelation(
      internal val phrase: String,
      internal val defaultTarget: Noun.Counted? = null,
      internal val countedPair: Boolean = false,
      internal val eventNoun: String? = null,
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

  internal enum class OwnershipPhrase {
    IMPLICIT,
    YOURS,
    ANYONES,
  }

  internal data class Requirement(
      internal val minimum: Bound? = null,
      internal val maximum: Bound? = null,
      internal val ownedCount: Noun? = null,
  ) {
    internal sealed interface Bound {
      public data class Threshold(
          internal val subject: String,
          internal val value: Value = Value.PLAIN,
      ) : Bound

      public data class Count(public val noun: Noun.Counted) : Bound
    }

    internal enum class Value {
      PLAIN,
      PERCENT,
      DOUBLE_PERCENT,
      TEMPERATURE,
    }
  }

  internal data class Score(internal val singular: String, internal val plural: String)

  /** The recurring event construction associated with this component Class. */
  internal sealed interface TriggerFrame {
    public data class PlayCard(
        internal val minimumProperties: Map<String, MinimumProperty> = emptyMap(),
    ) : TriggerFrame

    public data class PlayTag(internal val phrase: String? = null) : TriggerFrame

    public data object UseAction : TriggerFrame

    public data class Purchase(internal val noun: Noun.Counted) : TriggerFrame

    public data object SpendResource : TriggerFrame
  }

  internal data class ActionUse(
      internal val objectPhrase: String,
      internal val refundDiscountPredicate: String? = null,
      internal val refundDiscountNoun: Noun.Counted? = null,
      internal val minimumProperties: Map<String, MinimumProperty.Threshold> = emptyMap(),
  )

  internal sealed interface MinimumProperty {
    public data class Threshold(
        internal val noun: String,
        internal val unit: String? = null,
        internal val positiveObjectPhrase: String? = null,
    ) : MinimumProperty

    public data class Presence(public val noun: String) : MinimumProperty
  }

  internal enum class PaymentRole {
    OWED,
    ACCEPTANCE,
    BARRIER,
  }
}
