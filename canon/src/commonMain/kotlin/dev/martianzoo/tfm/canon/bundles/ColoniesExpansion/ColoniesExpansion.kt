package dev.martianzoo.tfm.canon.bundles.ColoniesExpansion

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.CanonResources
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.util.toSetStrict

/** The Colonies expansion rules currently supported by Canon. */
internal object ColoniesExpansion : TfmRuleset.Empty() {
  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    parseClasses(readResource("colonies.pets")).toSetStrict()
  }

  override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
    JsonReader.readColonyTiles(readResource("colonies.json5")).toSetStrict(::ColonyTileDefinition)
  }

  override val customClasses: Set<CustomClass> = setOf(AddColonyTile)

  private fun readResource(filename: String): String =
      CanonResources.read("bundles/ColoniesExpansion/$filename")
}
