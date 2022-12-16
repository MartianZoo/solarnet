package dev.martianzoo.tfm.pets

interface ProductionBox<P : PetsNode> {
  fun extract(): P
}
