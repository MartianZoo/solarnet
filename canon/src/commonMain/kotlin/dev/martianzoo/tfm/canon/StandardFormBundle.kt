package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.Bundle
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.util.toSetStrict

/**
 * A ruleset bundle loaded from conventionally named Pets and JSON resource files.
 *
 * Every `.pets` file in [resourceDirectory] is loaded. The supported JSON filenames are exposed as
 * constants below. Files for unsupported canonical data are recognized but ignored; other files
 * produce a warning. A declaration outside `setup.pets` replaces a same-named setup declaration
 * wholesale in gameplay rulesets; setup declarations without replacements are reused. A bundle
 * identity is raw source provenance, not a Pets class, so no declaration is required or synthesized
 * for it. Callers whose resources are not in Canon's generated index can provide
 * [resourceFilenames] and [resourceReader] directly.
 */
public class StandardFormBundle(
    name: String,
    override val customClasses: Set<CustomClass> = emptySet(),
    gameOptionClassNames: Set<ClassName> = emptySet(),
    private val areaShortNamePrefix: String? = null,
    public val resourceDirectory: String = "$DEFAULT_DIRECTORY/$name",
    private val resourceFilenames: Set<String> = CanonResources.filenames(resourceDirectory),
    private val resourceReader: (String) -> String = CanonResources::read,
) : Bundle(cn(name), gameOptionClassNames) {
  init {
    require(resourceFilenames.isNotEmpty()) { "No resources in $resourceDirectory" }
    val unexpected = resourceFilenames.filterNot(::isExpected).sorted()
    if (unexpected.isNotEmpty()) {
      println("Warning: ignoring unexpected files in $resourceDirectory: $unexpected")
    }
  }

  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    val gameplayDeclarations =
        resourceFilenames
            .filter { it.endsWith(PETS_EXTENSION) && it != SETUP_FILENAME }
            .sorted()
            .flatMap { parseClasses(read(it)) }
            .toSetStrict()
    val gameplayNames = gameplayDeclarations.mapTo(hashSetOf()) { it.className }
    gameplayDeclarations + setupClassDeclarations.filter { it.className !in gameplayNames }
  }

  /** Setup-world declarations contributed separately from this bundle's gameplay rules. */
  override val setupClassDeclarations: Set<ClassDeclaration> by lazy {
    val sourceDeclarations =
        if (SETUP_FILENAME in resourceFilenames) parseClasses(read(SETUP_FILENAME)).toSetStrict()
        else emptySet()
    sourceDeclarations +
        colonyTileDefinitions.map {
          parseOneLinerClass("CLASS ${it.className}Selected : SelectedColonyTile")
        }
  }

  override val cardDefinitions: Set<CardDefinition> by lazy {
    readIfPresent(CARDS_FILENAME, JsonReader::readCards).toSetStrict(::CardDefinition)
  }

  override val standardActionDefinitions: Set<StandardActionDefinition> by lazy {
    readIfPresent(ACTIONS_FILENAME, JsonReader::readActions).toSetStrict()
  }

  override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
    if (MAPS_FILENAME in resourceFilenames) {
      JsonReader.readMaps(read(MAPS_FILENAME), requireNotNull(areaShortNamePrefix)).toSetStrict()
    } else {
      emptySet()
    }
  }

  override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
    readIfPresent(MILESTONES_FILENAME, JsonReader::readMilestones).toSetStrict()
  }

  override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
    readIfPresent(COLONIES_FILENAME, JsonReader::readColonyTiles)
        .toSetStrict(::ColonyTileDefinition)
  }

  private fun read(filename: String): String = resourceReader("$resourceDirectory/$filename")

  private fun <T> readIfPresent(filename: String, parse: (String) -> List<T>): List<T> =
      if (filename in resourceFilenames) parse(read(filename)) else emptyList()

  private fun isExpected(filename: String): Boolean =
      filename.endsWith(PETS_EXTENSION) || filename in KNOWN_JSON_FILENAMES

  public companion object {
    public const val ACTIONS_FILENAME: String = "actions.json5"
    public const val CARDS_FILENAME: String = "cards.json5"
    public const val COLONIES_FILENAME: String = "colonies.json5"
    public const val MAPS_FILENAME: String = "maps.json5"
    public const val MILESTONES_FILENAME: String = "milestones.json5"
    public const val SETUP_FILENAME: String = "setup.pets"

    private const val AWARDS_FILENAME = "awards.json5"
    private const val DEFAULT_DIRECTORY = "bundles"
    private const val PETS_EXTENSION = ".pets"
    private val KNOWN_JSON_FILENAMES =
        setOf(
            ACTIONS_FILENAME,
            AWARDS_FILENAME,
            CARDS_FILENAME,
            COLONIES_FILENAME,
            MAPS_FILENAME,
            MILESTONES_FILENAME,
        )
  }
}
