package dev.martianzoo.tfm.petaform.api

val DEFAULT_EXPRESSION = Expression("Megacredit")

fun namePattern(): Regex {
  val NAME_PATTERN by lazy { Regex("^[A-Z][a-z][A-Za-z0-9_]*$") }
  return NAME_PATTERN
}
