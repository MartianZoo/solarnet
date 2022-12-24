package dev.martianzoo.tfm.types

data class DependencyKey(val declaringClass: PetClass, val index: Int, val classDep: Boolean = false) {
  init {
    require(index >= 0)
  }

  override fun toString(): String {
    val desc = "${declaringClass.name}_$index"
    return (KEY_TO_NAME[desc] ?: desc) + (if (classDep) "[C]" else "")
  }

  // REALLY STUPID HACK just to make things easier on myself while in the thick of early debugging.
  // We do not want to ACTUALLY name dependencies.
  val KEY_TO_NAME = mapOf(
      "Owned_0" to "owner",

      "Production_0" to "type",

      "Tile_0" to "on",
      "Adjacency_0" to "first",
      "Adjacency_1" to "second",
      "Border_0" to "left",
      "Border_1" to "right",
      "Neighbor_0" to "tile",
      "Neighbor_1" to "area",

      "Cardbound_0" to "on",
      "Holder_0" to "held",

      "PlayCard_0" to "back",
      "PlayCard_1" to "front",
      "PlayTag_0" to "type",

      "UseAction_0" to "from",

      "Accept_0" to "currency",
      "Owed_0" to "currency",
      "Pay_0" to "currency",
  )
}
