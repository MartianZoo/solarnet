package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.HasClassName

interface Definition : HasClassName {
  val id: ClassName
  override val className: ClassName
  val bundle: String
  val asClassDeclaration: ClassDeclaration
}
