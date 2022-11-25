package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.Card
import dev.martianzoo.tfm.data.Component
import dev.martianzoo.tfm.data.MarsMap
import dev.martianzoo.tfm.data.MoshiReader

object Canon {
  val componentClassData: Map<String, Component> by lazy {
    MoshiReader.readComponents(readResource("components.json5"))
  }

  val cardData: Map<String, Card> by lazy {
    MoshiReader.readCards(readResource("cards.json5"))
  }

  val mapData: Map<String, MarsMap> by lazy {
    MoshiReader.readMaps(readResource("maps.json5")).associateBy { it.name }
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
