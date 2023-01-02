package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ClassDeclaration

interface Definition {
  val toClassDeclaration: ClassDeclaration
}
