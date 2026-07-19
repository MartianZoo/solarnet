package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.bundles.ColoniesExpansion.ColoniesExpansion
import dev.martianzoo.tfm.canon.bundles.CorporateEraExpansion.CorporateEraExpansion
import dev.martianzoo.tfm.canon.bundles.ElysiumMap.ElysiumMap
import dev.martianzoo.tfm.canon.bundles.HellasMap.HellasMap
import dev.martianzoo.tfm.canon.bundles.PreludeExpansion.PreludeExpansion
import dev.martianzoo.tfm.canon.bundles.PromosExpansion.PromosExpansion
import dev.martianzoo.tfm.canon.bundles.SoloMode.SoloMode
import dev.martianzoo.tfm.canon.bundles.System.System
import dev.martianzoo.tfm.canon.bundles.TerraformingMars.TerraformingMars
import dev.martianzoo.tfm.canon.bundles.TharsisMap.TharsisMap
import dev.martianzoo.tfm.canon.bundles.TurmoilExpansion.TurmoilExpansion
import dev.martianzoo.tfm.canon.bundles.VenusNextExpansion.VenusNextExpansion
import dev.martianzoo.tfm.data.GameSetup

/** The composition of all rulesets containing officially published materials. */
public object Canon :
    TfmRuleset.Composite(
        System,
        TerraformingMars,
        CorporateEraExpansion,
        TharsisMap,
        HellasMap,
        ElysiumMap,
        VenusNextExpansion,
        PreludeExpansion,
        ColoniesExpansion,
        SoloMode,
        PromosExpansion,
        TurmoilExpansion,
    ) {
  public val SIMPLE_GAME: GameSetup = GameSetup(this, "BM", 2)
}
