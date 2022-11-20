package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.Component
import dev.martianzoo.tfm.data.MoshiReader

object Canon {
  val componentClassData: Map<String, Component> by lazy {
    val json5 = readResource("components.json5")
    MoshiReader.readComponents(json5)
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
