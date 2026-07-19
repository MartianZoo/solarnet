package dev.martianzoo.tfm.canon.bundles.PromosExpansion

import dev.martianzoo.api.CustomClass
import dev.martianzoo.tfm.canon.bundles.CanonicalBundle

/** The promotional cards currently supported by Canon. */
internal object PromosExpansion :
    CanonicalBundle(name = "PromosExpansion", legacyCode = "X", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(CopyPrelude)
}
