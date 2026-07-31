package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.TfmRuleset

internal object CanonSetupRuleset : TfmRuleset.Empty() {
  override val explicitClassDeclarations by lazy {
    Canon.bundles.flatMapTo(linkedSetOf()) { it.setupClassDeclarations }
  }
}
