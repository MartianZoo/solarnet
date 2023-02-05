package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.tfm.pets.Parsing.parseClassDeclarations
import dev.martianzoo.util.Debug

/**
 * Authoritative data source for officially published materials; should eventually be complete but
 * that will take a while.
 */
public object Canon : Authority() {

  private val PETS_FILENAMES = setOf("system.pets", "components.pets", "player.pets")

  override val explicitClassDeclarations: Collection<ClassDeclaration> by lazy {
    PETS_FILENAMES.flatMap {
      Debug.d("Reading $it")
      parseClassDeclarations(readResource(it))
    }
  }

  override val cardDefinitions: Collection<CardDefinition> by lazy {
    JsonReader.readCards(readResource("cards.json5"))
  }

  override val standardActionDefinitions: Collection<StandardActionDefinition> by lazy {
    JsonReader.readActions(readResource("actions.json5"))
  }

  override val marsMapDefinitions: Collection<MarsMapDefinition> by lazy {
    JsonReader.readMaps(readResource("maps.json5"))
  }

  override val milestoneDefinitions: Collection<MilestoneDefinition> by lazy {
    JsonReader.readMilestones(readResource("milestones.json5"))
  }

  override val customInstructions: List<CustomInstruction> by ::allCustomInstructions

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }

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
