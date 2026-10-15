package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.carddata.CardData

/**
 * A Catalog-provider bundle built from conventionally named Pets and JSON sources.
 *
 * Every `.pets` resource supplies declarations. Generated `cards.pets` and handwritten
 * `*.cards.pets` fragments additionally identify the bundle's card declarations, while bundle
 * language files and compact map diagrams supply category-specific metadata. Card replacement
 * metadata comes from the corresponding authored card JSON. A bundle identity is raw source
 * provenance, not a Pets class, so no declaration is required or synthesized for it. Canon supplies
 * [primaryModuleName] from its bundle manifest. Callers whose resources are not in Canon's
 * generated registry can provide [resourceFilenames] and [resourceReader] directly.
 *
 * @param name bundle identity and the default resource-directory suffix.
 * @param primaryModuleName Module governing this bundle's default Content and support availability.
 * @param resourceDirectory directory passed to [resourceReader], defaulting to `bundles/name`.
 * @param resourceFilenames relative paths in that directory; an empty set is rejected.
 * @param resourceReader reads each selected resource by its full path. Its failures propagate.
 */
public class StandardFormBundle
public constructor(
    name: String,
    primaryModuleName: ClassName? = null,
    private val resourceDirectory: String = "$DEFAULT_DIRECTORY/$name",
    private val resourceFilenames: Set<String> = CanonResources.filenames(resourceDirectory),
    private val resourceReader: (String) -> String = CanonResources::read,
) : Bundle(name, primaryModuleName) {
  private val resources = listOf(ResourceSet(resourceDirectory, resourceFilenames))

  init {
    resources.forEach { resourceSet ->
      require(resourceSet.filenames.isNotEmpty()) { "No resources in ${resourceSet.directory}" }
    }
  }

  private val petDeclarationsByResource:
      Map<
          ResourceSet,
          Map<String, List<ClassDeclaration>>,
      > =
      resources.associateWith { resourceSet ->
        petSourceFilenames(resourceSet).associateWith { filename ->
          parseClasses(read(resourceSet, filename))
        }
      }

  private val cardDeclarationsByResource: Map<ResourceSet, Set<ClassDeclaration>> =
      petDeclarationsByResource.mapValues { (_, declarationsByFilename) ->
        declarationsByFilename.filterKeys(::isCardPetSourceFilename).values.flatten().toSetStrict()
      }

  override val cardResourceClassNames: Set<ClassName> =
      cardDeclarationsByResource.values.flatten().mapTo(linkedSetOf(), ClassDeclaration::className)

  override val replacements: Map<ClassName, ClassName> = buildMap {
    if (name in CardData.bundleNames) {
      CardData.definitions(name).forEach { card ->
        card.replaces?.let { replaced ->
          put(cn(card.name), cn(replaced))
        }
      }
    }
    resources
        .filter { REPLACEMENTS_FILENAME in it.filenames }
        .forEach { resourceSet ->
          JsonReader.readReplacements(read(resourceSet, REPLACEMENTS_FILENAME)).forEach {
              (replacement, relation) ->
            require(replacement !in this) { "duplicate replacement relationship for $replacement" }
            put(replacement, relation)
          }
        }
  }

  override val excludedDefaultCards: Set<ClassName> =
      resources
          .filter { DEFAULT_EXCLUSIONS_FILENAME in it.filenames }
          .flatMapTo(linkedSetOf()) { resourceSet ->
            JsonReader.readExcludedDefaultCards(read(resourceSet, DEFAULT_EXCLUSIONS_FILENAME))
          }

  override val explicitClassDeclarations: Set<ClassDeclaration> =
      petDeclarationsByResource.values
          .flatMap { declarationsByFilename -> declarationsByFilename.values.flatten() }
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
        readPetSources(resourceSet, MarsMapReader::readMaps)
      }

  private fun read(resourceSet: ResourceSet, filename: String): String =
      resourceReader("${resourceSet.directory}/$filename")

  private fun petSourceFilenames(resourceSet: ResourceSet): List<String> =
      resourceSet.filenames.filter(::isPetSourceFilename).sorted()

  private fun <T> readPetSources(
      resourceSet: ResourceSet,
      parse: (String) -> List<T>,
  ): List<T> =
      petSourceFilenames(resourceSet).flatMap { filename ->
        parse(read(resourceSet, filename))
      }

  private fun isPetSourceFilename(filename: String): Boolean = filename.endsWith(PETS_SUFFIX)

  private fun isCardPetSourceFilename(filename: String): Boolean =
      filename == CARD_PETS_FILENAME || filename.endsWith(CARD_PETS_SUFFIX)

  private companion object {
    private const val DEFAULT_DIRECTORY = "bundles"
    private const val CARD_PETS_FILENAME = "cards.pets"
    private const val CARD_PETS_SUFFIX = ".cards.pets"
    private const val REPLACEMENTS_FILENAME = "replacements.json5"
    private const val DEFAULT_EXCLUSIONS_FILENAME = "card-pool-exclusions.json5"
    private const val PETS_SUFFIX = ".pets"
    private val LANGUAGE_FILENAME = Regex("language/([^/]+)\\.json5")
  }

  private data class ResourceSet(val directory: String, val filenames: Set<String>)
}
