package dev.martianzoo.tfm.canon

import dev.martianzoo.api.GameReader

/** The Terraforming Mars Authority used by this game. */
public val GameReader.tfmAuthority: TfmAuthority
  get() = authority as TfmAuthority
