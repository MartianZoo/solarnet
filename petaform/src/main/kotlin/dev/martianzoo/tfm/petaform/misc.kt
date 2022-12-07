package dev.martianzoo.tfm.petaform

val DEFAULT_EXPRESSION = Expression("Megacredit")

fun classNamePattern(): Regex {
  val CLASS_NAME_PATTERN by lazy { Regex("^[A-Z][a-z][A-Za-z0-9_]*$") }
  return CLASS_NAME_PATTERN
}
