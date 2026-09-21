package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.tfm.canon.milestonesawardsexpansion.customClasses as milestonesAwardsCustomClasses
import dev.martianzoo.tfm.canon.promocardpack.customClasses as promoCardPackCustomClasses
import dev.martianzoo.tfm.canon.terraformingmars.customClasses as terraformingMarsCustomClasses
import dev.martianzoo.tfm.canon.vastitasmap.customClasses as vastitasMapCustomClasses

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
