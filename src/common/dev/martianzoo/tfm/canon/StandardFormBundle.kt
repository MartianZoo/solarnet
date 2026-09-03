package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.toSetStrict

/**
 * An internal Catalog-provider bundle built from conventionally named Pets and JSON sources.
 *
 * `classes.pets` and `cards.pets` supply declarations, while bundle language files and compact map
 * diagrams supply category-specific metadata. Each is read by name or by language pattern, so a
 * directory may hold anything else without affecting the bundle. A bundle identity is raw source
 * provenance, not a Pets class, so no declaration is required or synthesized for it. Callers whose
 * resources are not in Canon's generated registry can provide [resourceFilenames] and
 * [resourceReader] directly.
 */
internal class StandardFormBundle(
    name: String,
    override val customClasses: Set<CustomClass> = emptySet(),
    override val moduleContentSelections: Map<ClassName, Set<BundleContentSelection>> = emptyMap(),
    override val moduleClassExclusions: Map<ClassName, Set<ClassName>> = emptyMap(),
    private val resourceDirectory: String = "$DEFAULT_DIRECTORY/$name",
    private val resourceFilenames: Set<String> = CanonResources.filenames(resourceDirectory),
    private val resourceReader: (String) -> String = CanonResources::read,
    private val additionalResourceDirectories: Set<String> = emptySet(),
) : Bundle(cn(name)) {
  private val resources =
      listOf(ResourceSet(resourceDirectory, resourceFilenames)) +
          additionalResourceDirectories.sorted().map { directory ->
            ResourceSet(directory, CanonResources.filenames(directory))
          }

  init {
    resources.forEach { resourceSet ->
      require(resourceSet.filenames.isNotEmpty()) { "No resources in ${resourceSet.directory}" }
    }
  }

  private val cardDeclarationsByResource: Map<ResourceSet, Set<ClassDeclaration>> =
      resources.associateWith { resourceSet ->
        readIfPresent(resourceSet, CARD_PETS_FILENAME, ::parseClasses).toSetStrict()
      }

  override val cardResourceClassNames: Set<ClassName> =
      cardDeclarationsByResource.values.flatten().mapTo(linkedSetOf(), ClassDeclaration::className)

  override val moduleCardClassNames: Map<ClassName, Set<ClassName>> =
      cardDeclarationsByResource
          .filterValues(Set<ClassDeclaration>::isNotEmpty)
          .mapKeys { (resourceSet, _) -> cn(resourceSet.directory.substringAfterLast('/')) }
          .mapValues { (_, declarations) ->
            declarations.mapTo(linkedSetOf(), ClassDeclaration::className)
          }

  override val explicitClassDeclarations: Set<ClassDeclaration> =
      resources
          .flatMap { resourceSet ->
            readIfPresent(resourceSet, CLASSES_FILENAME, ::parseClasses)
          }
          .plus(cardDeclarationsByResource.values.flatten())
          .toSetStrict()

  override val displayNamesByLanguage: Map<String, Map<ClassName, String>> =
      buildMap<String, MutableMap<ClassName, String>> {
        resources.forEach { resourceSet ->
          resourceSet.filenames.forEach { filename ->
            LANGUAGE_FILENAME.matchEntire(filename)?.groupValues?.get(1)?.let { language ->
              val names = getOrPut(language, ::linkedMapOf)
              JsonReader.readDisplayNames(read(resourceSet, filename)).forEach {
                  (className, displayName) ->
                val previous = names.put(className, displayName)
                require(previous == null || previous == displayName) {
                  "Conflicting $language display names for $className: $previous and $displayName"
                }
              }
            }
          }
        }
      }

  override val marsMapDefinitions: Set<MarsMapDefinition> =
      resources.flatMapTo(linkedSetOf()) { resourceSet ->
        readIfPresent(resourceSet, CLASSES_FILENAME, MarsMapReader::readMaps)
      }

  private fun read(resourceSet: ResourceSet, filename: String): String =
      resourceReader("${resourceSet.directory}/$filename")

  private fun <T> readIfPresent(
      resourceSet: ResourceSet,
      filename: String,
      parse: (String) -> List<T>,
  ): List<T> =
      if (filename in resourceSet.filenames) parse(read(resourceSet, filename)) else emptyList()

  private companion object {
    private const val DEFAULT_DIRECTORY = "bundles"
    private const val CLASSES_FILENAME = "classes.pets"
    private const val CARD_PETS_FILENAME = "cards.pets"
    private val LANGUAGE_FILENAME = Regex("language/([^/]+)\\.json5")
  }

  private data class ResourceSet(val directory: String, val filenames: Set<String>)
}
