package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

private val canonBundles: Array<TfmCatalog> =
    CanonResources.bundleNames
        .map { name -> StandardFormBundle(name, primaryModuleName = primaryModuleName(name)) }
        .toTypedArray()

private fun primaryModuleName(bundleName: String): ClassName? =
    when (bundleName) {
      "base" -> cn("TerraformingMars")
      "corpera" -> cn("CorporateEraExpansion")
      "prelude" -> cn("PreludeExpansion")
      "venus" -> cn("VenusNextExpansion")
      "colonies" -> cn("ColoniesExpansion")
      "turmoil" -> cn("TurmoilExpansion")
      else -> null
    }

/** Terraforming Mars Catalog assembled from its declarative resource directories. */
public object Canon : TfmCatalog(*canonBundles)
