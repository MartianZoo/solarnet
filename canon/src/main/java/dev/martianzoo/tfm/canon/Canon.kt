package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.CustomClass
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.tfm.pets.Parsing.parseClasses
import dev.martianzoo.util.toSetStrict

/**
 * Authoritative data source for officially published materials; should eventually be complete but
 * that will take a while.
 */
public object Canon : Authority() {
  private val PETS_FILENAMES = setOf(
      "system.pets",
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

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }

  public val SIMPLE_GAME = GameSetup(this, "BM", 2)

  public enum class Bundle(val id: String) {
    Base("B"),
    CorporateEra("R"), // well the letter R appears 3 times so...
    Tharsis("M"), // M for "map", ooh
    Hellas("H"),
    Elysium("E"),
    VenusNext("V"),
    Prelude("P"),
    Colonies("C"),
    Turmoil("T"),
    Promos("X"),
    ;

    companion object {
      public fun forId(id: String) = values().first { it.id == id }
    }
  }
}
