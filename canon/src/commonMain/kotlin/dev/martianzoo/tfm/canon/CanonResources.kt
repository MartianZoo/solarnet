package dev.martianzoo.tfm.canon

internal object CanonResources {
  fun read(filename: String): String = readCanonResource(filename)

  fun filenames(directory: String): Set<String> {
    val prefix = directory.trimEnd('/') + "/"
    return resourcePaths
        .filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix) }
        .filter { '/' !in it }
        .toSet()
  }

  private val resourcePaths: Set<String> by lazy {
    read(RESOURCE_INDEX).lineSequence().filter(String::isNotBlank).toSet()
  }

  private const val RESOURCE_INDEX = "resource-index.txt"
}

internal expect fun readCanonResource(filename: String): String
