package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass

private val canonCustomClasses: Set<CustomClass> =
    terraformingMarsCustomClasses +
        promoCardPackCustomClasses +
        milestonesAwardsCustomClasses +
        vastitasMapCustomClasses

private val canonBundles: Array<TfmCatalog> =
    CanonResources.bundleNames.map(::StandardFormBundle).toTypedArray()

/** Terraforming Mars Catalog assembled from its resource directories and custom implementations. */
public object Canon : TfmCatalog.Composite(*canonBundles) {
  override val customClasses: Set<CustomClass> = canonCustomClasses
}
