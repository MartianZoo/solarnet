package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.ClassDeclaration
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
  override val explicitClassDeclarations = emptySet<ClassDeclaration>()

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

  override val colonyTileDefinitions = emptySet<ColonyTileDefinition>()

  override val customClasses: Set<CustomClass> by ::canonCustomClasses

  private fun readResource(filename: String): String = CanonResources.read(filename)
}
