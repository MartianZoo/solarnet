package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ComponentDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.JsonReader.combine
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.Component
import dev.martianzoo.tfm.pets.PetsParser.parseComponents
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByCareful

object Canon {
  private val FILENAMES = setOf("system.pets", "components.pets", "payment.pets")

  val componentDefinitions: Map<String, ComponentDefinition> by lazy {
    val list = mutableListOf<Component>()
    for (f in FILENAMES) {
      val s = readResource(f)
      println("Parsing $f")
      list += parseComponents(s)
    }
    list.map(ComponentDefinition.Companion::from).associateByCareful { it.name }
  }

  val cardDefinitions: Map<String, CardDefinition> by lazy {
    JsonReader.readCards(readResource("cards.json5"))
  }

  val milestoneDefinitions: Map<String, MilestoneDefinition> by lazy {
    JsonReader.readMilestones(readResource("milestones.json5"))
  }

  val auxiliaryComponentDefinitions: Map<String, ComponentDefinition> by lazy {
    JsonReader.auxiliaryComponentDefinitions(cardDefinitions.values)
  }

  val mapAreaDefinitions: Map<String, Grid<MarsAreaDefinition>> by lazy {
    JsonReader.readMaps(readResource("maps.json5"))
  }

  // You wouldn't normally use this, but have only a single map in play.
  val allDefinitions: Map<String, ComponentDefinition> by lazy {
    combine(
        componentDefinitions.values,
        cardDefinitions.values,
        milestoneDefinitions.values,
        auxiliaryComponentDefinitions.values,
        mapAreaDefinitions.values.flatten())
  }

  private fun readResource(filename: String): String {
    println("reading filename $filename")
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
