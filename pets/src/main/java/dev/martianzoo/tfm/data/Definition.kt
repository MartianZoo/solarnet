package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.ClassName

interface Definition {
  val id: ClassName
  val name: ClassName
  val bundle: String
  val asClassDeclaration: ClassDeclaration
}
