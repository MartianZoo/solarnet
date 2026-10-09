package dev.martianzoo.tfm.canon

private val canonBundles: Array<TfmCatalog> =
    CanonResources.bundleNames.map(::StandardFormBundle).toTypedArray()

/** Terraforming Mars Catalog assembled from its declarative resource directories. */
public object Canon : TfmCatalog(*canonBundles)
