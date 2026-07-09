package dev.martianzoo.tfm.canon

internal actual fun readCanonResource(filename: String): String {
  val resourceName = "/dev/martianzoo/tfm/canon/$filename"
  return CanonResources::class.java.getResource(resourceName)?.readText()
      ?: error("Unknown canon resource: $filename")
}
