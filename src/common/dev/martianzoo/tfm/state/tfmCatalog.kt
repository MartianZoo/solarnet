package dev.martianzoo.tfm.state

import dev.martianzoo.state.GameReader
import dev.martianzoo.tfm.canon.TfmCatalog

/** The Terraforming Mars Catalog used by this game. */
public val GameReader.tfmCatalog: TfmCatalog
  get() = catalog as TfmCatalog
