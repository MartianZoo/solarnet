package dev.martianzoo.tfm.canon

/** Published Terraforming Mars Authority with typed Terraforming Mars definition registries. */
public object Canon :
    TfmAuthority.Composite(
        terraformingMarsBundle, // 2016
        hellasElysiumExpansionBundle, // 2017
        venusNextExpansionBundle, // 2017
        preludeExpansionBundle, // 2018
        coloniesExpansionBundle, // 2018
        turmoilCardPackBundle, // 2019
        prelude2ExpansionBundle, // 2024
        milestonesAwardsExpansionBundle, // 2024
        utopiaCimmeriaExpansionBundle, // 2024
        promoCardPackBundle,
    )
