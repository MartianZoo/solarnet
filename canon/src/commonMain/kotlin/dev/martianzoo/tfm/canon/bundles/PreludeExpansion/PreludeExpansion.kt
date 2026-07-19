package dev.martianzoo.tfm.canon.bundles.PreludeExpansion

import dev.martianzoo.api.CustomClass
import dev.martianzoo.tfm.canon.bundles.CanonicalBundle

/** The Prelude expansion rules currently supported by Canon. */
internal object PreludeExpansion :
    CanonicalBundle(name = "PreludeExpansion", legacyCode = "P", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(GainLowestProduction)
}
