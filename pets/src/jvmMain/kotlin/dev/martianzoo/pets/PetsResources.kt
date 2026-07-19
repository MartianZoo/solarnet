package dev.martianzoo.pets

internal actual fun readPetsResource(filename: String): String {
  val resourceName = "/pets/$filename"
  return Parsing::class.java.getResource(resourceName)?.readText()
      ?: error("Unknown Pets resource: $filename")
}
