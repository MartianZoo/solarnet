package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass

/** The promotional cards currently supported by Canon. */
internal object PromosExpansion :
    CanonicalBundle(name = "PromosExpansion", legacyCode = "X", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(CopyPrelude)
}
