package dev.martianzoo.tfm.types

interface PetClassTable {
  fun get(name: String): PetClass
  fun all(): Set<PetClass>
}
