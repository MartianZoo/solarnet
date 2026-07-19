package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.TfmRuleset

/** The composition of all rulesets containing officially published materials. */
public object Canon :
    TfmRuleset.Composite(
        JsonBundle("TerraformingMars", "B", baseCustomClasses),
        JsonBundle("SoloMode", "S"),
        JsonBundle("CorporateEraExpansion", "R", corporateEraCustomClasses),
        JsonBundle("TharsisMap", "M"),
        JsonBundle("HellasMap", "H"),
        JsonBundle("ElysiumMap", "E"),
        JsonBundle("VenusNextExpansion", "V"),
        JsonBundle("PreludeExpansion", "P", preludeCustomClasses),
        JsonBundle("ColoniesExpansion", "C", coloniesCustomClasses),
        JsonBundle("TurmoilExpansion", "T"),
        JsonBundle("PromoCardsBundle", "X", promoCardsCustomClasses),
    )
