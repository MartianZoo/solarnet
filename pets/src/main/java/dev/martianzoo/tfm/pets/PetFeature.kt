package dev.martianzoo.tfm.pets

enum class PetFeature {
  /** This element might use short names for classes. Should be done early. */
  SHORT_NAMES,

  /** The element might contain `This` in expressions (triggers are okay). Context-dependent. */
  THIS_EXPRESSIONS,

  /**
   * The element might include instructions like `2 OxygenStep` that need to be split up.
   * THIS_EXPRESSIONS should ideally be handled first.
   */
  ATOMIZE,

  /**
   * The element might contain expressions and [Change] instructions that expect to have defaults
   * filled in, like `GreeneryTile` which should be translated to `GreeneryTile<Owner, LandArea(HAS?
   * Neighbor<OwnedTile<Owner>>)>!`
   *
   * Requires THIS_EXPRESSIONS to be handled first.
   */
  DEFAULTS,

  /** The element should undergo dependency-specialization... or trigger-specialization? */
  SPECIALIZABLE,

  /**
   * The element might contain PROD blocks. Idempotent, invertible, and AFAICT orthogonal to
   * everything else. Just makes stuff unreadable is all.
   */
  PROD_BLOCKS;

  companion object {
    val STANDARD_FEATURES: Set<PetFeature> = setOf(ATOMIZE, DEFAULTS, PROD_BLOCKS)
    val ALL_FEATURES: Set<PetFeature> =
        STANDARD_FEATURES + SPECIALIZABLE + SHORT_NAMES + THIS_EXPRESSIONS
  }
}
