package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.PetsParser.parseComponents
import dev.martianzoo.tfm.pets.ast.Instruction.CustomInstruction
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

object Canon {
  private val FILENAMES = setOf("system.pets", "components.pets", "player.pets")

  val componentDefinitions: Map<String, ComponentDef> by lazy {
    FILENAMES.flatMap {
      println("Parsing $it")
      parseComponents(readResource(it))
    }.associateByStrict { it.className }
  }

  val cardDefinitions: Map<String, CardDefinition> by lazy {
    JsonReader.readCards(readResource("cards.json5"))
  }

  val auxiliaryComponentDefinitions: Map<String, ComponentDef> by lazy {
    JsonReader.auxiliaryComponentDefinitions(cardDefinitions.values)
  }

  val mapAreaDefinitions: Map<String, Grid<MarsAreaDefinition>> by lazy {
    JsonReader.readMaps(readResource("maps.json5"))
  }

  val milestoneDefinitions: Map<String, MilestoneDefinition> by lazy {
    JsonReader.readMilestones(readResource("milestones.json5"))
  }

  val allDefinitions: Map<String, ComponentDef> by lazy {
    combine(
        componentDefinitions.values,
        cardDefinitions.values.map { it.toComponentDef },
        auxiliaryComponentDefinitions.values,
        mapAreaDefinitions.values.flatten().map { it.toComponentDef },
        milestoneDefinitions.values.map { it.toComponentDef })
  }

  fun combine(vararg defs: Collection<ComponentDef>) =
      defs.flatMap { it }.associateByStrict { it.className }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }

  fun customInstruction(name: String): CustomInstruction {
    when (name) {
      //"createMarsAreas" -> {
      //  object : CustomInstruction {
      //    override val name = "createMarsAreas"
      //    override fun translate(game: GameApi, types: List<TypeExpression>): Instruction {
      //      return Instruction.Multi(
      //          mapAreaDefinitions.keys.filter {
      //            it.startsWith("Tharsis")
      //          }.map { Gain(te(it)) })
      //    }
      //  }
      //}
      "createBorders" -> TODO()
      "handleRequirement" -> TODO()
    }
    TODO()
  }

  enum class Bundle(val id: Char) {
    BASE('B'),
    CORPORATE_ERA('R'),  // well the letter R appears 3 times so...
    THARSIS('M'),        // for "map", ooh
    HELLAS('H'),
    ELYSIUM('E'),
    VENUS_NEXT('V'),
    PRELUDE('P'),
    COLONIES('C'),
    TURMOIL('T'),
    PROMOS('X'),
  }
}
