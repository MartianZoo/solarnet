package dev.martianzoo.tfm.canon

import dev.martianzoo.engine.Routine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.toSetStrict

/**
 * An internal Catalog-provider bundle loaded from conventionally named Pets and JSON resources.
 *
 * `classes.pets`, when present, supplies declarations and compact map diagrams; card and language
 * metadata remain JSON. Files for unsupported canonical data are recognized but ignored; other
 * files produce a warning. A bundle identity is raw source provenance, not a Pets class, so no
 * declaration is required or synthesized for it. Callers whose resources are not in Canon's
 * generated index can provide [resourceFilenames] and [resourceReader] directly.
 */
internal class StandardFormBundle(
    name: String,
    override val customClasses: Set<CustomClass> = emptySet(),
    override val moduleContentSelections: Map<ClassName, Set<BundleContentSelection>> = emptyMap(),
    override val routines: Map<String, Routine> = emptyMap(),
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
      val unexpected = resourceSet.filenames.filterNot(::isExpected).sorted()
      if (unexpected.isNotEmpty()) {
        println("Warning: ignoring unexpected files in ${resourceSet.directory}: $unexpected")
      }
    }
  }

  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    resources
        .flatMap { resourceSet ->
          readIfPresent(resourceSet, CLASSES_FILENAME, ::parseClasses) +
              readIfPresent(resourceSet, CARD_PETS_FILENAME, ::parseClasses)
        }
        .toSetStrict()
  }

  override val displayNamesByLanguage: Map<String, Map<ClassName, String>> by lazy {
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
  }

  private val cardsByResource: Map<ResourceSet, Set<CardDefinition>> by lazy {
    resources.associateWith { resourceSet ->
      readIfPresent(resourceSet, CARDS_FILENAME, JsonReader::readCards)
          .toSetStrict(::CardDefinition)
    }
  }

  override val cardDefinitions: Set<CardDefinition> by lazy {
    cardsByResource.values.flatten().toSetStrict()
  }

  override val moduleCardDefinitions: Map<ClassName, Set<CardDefinition>> by lazy {
    cardsByResource
        .filterValues { it.isNotEmpty() }
        .mapKeys { (resourceSet, _) -> cn(resourceSet.directory.substringAfterLast('/')) }
  }

  override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
    resources.flatMapTo(linkedSetOf()) { resourceSet ->
      readIfPresent(resourceSet, CLASSES_FILENAME, MarsMapReader::readMaps)
    }
  }

  private fun read(resourceSet: ResourceSet, filename: String): String =
      resourceReader("${resourceSet.directory}/$filename")

  private fun <T> readIfPresent(
      resourceSet: ResourceSet,
      filename: String,
      parse: (String) -> List<T>,
  ): List<T> =
      if (filename in resourceSet.filenames) parse(read(resourceSet, filename)) else emptyList()

  private fun isExpected(filename: String): Boolean =
      filename == CLASSES_FILENAME ||
          filename == CARD_PETS_FILENAME ||
          LANGUAGE_FILENAME.matches(filename) ||
          filename in KNOWN_JSON_FILENAMES

  private companion object {
    private const val CARDS_FILENAME: String = "cards.json5"
    private const val DEFAULT_DIRECTORY = "bundles"
    private const val CLASSES_FILENAME = "classes.pets"
    private const val CARD_PETS_FILENAME = "cards.pets"
    private val LANGUAGE_FILENAME = Regex("language/([^/]+)\\.json5")
    private val KNOWN_JSON_FILENAMES =
        setOf(
            CARDS_FILENAME,
        )
  }

  private data class ResourceSet(val directory: String, val filenames: Set<String>)
}
