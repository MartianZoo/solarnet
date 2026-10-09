package dev.martianzoo.state

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ModuleProperties.AUTO_SELECT_WHEN
import dev.martianzoo.pets.types.PremiseClassTable

/**
 * Resolves the generic part of a [GameConfig], then accepts game-specific selections and setup.
 * [build] freezes those choices into a [GamePremise] without changing the Catalog's master table.
 * Automatic Module defaults are resolved before game-specific content policies are applied.
 */
public class GamePremiseBuilder(
    private val catalog: Catalog,
    config: GameConfig,
    additionalClassDeclarations: Set<ClassDeclaration> = emptySet(),
) {
  public val playerNames: List<ClassName> = config.playerNames
  public val configurationTable: PremiseClassTable
  public val explicitlyIncluded: Set<ClassName>
  public val explicitlyExcluded: Set<ClassName>
  public val included: MutableSet<ClassName>
  public val excluded: MutableSet<ClassName>
  public var bootstrapClassName: ClassName? = null
  /** Additional effects on the generated Premise, after its ordinary initialization effects. */
  public val initializationEffects: MutableList<Effect> = mutableListOf()
  public val moduleNames: Set<ClassName>
    get() = included.filterTo(linkedSetOf()) { it in catalog.modules }

  private val componentAdjustments: Map<ClassName, Int>
  private val premiseClassDeclarations: Set<ClassDeclaration>

  init {
    val configuredPlayerNames = playerNames
    if (PLAYER !in catalog.allClassNames && playerNames.isNotEmpty()) {
      throw InvalidGameConfigException(
          "a Catalog without Player cannot configure player names: $playerNames"
      )
    }
    if (
        additionalClassDeclarations.any { declaration ->
          declaration.customMetric ||
              declaration.supertypes.any { it.className == CUSTOM_INSTRUCTION }
        }
    ) {
      throw InvalidGameConfigException(
          "premise-local custom Classes require a Catalog-owned implementation"
      )
    }
    val additionalNames =
        additionalClassDeclarations.mapTo(linkedSetOf(), ClassDeclaration::className)
    val missingPlayerNames = configuredPlayerNames.filter {
      it !in catalog.allClassNames && it !in additionalNames
    }
    val playerDeclarations =
        if (missingPlayerNames.isEmpty()) emptySet()
        else
            parseClasses(missingPlayerNames.joinToString("\n") { name -> "CLASS $name : Player" })
                .toSet()
    configurationTable =
        PremiseClassTable(catalog.classTable, additionalClassDeclarations + playerDeclarations)
    premiseClassDeclarations = additionalClassDeclarations + playerDeclarations
    componentAdjustments = resolveComponentAdjustments(config.componentAdjustments)
    explicitlyIncluded =
        resolveConfigurationNames(config.includedClassNames) + configuredPlayerNames
    explicitlyExcluded = resolveConfigurationNames(config.excludedClassNames)
    if (explicitlyIncluded.intersect(explicitlyExcluded).isNotEmpty()) {
      throw InvalidGameConfigException(
          "a game configuration cannot include and exclude the same class"
      )
    }

    var selected = explicitlyIncluded
    val seenSelections = mutableSetOf<Set<ClassName>>()
    while (seenSelections.add(selected)) {
      val next = explicitlyIncluded.toMutableSet()
      catalog.modules.keys
          .filter { moduleName -> moduleName !in explicitlyExcluded }
          .forEach { moduleName ->
            val property = catalog.classTable.getClass(moduleName).properties[AUTO_SELECT_WHEN]
            val requirement = (property as? RequirementValue)?.value ?: return@forEach
            if (
                requirement.isMetBy { metric ->
                  countConfigured(metric, selected - moduleName)
                }
            ) {
              next.add(moduleName)
            }
          }
      if (next == selected) break
      selected = next
    }
    if (seenSelections.last() != selected) {
      throw InvalidPetDefinitionException(
          "Module defaults do not converge: ${seenSelections.joinToString(" -> ")}"
      )
    }

    included = selected.toMutableSet()
    excluded = explicitlyExcluded.toMutableSet()
  }

  /** Completes the exact selections and the ordinary Pets declaration that initializes them. */
  public fun build(): GamePremise {
    val modules = moduleNames
    val resolvedSelections = linkedMapOf<ClassName, Boolean>()
    catalog.modules.keys.forEach { resolvedSelections[it] = false }
    included.forEach { resolvedSelections[it] = true }
    excluded.forEach { resolvedSelections[it] = false }
    val premiseDeclaration =
        if (modules.isEmpty() && componentAdjustments.isEmpty() && initializationEffects.isEmpty())
            null
        else
            generatedPremiseDeclaration(
                modules.toList(),
                playerNames,
                componentAdjustments,
            )
    return GamePremise(
        catalog = catalog,
        classSelections =
            resolvedSelections
                .filterKeys { it !in playerNames }
                .mapTo(linkedSetOf()) { (name, included) -> ClassSelection(name, included) },
        componentAdjustments = componentAdjustments,
        playerNames = playerNames,
        bootstrapClassName = bootstrapClassName,
        premiseClassName = PREMISE_CLASS.takeIf { premiseDeclaration != null },
        premiseClassDeclarations = premiseClassDeclarations + listOfNotNull(premiseDeclaration),
    )
  }

  /** Counts configured Classes matching a simple Class metric, including Player subclasses. */
  public fun countConfigured(
      metric: Metric,
      configuredClassNames: Set<ClassName>,
  ): Int {
    if (metric !is Count || !metric.expression.simple) {
      throw InvalidPetDefinitionException("Module defaults must count simple classes: $metric")
    }
    return configuredClassNames.count { configuredName ->
      configurationTable.isSubtypeOf(configuredName, metric.expression.className)
    }
  }

  private fun resolveConfigurationNames(names: Iterable<ClassName>): Set<ClassName> =
      names.mapTo(linkedSetOf()) { configuredName ->
        resolveConfigurationName(configuredName)
            ?: throw InvalidGameConfigException("unknown configuration class: $configuredName")
      }

  private fun resolveConfigurationName(configuredName: ClassName): ClassName? {
    val configuredClass = catalog.classTable.findClass(configuredName) ?: return null
    val playerClass = catalog.classTable.findClass(PLAYER)
    if (playerClass != null && configuredClass.isSubtypeOf(playerClass)) return null
    return configuredName.takeIf { it in catalog.allClassNames }
  }

  private fun resolveComponentAdjustments(
      requestedAdjustments: Map<ClassName, Int>
  ): Map<ClassName, Int> =
      requestedAdjustments.entries.associateTo(linkedMapOf()) { (requestedName, adjustment) ->
        val name =
            resolveConfigurationName(requestedName)
                ?: throw InvalidGameConfigException(
                    "unknown setup adjustment class: $requestedName"
                )
        val configuredClass = catalog.classTable.getClass(name)
        if (
            configuredClass.abstract ||
                configuredClass.defaultType.abstract ||
                !configuredClass.isSubtypeOf(catalog.classTable.getClass(SYSTEM)) ||
                requestedName in catalog.modules
        ) {
          throw InvalidGameConfigException(
              "setup adjustment class must be a concrete dependency-free non-Module System: $name"
          )
        }
        name to adjustment
      }

  private fun generatedPremiseDeclaration(
      moduleNames: List<ClassName>,
      playerNames: List<ClassName>,
      componentAdjustments: Map<ClassName, Int>,
  ): ClassDeclaration {
    require(PREMISE_CLASS !in catalog.allClassNames) {
      "$PREMISE_CLASS is reserved for the resolved game configuration"
    }
    val effects = buildList {
      if (moduleNames.isNotEmpty()) add("This:: ${moduleNames.joinToString()}")
      if (PLAYER in catalog.allClassNames && playerNames.isNotEmpty()) {
        add("This:: ${playerNames.joinToString(" THEN ")}")
      }
      if (componentAdjustments.isNotEmpty()) {
        add(
            "This:: " +
                componentAdjustments.entries.joinToString { (name, adjustment) ->
                  "$adjustment $name"
                }
        )
      }
    }
    return parseClasses(
            """
            "The resolved game configuration that initializes this game"
            CLASS Premise : System {
              HAS =1 This
              ${effects.joinToString("\n")}
            }
            """
                .trimIndent()
        )
        .single()
        .let { it.copy(authoredEffects = it.authoredEffects + initializationEffects) }
  }

  private companion object {
    val PREMISE_CLASS = cn("Premise")
  }
}
