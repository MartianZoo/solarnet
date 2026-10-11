package dev.martianzoo.tfm.canon

private val canonBundles: Array<TfmCatalog> =
    CanonResources.bundleNames.map(::StandardFormBundle).toTypedArray()

/**
 * Catalog assembled from every bundle in the generated Canon resource registry. Use a smaller
 * [TfmCatalog] when only selected authored sources should be available for configuration.
 */
public object Canon : TfmCatalog(*canonBundles)
