package dev.martianzoo.tfm.pets.ast

interface ProductionBox<P : PetsNode> {
  fun extract(): P
}
