package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

internal object CanonResources {
  fun read(filename: String): String = GeneratedCanonResources.read(filename)

  val bundleModules: Map<String, ClassName?> by lazy {
    JsonReader.readBundleManifest(read("bundles/manifest.json5")).mapValues { (_, module) ->
      module?.let(::cn)
    }
  }

  fun filenames(directory: String): Set<String> {
    val prefix = directory.trimEnd('/') + "/"
    return GeneratedCanonResources.filenames
        .filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix) }
        .toSet()
  }
}
