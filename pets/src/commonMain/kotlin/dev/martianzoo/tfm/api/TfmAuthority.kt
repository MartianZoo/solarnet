package dev.martianzoo.tfm.api

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassProperties.ACTIVATION_REQUIREMENT
import dev.martianzoo.data.ClassProperties.AUTOMATIC_SELECTION_REQUIREMENT
import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.Definition
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.ModuleProperties.AUTO_SELECT_WHEN
import dev.martianzoo.data.ModuleProvenance
import dev.martianzoo.data.Player
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.And
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.tfm.api.BundleContentSelection.Kind
import dev.martianzoo.tfm.data.AwardDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable
import dev.martianzoo.util.associateByStrict

/** A Terraforming Mars Authority with typed registries for its structured definitions. */
public open class TfmAuthority : Authority {
  final override val classTable: ClassTable by lazy {
    ClassLoader(this).loadEverything().also(::validateCardTags)
  }

  private val universe: ClassTable by lazy { classTable }

  private fun validateCardTags(table: ClassTable) {
    val tagClass = table.findClass(TAG_CLASS) ?: return
    cardDefinitions.forEach { card ->
      card.tags.elements.forEach { tagName ->
        require(table.getClass(tagName).isSubtypeOf(tagClass)) {
          "${card.className} names non-Tag class $tagName as a tag"
        }
      }
    }
  }

  final override val derivedPetsNameClassNames: Set<ClassName> by lazy {
    (cardDefinitions + milestoneDefinitions + awardDefinitions + colonyTileDefinitions).mapTo(
        linkedSetOf(),
        Definition::className,
    )
  }

  /** Organizational bundles from which this Authority is assembled. */
  public open val bundles: List<Bundle> = emptyList()

  /**
   * Cooks user-facing Module and setup selections into an exact game premise by applying Authority
   * defaults and selection policies.
   *
   * Structured definitions may use unambiguous English Pets names. Naming any milestones or awards
   * selects the exact configured pool for that category. A playable Terraforming Mars Authority
   * requires one to five player names in seat order. These become vocabulary aliases for the
   * canonical `Player1` through `Player5` classes.
   */
  public open fun gamePremise(config: GameConfig): GamePremise {
    val configuredPlayerNames = config.playerNames
    if (PLAYER_CLASS in allClassNames) {
      require(configuredPlayerNames.size in 1..5) {
        "a Terraforming Mars configuration must have 1 to 5 player names"
      }
    }
    val canonicalPlayerNames = Player.players(configuredPlayerNames.size).map(Player::className)
    val explicitlyIncluded =
        resolveConfigurationNames(config.includedClassNames) + canonicalPlayerNames
    val explicitlyExcluded = resolveConfigurationNames(config.excludedClassNames)
    require(explicitlyIncluded.intersect(explicitlyExcluded).isEmpty()) {
      "a game configuration cannot include and exclude the same class"
    }

    val included = explicitlyIncluded.toMutableSet()
    var changed: Boolean
    do {
      changed = false
      modules.keys
          .filter { moduleName -> moduleName !in included && moduleName !in explicitlyExcluded }
          .forEach { moduleName ->
            val property = universe.getClass(moduleName).properties[AUTO_SELECT_WHEN]
            val requirement = (property as? RequirementValue)?.value ?: return@forEach
            if (requirement.isMetBy { metric -> countConfigured(metric, included) }) {
              changed = included.add(moduleName) || changed
            }
          }
      included
          .filter { it in modules }
          .flatMap { source -> constructivelySelectedModules(source, included) }
          .forEach { target ->
            require(target !in explicitlyExcluded) {
              "active Module provenance selects excluded Module $target"
            }
            changed = included.add(target) || changed
          }
    } while (changed)

    val moduleNames = included.filterTo(linkedSetOf()) { it in modules }
    val colonyNames = colonyTileDefinitions.mapTo(hashSetOf(), Definition::className)
    val individualNames = included - moduleNames
    validateSelectedReplacements(moduleNames, included)

    val individualSelections = linkedMapOf<ClassName, Boolean>()
    individualNames.forEach { individualSelections[it] = true }
    (explicitlyExcluded - modules.keys).forEach { individualSelections[it] = false }
    addExactDefinitionSelections(
        individualSelections,
        milestoneDefinitions,
        explicitlyIncluded,
    )
    addExactDefinitionSelections(
        individualSelections,
        awardDefinitions,
        explicitlyIncluded,
    )
    val classSelections =
        individualSelections
            .filterKeys { it !in canonicalPlayerNames }
            .mapTo(linkedSetOf()) { (className, included) ->
              ClassSelection(className, included)
            }
    val initialTypes =
        individualNames
            .filter { it in colonyNames }
            .mapTo(linkedSetOf()) { className ->
              val resourceType = colonyTile(className).resourceType
              if (resourceType == null) {
                className.expression
              } else {
                DELAYED_COLONY_TILE.of(className.classExpression(), resourceType.classExpression())
              }
            }
    val selectedByModules =
        moduleNames
            .flatMap { modules.getValue(it) }
            .filter { it.included && it.appliesTo(included, universe) }
            .mapTo(hashSetOf(), ClassSelection::className)
    require(individualNames.intersect(colonyNames).all { it in selectedByModules }) {
      "initial ColonyTiles must be provided by a selected Module"
    }
    return GamePremise(
        this,
        moduleNames,
        classSelections,
        initialTypes,
        configuredPlayerNames,
    )
  }

