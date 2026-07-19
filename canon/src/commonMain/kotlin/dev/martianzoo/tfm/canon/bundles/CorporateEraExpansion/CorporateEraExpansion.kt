package dev.martianzoo.tfm.canon.bundles.CorporateEraExpansion

import dev.martianzoo.api.CustomClass
import dev.martianzoo.tfm.canon.bundles.CanonicalBundle

/** The Corporate Era card expansion. */
internal object CorporateEraExpansion :
    CanonicalBundle(name = "CorporateEraExpansion", legacyCode = "R", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(CopyProductionBox)
}
