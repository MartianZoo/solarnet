package dev.martianzoo.tfm.state

import dev.martianzoo.state.GameReader
import dev.martianzoo.tfm.canon.TfmCatalog

/**
 * The [TfmCatalog] supplied by this reader's game. Requires the underlying catalog to be a
 * [TfmCatalog]; the cast fails for a game built with another catalog type.
 */
public val GameReader.tfmCatalog: TfmCatalog
  get() = catalog as TfmCatalog