  /** Module gains on an active Module's own creation are forward selection provenance. */
  private fun constructivelySelectedModules(
      source: ClassName,
      configuredClassNames: Set<ClassName>,
  ): Set<ClassName> =
      ModuleProvenance.gains(universe.getClass(source).declaration)
          .filter { gain ->
            gain.target in modules &&
                gain.requirements.all { requirement ->
                  requirement.isMetBy { metric -> countConfigured(metric, configuredClassNames) }
                }
          }
          .mapTo(linkedSetOf()) { it.target }

  private fun countConfigured(metric: Metric, configuredClassNames: Set<ClassName>): Int {
    require(metric is Count && metric.expression.simple) {
      "Module defaults must count simple classes: $metric"
    }
    val countedClass = universe.getClass(metric.expression.className)
    return configuredClassNames.count { configuredName ->
      universe.getClass(configuredName).isSubtypeOf(countedClass)
    }
  }

  private fun resolveConfigurationNames(names: Iterable<ClassName>): Set<ClassName> =
      names.mapTo(linkedSetOf()) { configuredName ->
        resolveConfigurationName(configuredName)
            ?: throw IllegalArgumentException("unknown configuration class: $configuredName")
      }

  private fun resolveConfigurationName(configuredName: ClassName): ClassName? {
    // Canonical PlayerN seat classes are activated only by the ordered player-name list.
    if (Player.isValid(configuredName)) return null
    return configuredName.takeIf { it in allClassNames }
  }

  private fun addExactDefinitionSelections(
      selections: MutableMap<ClassName, Boolean>,
      definitions: Set<Definition>,
      explicitlyIncluded: Set<ClassName>,
  ) {
    val definitionNames = definitions.mapTo(linkedSetOf(), Definition::className)
    val selectedNames = explicitlyIncluded intersect definitionNames
    if (selectedNames.isEmpty()) return
    definitionNames.forEach { selections[it] = it in selectedNames }
  }

  // CLASS DECLARATIONS

  internal open val contributedClassDeclarations: List<ClassDeclaration> by lazy {
    explicitClassDeclarations.toList() +
        allDefinitions.map(Definition::asClassDeclaration) +
        cardDefinitions.flatMap(CardDefinition::executableExtraClasses)
  }

