package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.bundles.System.System
import dev.martianzoo.tfm.canon.bundles.TerraformingMars.TerraformingMars
import dev.martianzoo.tfm.data.GameSetup

/** The composition of all rulesets containing officially published materials. */
public object Canon : TfmRuleset.Composite(System, TerraformingMars, UnbundledCanon) {
  public val SIMPLE_GAME: GameSetup = GameSetup(this, "BM", 2)
}
