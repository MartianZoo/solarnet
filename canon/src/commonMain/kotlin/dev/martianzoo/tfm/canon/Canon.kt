package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.TfmAuthority

/** Published Terraforming Mars Authority with typed Terraforming Mars definition registries. */
public object Canon :
    TfmAuthority.Composite(
        StandardFormBundle(
            "TerraformingMars",
            baseCustomClasses,
        ),
        StandardFormBundle(
            "CorporateEraExpansion",
            corporateEraCustomClasses,
        ),
        StandardFormBundle("TharsisMap"),
        StandardFormBundle("HellasElysiumExpansion"),
        StandardFormBundle("UtopiaCimmeriaExpansion"),
        StandardFormBundle("VenusNextExpansion"),
        milestonesAwardsExpansionBundle,
        StandardFormBundle(
            "PreludeExpansion",
            preludeCustomClasses,
        ),
        StandardFormBundle("Prelude2Expansion"),
        StandardFormBundle(
            "ColoniesExpansion",
            coloniesCustomClasses,
        ),
        StandardFormBundle("TurmoilCardPack"),
        StandardFormBundle(
            "PromoCardPack",
            promoCardPackCustomClasses,
        ),
    )
