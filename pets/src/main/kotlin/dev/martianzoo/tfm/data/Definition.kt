package dev.martianzoo.tfm.data

interface Definition {
  val className: String
  val bundle: String
  val asClassDeclaration: ClassDeclaration
}
