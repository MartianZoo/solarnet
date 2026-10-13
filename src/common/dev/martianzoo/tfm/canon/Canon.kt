package dev.martianzoo.tfm.canon

private val canonBundles: Array<TfmCatalog> =
    CanonResources.bundleModules
        .map { (name, primaryModule) -> StandardFormBundle(name, primaryModule) }
        .toTypedArray()

/**
 * Catalog assembled from the bundles listed in `bundles/manifest.json5`. Use a smaller [TfmCatalog]
 * when only selected authored sources should be available for configuration.
 */
public object Canon : TfmCatalog(*canonBundles)
