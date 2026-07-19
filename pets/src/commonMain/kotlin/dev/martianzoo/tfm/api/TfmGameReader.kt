package dev.martianzoo.tfm.api

import dev.martianzoo.api.GameReader

/** The resolved Terraforming Mars ruleset used by this game. */
public val GameReader.tfmRuleset: TfmRuleset
  get() = ruleset as TfmRuleset
