package dev.martianzoo.tfm.api

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.AUTO_LOAD
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.Definition
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Player
import dev.martianzoo.data.classNameRenamer
import dev.martianzoo.pets.Vocabulary.Companion.petsClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.tfm.api.BundleContentSelection.Kind
import dev.martianzoo.tfm.data.AwardDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.util.associateByStrict

/** A Terraforming Mars Authority with typed registries for its structured definitions. */
public open class TfmAuthority : Authority {
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
   * defaults, implications, and selection policies.
   *
   * Structured definitions may use unambiguous English Pets names. Naming any milestones or awards
   * selects the exact configured pool for that category. A playable Terraforming Mars Authority
   * requires one to five player class names in seat order. New, otherwise unknown names are
   * introduced as real player classes.
   */
  public open fun gamePremise(config: GameConfig): GamePremise {
    val configuredPlayerNames = config.playerClassNames
    if (PLAYER_CLASS in allClassNames) {
      require(configuredPlayerNames.size in 1..5) {
        "a Terraforming Mars configuration must have 1 to 5 player class names"
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
    val implications = bundles.flatMap { it.metadata.configurationImplications }
    var changed: Boolean
    do {
      changed = false
      implications
          .filter { it.appliesTo(included) }
          .forEach { implication ->
            changed = included.addAll(implication.included - explicitlyExcluded) || changed
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
        individualSelections.mapTo(linkedSetOf()) { (className, included) ->
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
    val aliases =
        canonicalPlayerNames
            .zip(configuredPlayerNames)
            .filter { (canonical, configured) -> canonical != configured }
            .toMap()
    val rename = classNameRenamer(aliases)
    return GamePremise(
        this,
        moduleNames,
        classSelections.mapTo(linkedSetOf()) { selection ->
          selection.copy(
              className = rename.transform(selection.className),
              requirement = selection.requirement?.let(rename::transform),
          )
        },
        initialTypes.mapTo(linkedSetOf(), rename::transform),
        configuredPlayerNames,
        aliases,
    )
  }

  private fun resolveConfigurationNames(names: Iterable<ClassName>): Set<ClassName> =
      names.mapTo(linkedSetOf()) { configuredName ->
        resolveConfigurationName(configuredName)
            ?: throw IllegalArgumentException("unknown configuration class: $configuredName")
      }

  private fun resolveConfigurationName(configuredName: ClassName): ClassName? {
    // PlayerN declarations are templates, not selectable configuration classes. Player names are
    // supplied through GameConfig.playerClassNames instead.
    if (Player.isDefaultClassName(configuredName)) return null
    if (configuredName in allClassNames) return configuredName
    val matches = derivedPetsNameClassNames.filter { canonicalName ->
      displayNamesByLanguage["en"]?.get(canonicalName)?.let(::petsClassName) == configuredName
    }
    require(matches.size <= 1) { "ambiguous configuration class $configuredName: $matches" }
    return matches.singleOrNull()
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
        cardDefinitions.flatMap(CardDefinition::extraClasses)
  }

  final override val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    validateReplacements(cardDefinitions, CardDefinition::id, CardDefinition::replaces)
    validateReplacements(
        milestoneDefinitions,
        MilestoneDefinition::id,
        MilestoneDefinition::replaces,
    )
    validateReplacements(awardDefinitions, AwardDefinition::id, AwardDefinition::replaces)
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
        .associate { declaration -> declaration.className to selectionsFor(declaration.className) }
  }

  private fun selectionsFor(moduleName: ClassName): Set<ClassSelection> {
    val owners = bundles.filter {
      moduleName in it.contributedClassDeclarations.map { d -> d.className }
    }
    require(owners.size <= 1) {
      "Module $moduleName has ambiguous bundle ownership: ${owners.map(Bundle::bundleName)}"
    }
    val owner = owners.singleOrNull() ?: return emptySet()
    val contentSelections =
        owner.moduleContentSelections[moduleName] ?: setOf(BundleContentSelection(owner.bundleName))
    val bundlesByName = bundles.associateByStrict(Bundle::bundleName)
    val selections =
        contentSelections.flatMapTo(linkedSetOf()) { content ->
          require(content.bundleName in bundlesByName) {
            "Module $moduleName selects unknown bundle ${content.bundleName}"
          }
          selectionsFrom(bundlesByName.getValue(content.bundleName), content.kinds)
        }
    selections += owner.metadata.moduleClassSelections[moduleName].orEmpty()
    return selections
  }

  private fun selectionsFrom(bundle: Bundle, kinds: Set<Kind>): Set<ClassSelection> = buildSet {
    if (Kind.AUTO_LOAD_CLASSES in kinds) {
      bundle.explicitClassDeclarations
          .filter { isSubtypeOf(it.className, AUTO_LOAD) }
          .mapTo(this) { ClassSelection(it.className) }
    }
    if (Kind.CARDS in kinds) {
      addDefinitions(bundle.cardDefinitions)
      addReplacementExclusions(
          bundle.cardDefinitions,
          cardDefinitions,
          CardDefinition::id,
          CardDefinition::replaces,
      )
    }
    if (Kind.STANDARD_ACTIONS in kinds) addDefinitions(bundle.standardActionDefinitions)
    if (Kind.MAPS in kinds) {
      bundle.marsMapDefinitions.forEach { map ->
        add(ClassSelection(map.className, requirement = map.setupRequirement))
        map.areas.forEach { area ->
          add(ClassSelection(area.className, requirement = map.setupRequirement))
        }
      }
    }
    if (Kind.MILESTONES in kinds) {
      addDefinitions(bundle.milestoneDefinitions)
      addReplacementExclusions(
          bundle.milestoneDefinitions,
          milestoneDefinitions,
          MilestoneDefinition::id,
          MilestoneDefinition::replaces,
      )
    }
    if (Kind.AWARDS in kinds) {
      addDefinitions(bundle.awardDefinitions)
      addReplacementExclusions(
          bundle.awardDefinitions,
          awardDefinitions,
          AwardDefinition::id,
          AwardDefinition::replaces,
      )
    }
    if (Kind.COLONY_TILES in kinds) addDefinitions(bundle.colonyTileDefinitions)
  }

  private fun MutableSet<ClassSelection>.addDefinitions(definitions: Collection<Definition>) {
    definitions.mapTo(this) { definition ->
      ClassSelection(definition.className, requirement = definition.setupRequirement)
    }
  }

  private fun <D : Definition> MutableSet<ClassSelection>.addReplacementExclusions(
      selected: Collection<D>,
      known: Collection<D>,
      id: (D) -> String,
      replaces: (D) -> String?,
  ) {
    val knownById = known.associateByStrict(id)
    selected.forEach { replacement ->
      var target = replaces(replacement)
      while (target != null) {
        val replaced = knownById.getValue(target)
        add(
            ClassSelection(
                className = replaced.className,
                included = false,
                requirement = replacement.setupRequirement,
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
            .filter { it.included && it.appliesTo(configuredClassNames) }
            .mapTo(hashSetOf(), ClassSelection::className)
    validateSelectedReplacements(
        cardDefinitions.filter { it.className in selectedDefinitionNames },
        CardDefinition::id,
        CardDefinition::replaces,
    )
    validateSelectedReplacements(
        milestoneDefinitions.filter { it.className in selectedDefinitionNames },
        MilestoneDefinition::id,
        MilestoneDefinition::replaces,
    )
    validateSelectedReplacements(
        awardDefinitions.filter { it.className in selectedDefinitionNames },
        AwardDefinition::id,
        AwardDefinition::replaces,
    )
  }

  private fun <D> validateSelectedReplacements(
      selected: Collection<D>,
      id: (D) -> String,
      replaces: (D) -> String?,
  ) {
    selected
        .mapNotNull { replacement -> replaces(replacement)?.let { it to replacement } }
        .groupBy({ it.first }, { it.second })
        .forEach { (target, replacements) ->
          require(replacements.size == 1) {
            "multiple selected replacements for $target: ${replacements.map(id)}"
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

  public fun action(name: ClassName): StandardActionDefinition = standardActionDefinitions.first {
    it.className == name
  }

  public open val standardActionDefinitions: Set<StandardActionDefinition> = emptySet()

  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.firstOrNull { it.className == name }
          ?: throw IllegalArgumentException("No `$name` in this Authority")

  public open val marsMapDefinitions: Set<MarsMapDefinition> = emptySet()

  public fun milestone(name: ClassName): MilestoneDefinition = milestoneDefinitions.first {
    it.className == name
  }

  public open val milestoneDefinitions: Set<MilestoneDefinition> = emptySet()

  public fun award(name: ClassName): AwardDefinition = awardDefinitions.first {
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
    private val DELAYED_COLONY_TILE = cn("DelayedColonyTile")

    private fun <D> validateReplacements(
        definitions: Collection<D>,
        id: (D) -> String,
        replaces: (D) -> String?,
    ) {
      val knownById = definitions.associateByStrict(id)
      definitions.forEach { definition ->
        replaces(definition)?.let { target ->
          require(target in knownById) { "${id(definition)} replaces unknown definition $target" }
        }
      }
      definitions.forEach { start ->
        val path = mutableSetOf<String>()
        var current: D? = start
        while (current != null && replaces(current) != null) {
          require(path.add(id(current))) { "replacement cycle involving ${id(current)}" }
          current = knownById.getValue(replaces(current)!!)
        }
      }
    }
  }

  /** One Authority assembled from several providers before it is exposed to callers. */
  public open class Composite(vararg authorities: TfmAuthority) : TfmAuthority() {
    public val authorities: List<TfmAuthority> = authorities.toList()

    final override val bundles: List<Bundle> = authorities.flatMap(TfmAuthority::bundles)

    override val bootstrapValidations: List<Set<Requirement>> by lazy {
      authorities.flatMap(Authority::bootstrapValidations)
    }

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
