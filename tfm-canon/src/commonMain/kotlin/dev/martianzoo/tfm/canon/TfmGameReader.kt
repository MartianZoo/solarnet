package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.GameReader

/** The Terraforming Mars Catalog used by this game. */
public val GameReader.tfmCatalog: TfmCatalog
  get() = catalog as TfmCatalog
