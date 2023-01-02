package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ComponentDeclaration

interface Definition {
  val toComponentDeclaration: ComponentDeclaration
}
