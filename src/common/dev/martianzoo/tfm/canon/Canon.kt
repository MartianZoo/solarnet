package dev.martianzoo.tfm.canon

/** Terraforming Mars Catalog with typed Terraforming Mars definition registries. */
public object Canon :
    TfmCatalog.Composite(
        terraformingMarsBundle, // 2016
        tharsisMapBundle, // 2016
        hellasMapBundle, // 2017
        elysiumMapBundle, // 2017
        venusNextExpansionBundle, // 2017
        preludeExpansionBundle, // 2018
        coloniesExpansionBundle, // 2018
        turmoilCardPackBundle, // 2019
        prelude2ExpansionBundle, // 2024
        milestonesAwardsExpansionBundle, // 2024
        amazonisMapBundle, // 2024
        vastitasMapBundle, // 2024
        utopiaMapBundle, // 2024
        cimmeriaMapBundle, // 2024
        promoCardPackBundle,
    )
