package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass

/** The Prelude expansion rules currently supported by Canon. */
internal object PreludeExpansion :
    CanonicalBundle(name = "PreludeExpansion", legacyCode = "P", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(GainLowestProduction)
}
