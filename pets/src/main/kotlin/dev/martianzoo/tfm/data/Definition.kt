package dev.martianzoo.tfm.data

interface Definition {
  val componentName: String
  val asClassDeclaration: ClassDeclaration
}
