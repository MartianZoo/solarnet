package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ComponentClassDefinition
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.MoshiReader
import dev.martianzoo.util.Grid

object Canon {
  val componentClassDefinitions: Map<String, ComponentClassDefinition> by lazy {
    MoshiReader.readComponentTypes(readResource("components.json5"))
  }

  val cardDefinitions: Map<String, CardDefinition> by lazy {
    MoshiReader.readCards(readResource("cards.json5"))
  }

  val mapAreaDefinitions: Map<String, Grid<MarsAreaDefinition>> by lazy {
    MoshiReader.readMaps(readResource("maps.json5"))
  }

  // You wouldn't normally use this, but have only a single map in play.
  val allDefinitions: Map<String, ComponentClassDefinition> by lazy {
    val ct = componentClassDefinitions.values
    val cards = cardDefinitions.values
    val areas =  mapAreaDefinitions.values.flatMap { it }
    val expected = ct.size + cards.size + areas.size
    (ct + cards + areas).map { it.asComponentClassDefinition }.associateBy { it.name }.also {
      require(it.size == expected)
    }
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
