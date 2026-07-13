package dev.martianzoo.tfm.canon

internal object CanonResources {
  fun read(filename: String): String = readCanonResource(filename)
}

internal expect fun readCanonResource(filename: String): String
