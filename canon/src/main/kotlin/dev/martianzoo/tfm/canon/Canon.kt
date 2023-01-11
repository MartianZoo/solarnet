package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.data.ActionDefinition
import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.ClassDeclarationParser
import dev.martianzoo.util.Debug
import dev.martianzoo.util.Grid

object Canon : Authority() {

  private val PETS_FILENAMES = setOf("system.pets", "components.pets", "player.pets")

  override val explicitClassDeclarations: Collection<ClassDeclaration> by lazy {
    PETS_FILENAMES.flatMap {
      Debug.d("Reading $it")
      ClassDeclarationParser.parseClassDeclarations(readResource(it))
    }
  }

  override val mapAreaDefinitions: Map<String, Grid<MapAreaDefinition>> by lazy {
    JsonReader.readMaps(readResource("maps.json5"))
  }

  fun getMap(bundle: Bundle): Grid<MapAreaDefinition> =
      mapAreaDefinitions[bundle.name] ?: error("not a map: $bundle")

  override val actionDefinitions: Collection<ActionDefinition> by lazy {
    JsonReader.readActions(readResource("actions.json5"))
  }

  override val cardDefinitions: Collection<CardDefinition> by lazy {
    JsonReader.readCards(readResource("cards.json5"))
  }

  override val milestoneDefinitions: Collection<MilestoneDefinition> by lazy {
    JsonReader.readMilestones(readResource("milestones.json5"))
  }

  override fun customInstructions(): List<CustomInstruction> = allCustomInstructions

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }

  enum class Bundle(val id: String, val isMap: Boolean = false) {
    Base("B"),
    CorporateEra("R"),  // well the letter R appears 3 times so...
    Tharsis("M", true),  // M for "map", ooh
    Hellas("H", true),
    Elysium("E", true),
    VenusNext("V"),
    Prelude("P"),
    Colonies("C"),
    Turmoil("T"),
    Promos("X"),
  }
}
