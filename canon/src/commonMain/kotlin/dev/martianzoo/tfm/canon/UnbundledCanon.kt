package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.util.toSetStrict

/** Canon content that has not yet moved into its owning bundle rulesets. */
internal object UnbundledCanon : TfmRuleset() {
  private val PETS_FILENAMES =
      setOf(
          "global.pets",
          "maps-tiles.pets",
          "player.pets",
          "cards.pets",
          "actions.pets",
          "payment.pets",
          "colonies.pets",
      )

  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    PETS_FILENAMES.flatMap { parseClasses(readResource(it)) }.toSetStrict()
  }

  override val cardDefinitions: Set<CardDefinition> by lazy {
    JsonReader.readCards(readResource("cards.json5")).toSetStrict(::CardDefinition)
  }

  override val standardActionDefinitions: Set<StandardActionDefinition> by lazy {
    JsonReader.readActions(readResource("actions.json5")).toSetStrict()
  }

  override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
    JsonReader.readMaps(readResource("maps.json5")).toSetStrict()
  }

  override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
    JsonReader.readMilestones(readResource("milestones.json5")).toSetStrict()
  }

  override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
    JsonReader.readColonyTiles(readResource("colonies.json5")).toSetStrict(::ColonyTileDefinition)
  }

  override val customClasses: Set<CustomClass> by ::canonCustomClasses

  private fun readResource(filename: String): String = CanonResources.read(filename)
}
