package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.util.toSetStrict

/**
 * Authoritative data source for officially published materials; should eventually be complete but
 * that will take a while.
 */
public object Canon : TfmAuthority() {
  private val PETS_FILENAMES =
      listOf(
          "system.pets",
          "global.pets",
          "player.pets",
          "bundles/TerraformingMars/maps-tiles.pets",
          "bundles/TerraformingMars/cards.pets",
          "bundles/TerraformingMars/actions.pets",
          "bundles/TerraformingMars/payment.pets",
          "bundles/ColoniesExpansion/colonies.pets",
      )

  private val CARD_FILENAMES =
      listOf(
          "bundles/TerraformingMars/cards.json5",
          "bundles/CorporateEraExpansion/cards.json5",
          "bundles/VenusNextExpansion/cards.json5",
          "bundles/PreludeExpansion/cards.json5",
          "bundles/ColoniesExpansion/cards.json5",
          "bundles/TurmoilExpansion/cards.json5",
          "bundles/PromoCardsBundle/cards.json5",
      )

  private val ACTION_FILENAMES =
      listOf(
          "bundles/TerraformingMars/actions.json5",
          "bundles/VenusNextExpansion/actions.json5",
          "bundles/ColoniesExpansion/actions.json5",
      )

  private val MAP_FILENAMES =
      listOf(
          "bundles/TharsisMap/maps.json5",
          "bundles/HellasMap/maps.json5",
          "bundles/ElysiumMap/maps.json5",
      )

  private val MILESTONE_FILENAMES =
      listOf(
          "bundles/TharsisMap/milestones.json5",
          "bundles/HellasMap/milestones.json5",
          "bundles/ElysiumMap/milestones.json5",
          "bundles/VenusNextExpansion/milestones.json5",
      )

  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    PETS_FILENAMES.flatMap { parseClasses(readResource(it)) }.toSetStrict()
  }

  override val cardDefinitions: Set<CardDefinition> by lazy {
    readResources(CARD_FILENAMES, JsonReader::readCards).toSetStrict(::CardDefinition)
  }

  override val standardActionDefinitions: Set<StandardActionDefinition> by lazy {
    readResources(ACTION_FILENAMES, JsonReader::readActions).toSetStrict()
  }

  override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
    readResources(MAP_FILENAMES, JsonReader::readMaps).toSetStrict()
  }

  override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
    readResources(MILESTONE_FILENAMES, JsonReader::readMilestones).toSetStrict()
  }

  override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
    JsonReader.readColonyTiles(readResource("bundles/ColoniesExpansion/colonies.json5"))
        .toSetStrict(::ColonyTileDefinition)
  }

  override val customClasses: Set<CustomClass> by ::canonCustomClasses

  private fun readResource(filename: String): String = CanonResources.read(filename)

  private fun <T> readResources(
      filenames: List<String>,
      reader: (String) -> List<T>,
  ): List<T> = filenames.flatMap { reader(readResource(it)) }

  public val SIMPLE_GAME = GameSetup(this, "BM", 2)
}
