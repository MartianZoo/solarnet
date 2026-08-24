package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.Bundle
import dev.martianzoo.tfm.api.BundleContentSelection
import dev.martianzoo.tfm.data.AwardDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.util.toSetStrict

/**
 * An internal Authority-provider bundle loaded from conventionally named Pets and JSON resources.
 *
 * `classes.pets`, when present, supplies the Pets declarations, including milestones and awards.
 * The supported JSON filenames are exposed as constants below. Files for unsupported canonical data
 * are recognized but ignored; other files produce a warning. A bundle identity is raw source
 * provenance, not a Pets class, so no declaration is required or synthesized for it. Callers whose
 * resources are not in Canon's generated index can provide [resourceFilenames] and [resourceReader]
 * directly.
 */
internal class StandardFormBundle(
    name: String,
    override val customClasses: Set<CustomClass> = emptySet(),
    override val moduleContentSelections: Map<ClassName, Set<BundleContentSelection>> = emptyMap(),
    private val resourceDirectory: String = "$DEFAULT_DIRECTORY/$name",
    private val resourceFilenames: Set<String> = CanonResources.filenames(resourceDirectory),
    private val resourceReader: (String) -> String = CanonResources::read,
) : Bundle(cn(name)) {
  init {
    require(resourceFilenames.isNotEmpty()) { "No resources in $resourceDirectory" }
    val unexpected = resourceFilenames.filterNot(::isExpected).sorted()
    if (unexpected.isNotEmpty()) {
      println("Warning: ignoring unexpected files in $resourceDirectory: $unexpected")
    }
  }

  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    readIfPresent(CLASSES_FILENAME, ::parseClasses).toSetStrict()
  }

  override val displayNamesByLanguage: Map<String, Map<ClassName, String>> by lazy {
    resourceFilenames
        .mapNotNull { filename ->
          LANGUAGE_FILENAME.matchEntire(filename)?.groupValues?.get(1)?.let { language ->
            language to JsonReader.readDisplayNames(read(filename))
          }
        }
        .toMap()
  }

  override val cardDefinitions: Set<CardDefinition> by lazy {
    readIfPresent(CARDS_FILENAME, JsonReader::readCards).toSetStrict(::CardDefinition)
  }

  override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
    mapJsonResourceFiles().flatMapTo(linkedSetOf()) { filename ->
      JsonReader.readMaps(read(filename))
    }
  }

  override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
    goalDeclarations(cn("Milestone"))
        .map { (declaration, group) ->
          MilestoneDefinition.fromClassDeclaration(declaration, group)
        }
        .toSetStrict()
  }

  override val awardDefinitions: Set<AwardDefinition> by lazy {
    goalDeclarations(cn("Award"))
        .map { (declaration, group) -> AwardDefinition.fromClassDeclaration(declaration, group) }
        .toSetStrict()
  }

  private fun goalDeclarations(
      goalClass: ClassName,
  ): List<Pair<ClassDeclaration, ClassName?>> {
    val groups =
        explicitClassDeclarations
            .filter { declaration ->
              declaration.abstract && declaration.supertypes.any { it.className == goalClass }
            }
            .mapTo(linkedSetOf(), ClassDeclaration::className)
    return explicitClassDeclarations.mapNotNull { declaration ->
      if (declaration.abstract) return@mapNotNull null
      val superclasses = declaration.supertypes.map { it.className }
      when {
        goalClass in superclasses -> declaration to null
        else -> superclasses.singleOrNull { it in groups }?.let { declaration to it }
      }
    }
  }

  private fun read(filename: String): String = resourceReader("$resourceDirectory/$filename")

  private fun <T> readIfPresent(filename: String, parse: (String) -> List<T>): List<T> =
      if (filename in resourceFilenames) parse(read(filename)) else emptyList()

  private fun mapJsonResourceFiles(): List<String> =
      resourceFilenames.filter { it == MAPS_FILENAME || it.endsWith("-$MAPS_FILENAME") }.sorted()

  private fun isExpected(filename: String): Boolean =
      filename == CLASSES_FILENAME ||
          LANGUAGE_FILENAME.matches(filename) ||
          filename in KNOWN_JSON_FILENAMES ||
          filename.endsWith("-$MAPS_FILENAME")

  public companion object {
    private const val CARDS_FILENAME: String = "cards.json5"
    internal const val MAPS_FILENAME: String = "maps.json5"
    private const val DEFAULT_DIRECTORY = "bundles"
    private const val CLASSES_FILENAME = "classes.pets"
    private val LANGUAGE_FILENAME = Regex("language/([^/]+)\\.json5")
    private val KNOWN_JSON_FILENAMES =
        setOf(
            CARDS_FILENAME,
            MAPS_FILENAME,
        )
  }
}
