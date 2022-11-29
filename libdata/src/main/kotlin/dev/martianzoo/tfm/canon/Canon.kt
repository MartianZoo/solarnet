package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardData
import dev.martianzoo.tfm.data.CTypeData
import dev.martianzoo.tfm.data.MarsAreaData
import dev.martianzoo.tfm.data.MoshiReader
import dev.martianzoo.util.Grid

object Canon {
  val componentTypeData: Map<String, CTypeData> by lazy {
    MoshiReader.readComponentTypes(readResource("components.json5"))
  }

  val cardData: Map<String, CardData> by lazy {
    MoshiReader.readCards(readResource("cards.json5"))
  }

  val mapData: Map<String, Grid<MarsAreaData>> by lazy {
    MoshiReader.readMaps(readResource("maps.json5"))
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
