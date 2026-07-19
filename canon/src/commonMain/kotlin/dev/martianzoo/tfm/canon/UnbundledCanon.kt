package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.tfm.api.TfmRuleset

/** Canon content that has not yet moved into its owning bundle rulesets. */
internal object UnbundledCanon : TfmRuleset.Empty() {
  override val customClasses: Set<CustomClass> by ::canonCustomClasses
}
