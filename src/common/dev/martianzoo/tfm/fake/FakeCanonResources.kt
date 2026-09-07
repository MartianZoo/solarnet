package dev.martianzoo.tfm.fake

internal object FakeCanonResources {
  fun read(filename: String): String = GeneratedFakeCanonResources.read(filename)

  fun filenames(directory: String): Set<String> {
    val prefix = directory.trimEnd('/') + "/"
    return GeneratedFakeCanonResources.filenames
        .filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix) }
        .toSet()
  }
}
