package dev.martianzoo.tfm.pets

enum class PetFeature {
  /** The element might include instructions like `2 OxygenStep` that need to be split up. */
  ATOMIZE,

  /**
   * The element might contain expressions and [Change] instructions that expect to have defaults
   * filled in, like `GreeneryTile` which should be translated to `GreeneryTile<Owner, LandArea(HAS?
   * Neighbor<OwnedTile<Owner>>)>!`
   */
  DEFAULTS,

  /** The element might contain PROD blocks. (Idempotent, invertible) */
  PROD_BLOCKS,

  /** The element should undergo dependency-specialization. */
  DEPENDENCY_SPEC,

  /** This element might use short names for classes. (Idempotent, invertible) */
  SHORT_NAMES,

  /** The element might contain `This` in expressions (triggers are okay). */
  THIS_EXPRESSIONS,

  /** This is an [Effect] which should undergo trigger-specialization. */
  TRIGGER_SPEC,
  ;

  companion object {
    val STANDARD_FEATURES: Set<PetFeature> = setOf(ATOMIZE, DEFAULTS, PROD_BLOCKS)
    val ALL_FEATURES: Set<PetFeature> =
        STANDARD_FEATURES + DEPENDENCY_SPEC + SHORT_NAMES + THIS_EXPRESSIONS + TRIGGER_SPEC
  }
}
