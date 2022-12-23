package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.Parser.parseComponents
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

object Canon {
  private val FILENAMES = setOf("system.pets", "components.pets", "player.pets")

  val componentDefinitions: Map<String, ComponentDef> by lazy {
    val list = mutableListOf<ComponentDef>()
    for (f in FILENAMES) {
      val s = readResource(f)
      println("Parsing $f")
      list += parseComponents(s)
    }
    list.associateByStrict { it.name }
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
      defs.flatMap { it }.associateByStrict { it.name }

  private fun readResource(filename: String): String {
    println("reading filename $filename")
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