  final override val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    validateReplacements(cardDefinitions, CardDefinition::className, CardDefinition::replaces)
    validateReplacements(
        milestoneDefinitions,
        MilestoneDefinition::className,
        MilestoneDefinition::replaces,
    )
    validateReplacements(awardDefinitions, AwardDefinition::className, AwardDefinition::replaces)
    try {
      (systemClassDeclarations.toList() + contributedClassDeclarations)
          .distinct()
          .associateByStrict { declaration ->
            validateSystemDeclaration(declaration)
            declaration.className
          }
    } catch (e: IllegalArgumentException) {
      throw PetException("Multiple class declarations must be identical: ${e.message}")
    }
  }

  final override val allDefinitions: Set<Definition> by lazy {
    setOf<Definition>() +
        cardDefinitions +
        awardDefinitions +
        milestoneDefinitions +
        colonyTileDefinitions +
        standardActionDefinitions +
        marsMapDefinitions +
        marsMapDefinitions.flatMap(MarsMapDefinition::areas)
  }

  private fun validateSystemDeclaration(declaration: ClassDeclaration) {
    when (declaration.className) {
      COMPONENT -> {
        require(declaration.abstract)
        require(declaration.supertypes.none())
        require(declaration.dependencies.none())
      }
      CLASS -> {
        require(!declaration.abstract)
        require(declaration.dependencies.single() == COMPONENT.expression)
      }
    }
  }

  // MODULES

  final override val modules: Map<ClassName, Set<ClassSelection>> by lazy {
    allClassDeclarations.values
        .filter { declaration ->
          !declaration.abstract && isSubtypeOf(declaration.className, MODULE_CLASS)
        }
        .associate { declaration ->
          val provenanceSelections =
              ModuleProvenance.gains(declaration)
                  .filter { gain -> isSubtypeOf(gain.target, MODULE_CLASS) }
                  .mapTo(linkedSetOf()) { gain ->
                    ClassSelection(
                        gain.target,
                        requirement =
                            if (gain.requirements.isEmpty()) null
                            else And.create(gain.requirements),
                    )
                  }
          declaration.className to (selectionsFor(declaration.className) + provenanceSelections)
        }
  }

  private fun selectionsFor(moduleName: ClassName): Set<ClassSelection> {
    val owners = bundles.filter {
      moduleName in it.contributedClassDeclarations.map { d -> d.className }
    }
    require(owners.size <= 1) {
      "Module $moduleName has ambiguous bundle ownership: ${owners.map(Bundle::bundleName)}"
    }
    val owner = owners.singleOrNull() ?: return emptySet()
    val groupedMilestones = owner.milestoneDefinitions.filter { it.selectionGroup == moduleName }
    val groupedAwards = owner.awardDefinitions.filter { it.selectionGroup == moduleName }
    if (groupedMilestones.isNotEmpty() || groupedAwards.isNotEmpty()) {
      return buildSet {
        addDefinitions(groupedMilestones)
        addReplacementExclusions(
            groupedMilestones,
            milestoneDefinitions,
            MilestoneDefinition::className,
            MilestoneDefinition::replaces,
        )
        addDefinitions(groupedAwards)
        addReplacementExclusions(
            groupedAwards,
            awardDefinitions,
            AwardDefinition::className,
            AwardDefinition::replaces,
        )
      }
    }
    owner.marsMapDefinitions
        .singleOrNull { it.className == moduleName }
        ?.let { map ->
          return buildSet {
            add(ClassSelection(map.className))
            map.areas.forEach { area -> add(ClassSelection(area.className)) }
          }
        }
    val contentSelections =
        owner.moduleContentSelections[moduleName]
            ?: if (moduleName == owner.bundleName) {
              setOf(
                  BundleContentSelection(
                      owner.bundleName,
                      setOf(Kind.CARDS, Kind.COLONY_TILES),
                  )
              )
            } else {
              emptySet()
            }
    val bundlesByName = bundles.associateByStrict(Bundle::bundleName)
    val selections =
        contentSelections.flatMapTo(linkedSetOf()) { content ->
          require(content.bundleName in bundlesByName) {
            "Module $moduleName selects unknown bundle ${content.bundleName}"
          }
          selectionsFrom(bundlesByName.getValue(content.bundleName), content)
        }
    return selections
  }

  private fun selectionsFrom(
      bundle: Bundle,
      selection: BundleContentSelection,
  ): Set<ClassSelection> = buildSet {
    val kinds = selection.kinds
    if (Kind.CARDS in kinds) {
      val selectedCards =
          bundle.cardDefinitions.filter { card ->
            selection.cardDecks == null || card.deck in selection.cardDecks
          }
      addDefinitions(selectedCards)
      addReplacementExclusions(
          selectedCards,
          cardDefinitions,
          CardDefinition::className,
          CardDefinition::replaces,
      )
    }
    if (Kind.MAPS in kinds) {
      bundle.marsMapDefinitions.forEach { map ->
        val requirement = automaticSelectionRequirement(map)
        add(ClassSelection(map.className, requirement = requirement))
        map.areas.forEach { area ->
          add(
              ClassSelection(
                  area.className,
                  requirement = Requirement.join(requirement, automaticSelectionRequirement(area)),
              )
          )
        }
      }
    }
    if (Kind.MILESTONES in kinds) {
      val definitions = bundle.milestoneDefinitions.filter { it.selectionGroup == null }
      addDefinitions(definitions)
      addReplacementExclusions(
          definitions,
          milestoneDefinitions,
          MilestoneDefinition::className,
          MilestoneDefinition::replaces,
      )
    }
    if (Kind.AWARDS in kinds) {
      val definitions = bundle.awardDefinitions.filter { it.selectionGroup == null }
      addDefinitions(definitions)
      addReplacementExclusions(
          definitions,
          awardDefinitions,
          AwardDefinition::className,
          AwardDefinition::replaces,
      )
    }
    if (Kind.COLONY_TILES in kinds) addDefinitions(bundle.colonyTileDefinitions)
  }

  private fun MutableSet<ClassSelection>.addDefinitions(definitions: Collection<Definition>) {
    definitions.mapTo(this) { definition ->
      ClassSelection(
          definition.className,
          requirement = automaticSelectionRequirement(definition),
      )
    }
  }

  private fun automaticSelectionRequirement(definition: Definition): Requirement? {
    val declarations =
        listOf(definition.asClassDeclaration) +
            if (definition is CardDefinition) definition.extraClasses else emptyList()
    val referencedClassNames =
        declarations
            .flatMap { it.allNodes }
            .flatMapTo(linkedSetOf()) { node -> node.descendantsOfType<ClassName>() }
            .filter { it != definition.className && it in allClassNames }
    val derived = referencedClassNames.flatMap { className ->
      val properties = universe.getClass(className).properties
      listOfNotNull(
          (properties[AUTOMATIC_SELECTION_REQUIREMENT] as? RequirementValue)?.value,
          (properties[ACTIVATION_REQUIREMENT] as? RequirementValue)?.value,
      )
    }
    return derived.fold(definition.automaticSelectionRequirement, Requirement::join)
  }

  private fun <D : Definition> MutableSet<ClassSelection>.addReplacementExclusions(
      selected: Collection<D>,
      known: Collection<D>,
      name: (D) -> ClassName,
      replaces: (D) -> ClassName?,
  ) {
    val knownByName = known.associateByStrict(name)
    selected.forEach { replacement ->
      var target = replaces(replacement)
      while (target != null) {
        val replaced = knownByName.getValue(target)
        add(
            ClassSelection(
                className = replaced.className,
                included = false,
                requirement = automaticSelectionRequirement(replacement),
            )
        )
        target = replaces(replaced)
      }
    }
  }

  private fun validateSelectedReplacements(
      moduleNames: Set<ClassName>,
      configuredClassNames: Set<ClassName>,
  ) {
    val selectedDefinitionNames =
        moduleNames
            .flatMap { modules.getValue(it) }
            .filter { it.included && it.appliesTo(configuredClassNames, universe) }
            .mapTo(hashSetOf(), ClassSelection::className)
    validateSelectedReplacements(
        cardDefinitions.filter { it.className in selectedDefinitionNames },
        CardDefinition::className,
        CardDefinition::replaces,
    )
    validateSelectedReplacements(
        milestoneDefinitions.filter { it.className in selectedDefinitionNames },
        MilestoneDefinition::className,
        MilestoneDefinition::replaces,
    )
    validateSelectedReplacements(
        awardDefinitions.filter { it.className in selectedDefinitionNames },
        AwardDefinition::className,
        AwardDefinition::replaces,
    )
  }

  private fun <D : Definition> validateSelectedReplacements(
      selected: Collection<D>,
      name: (D) -> ClassName,
      replaces: (D) -> ClassName?,
  ) {
    selected
        .mapNotNull { replacement -> replaces(replacement)?.let { it to replacement } }
        .groupBy({ it.first }, { it.second })
        .forEach { (target, replacements) ->
          require(replacements.size == 1) {
            "multiple selected replacements for $target: ${replacements.map(name)}"
          }
        }
  }

  private fun isSubtypeOf(className: ClassName, possibleSupertype: ClassName): Boolean {
    if (className == possibleSupertype) return true
    return classDeclaration(className).supertypes.any { supertype ->
      isSubtypeOf(supertype.className, possibleSupertype)
    }
  }

  // DEFINITIONS

  public fun card(name: ClassName): CardDefinition =
      cardsByClassName[name] ?: throw IllegalArgumentException("No card named $name")

  public open val cardDefinitions: Set<CardDefinition> = emptySet()

  private val cardsByClassName by lazy { cardDefinitions.associateByStrict(Definition::className) }

  private fun action(name: ClassName): StandardActionDefinition = standardActionDefinitions.first {
    it.className == name
  }

  public open val standardActionDefinitions: Set<StandardActionDefinition> = emptySet()

  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.firstOrNull { it.className == name }
          ?: throw IllegalArgumentException("No `$name` in this Authority")

  public open val marsMapDefinitions: Set<MarsMapDefinition> = emptySet()

  private fun milestone(name: ClassName): MilestoneDefinition = milestoneDefinitions.first {
    it.className == name
  }

  public open val milestoneDefinitions: Set<MilestoneDefinition> = emptySet()

  private fun award(name: ClassName): AwardDefinition = awardDefinitions.first {
    it.className == name
  }

  public open val awardDefinitions: Set<AwardDefinition> = emptySet()

  public fun colonyTile(name: ClassName): ColonyTileDefinition = colonyTileDefinitions.first {
    it.className == name
  }

  public open val colonyTileDefinitions: Set<ColonyTileDefinition> = emptySet()

  // CUSTOM CLASSES

  override val explicitClassDeclarations: Set<ClassDeclaration> = emptySet()

  override val customClasses: Set<CustomClass> = emptySet()

  public companion object {
    /** Returns one Authority containing the unique contributions from [authorities]. */
    public fun compose(vararg authorities: TfmAuthority): TfmAuthority = Composite(*authorities)

    private val MODULE_CLASS = cn("Module")
    private val PLAYER_CLASS = cn("Player")
    private val TAG_CLASS = cn("Tag")
    private val DELAYED_COLONY_TILE = cn("DelayedColonyTile")

    private fun <D : Definition> validateReplacements(
        definitions: Collection<D>,
        name: (D) -> ClassName,
        replaces: (D) -> ClassName?,
    ) {
      val knownByName = definitions.associateByStrict(name)
      definitions.forEach { definition ->
        replaces(definition)?.let { target ->
          require(target in knownByName) {
            "${name(definition)} replaces unknown definition $target"
          }
        }
      }
      definitions.forEach { start ->
        val path = mutableSetOf<ClassName>()
        var current: D? = start
        while (current != null && replaces(current) != null) {
          require(path.add(name(current))) { "replacement cycle involving ${name(current)}" }
          current = knownByName.getValue(replaces(current)!!)
        }
      }
    }
  }

  /** One Authority assembled from several providers before it is exposed to callers. */
  public open class Composite(vararg authorities: TfmAuthority) : TfmAuthority() {
    private val authorities: List<TfmAuthority> = authorities.toList()

    final override val bundles: List<Bundle> = authorities.flatMap(TfmAuthority::bundles)

    override val displayNamesByLanguage: Map<String, Map<ClassName, String>> by lazy {
      val combined = mutableMapOf<String, MutableMap<ClassName, String>>()
      authorities.forEach { authority ->
        authority.displayNamesByLanguage.forEach { (language, names) ->
          val languageNames = combined.getOrPut(language, ::linkedMapOf)
          names.forEach { (className, displayName) ->
            val previous = languageNames.put(className, displayName)
            require(previous == null || previous == displayName) {
              "Conflicting $language display names for $className: $previous and $displayName"
            }
          }
        }
      }
      combined
    }

    internal override val contributedClassDeclarations: List<ClassDeclaration> by lazy {
      authorities.flatMap(TfmAuthority::contributedClassDeclarations)
    }

    override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
      authorities.flatMapTo(linkedSetOf(), Authority::explicitClassDeclarations)
    }

    override val cardDefinitions: Set<CardDefinition> by lazy {
      authorities.flatMapTo(linkedSetOf(), TfmAuthority::cardDefinitions)
    }

    override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
      authorities.flatMapTo(linkedSetOf(), TfmAuthority::marsMapDefinitions)
    }

    override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
      authorities.flatMapTo(linkedSetOf(), TfmAuthority::milestoneDefinitions)
    }

    override val awardDefinitions: Set<AwardDefinition> by lazy {
      authorities.flatMapTo(linkedSetOf(), TfmAuthority::awardDefinitions)
    }

    override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
      authorities.flatMapTo(linkedSetOf(), TfmAuthority::colonyTileDefinitions)
    }

    override val standardActionDefinitions: Set<StandardActionDefinition> by lazy {
      authorities.flatMapTo(linkedSetOf(), TfmAuthority::standardActionDefinitions)
    }

    override val customClasses: Set<CustomClass> by lazy {
      authorities.flatMapTo(linkedSetOf(), Authority::customClasses)
    }
  }
}
