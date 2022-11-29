package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CTypeDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.MoshiReader
import dev.martianzoo.util.Grid

object Canon {
  val cTypeDefinitions: Map<String, CTypeDefinition> by lazy {
    MoshiReader.readComponentTypes(readResource("components.json5"))
  }

  val cardDefinitions: Map<String, CardDefinition> by lazy {
    MoshiReader.readCards(readResource("cards.json5"))
  }

  val mapAreaDefinitions: Map<String, Grid<MarsAreaDefinition>> by lazy {
    MoshiReader.readMaps(readResource("maps.json5"))
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
