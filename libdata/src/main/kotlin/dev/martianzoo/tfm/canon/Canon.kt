package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.Card
import dev.martianzoo.tfm.data.RawComponentType
import dev.martianzoo.tfm.data.MarsArea
import dev.martianzoo.tfm.data.MoshiReader
import dev.martianzoo.util.Grid

object Canon {
  val componentTypeData: Map<String, RawComponentType> by lazy {
    MoshiReader.readComponentTypes(readResource("components.json5"))
  }

  val cardData: Map<String, Card> by lazy {
    MoshiReader.readCards(readResource("cards.json5"))
  }

  val mapData: Map<String, Grid<MarsArea>> by lazy {
    MoshiReader.readMaps(readResource("maps.json5"))
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
