package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ComponentDefinition
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.JsonReader.combine
import dev.martianzoo.tfm.petaform.api.ComponentClassDeclaration
import dev.martianzoo.tfm.petaform.parser.PetaformParser
import dev.martianzoo.util.Grid

object Canon {
  val componentDefinitions: Map<String, ComponentDefinition> by lazy {
    PetaformParser.parseComponentClasses(readResource("components.pets"))
        .map { ComponentDefinition.from(it) }.associateBy { it.name }
  }

  val cardDefinitions: Map<String, CardDefinition> by lazy {
    JsonReader.readCards(readResource("cards.json5"))
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
        auxiliaryComponentDefinitions.values,
        mapAreaDefinitions.values.flatten())
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
