package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.ClassName

interface Definition {
  val className: ClassName
  val bundle: String
  val asClassDeclaration: ClassDeclaration
}
