package dev.martianzoo.tfm.canon

internal object CanonResources {
  fun read(filename: String): String = GeneratedCanonResources.read(filename)

  val bundleNames: Set<String>
    get() =
        GeneratedCanonResources.filenames
            .map { filename -> filename.removePrefix("bundles/").substringBefore('/') }
            .toSet()

  fun filenames(directory: String): Set<String> {
    val prefix = directory.trimEnd('/') + "/"
    return GeneratedCanonResources.filenames
        .filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix) }
        .toSet()
  }
}
