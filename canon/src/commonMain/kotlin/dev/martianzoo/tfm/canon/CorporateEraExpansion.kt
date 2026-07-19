package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass

/** The Corporate Era card expansion. */
internal object CorporateEraExpansion :
    CanonicalBundle(name = "CorporateEraExpansion", legacyCode = "R", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(CopyProductionBox)
}
