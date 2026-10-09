package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ModuleProperties.AUTO_SELECT_WHEN
import dev.martianzoo.pets.types.Class as PetClass
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.PremiseClassTable
import dev.martianzoo.pets.util.associateByStrict
import dev.martianzoo.state.Catalog
import dev.martianzoo.state.ClassSelection
import dev.martianzoo.state.GameConfig
import dev.martianzoo.state.GamePremise
import dev.martianzoo.state.GamePremiseBuilder

/** A Terraforming Mars Catalog with declarations, structured card/map data, and selection rules. */
public open class TfmCatalog(vararg catalogs: Catalog) : Catalog(*catalogs) {
  final override val customClassDependencies: Map<ClassName, Set<ClassName>> = buildMap {
    putAll(super.customClassDependencies)
    TFM_CUSTOM_CLASS_DEPENDENCIES.forEach { (name, dependencies) ->
      val previous = put(name, dependencies)
      require(previous == null || previous == dependencies) {
        "Conflicting custom class dependencies for $name"
      }
    }
  }

  final override val transformHandlerFactories: Map<String, (ClassTable) -> TransformHandler> =
      mapOf(
          TfmClasses.PROD to Prod::handler,
      )

  private val universe: ClassTable
    get() = classTable

  final override fun validateClasses(table: ClassTable) {
    val tagClass = table.findClass(TAG_CLASS) ?: return
    val eventCard = table.findClass(TfmClasses.EVENT_CARD)
    val eventTagRequirement: Requirement = parse("=1 EventTag<This>")
    eventCard?.let {
      require(eventTagRequirement in it.declaration.invariants) {
        "EventCard must declare HAS $eventTagRequirement"
      }
    }
    val projectCard = table.findClass(TfmClasses.PROJECT_CARD)
    val activeCard = table.findClass(TfmClasses.ACTIVE_CARD)
    val automatedCard = table.findClass(TfmClasses.AUTOMATED_CARD)
    cardClassNames.map(table::getClass).forEach { card ->
      cardTags(card).elements.forEach { tagName ->
        require(table.getClass(tagName).isSubtypeOf(tagClass)) {
          "${card.className} names non-Tag class $tagName as a tag"
        }
      }
      if (TfmClasses.EVENT_TAG in cardTags(card).elements) {
        require(eventCard != null && card.isSubtypeOf(eventCard)) {
          "non-EventCard ${card.className} has an EventTag"
        }
      }
      if (
          projectCard != null &&
              eventCard != null &&
              activeCard != null &&
              automatedCard != null &&
              cardBack(card)?.isSubtypeOf(projectCard) == true &&
              !card.isSubtypeOf(eventCard)
      ) {
        val hasNontrivialBehavior =
            cardActions(card).isNotEmpty() ||
                card.invariants.filterIsInstance<Exact>().any {
                  val expression = (it.countedMetric as? Count)?.expression
                  it.expected > 0 &&
                      expression != null &&
                      THIS.expression in expression.arguments &&
                      table.getClass(expression.className).carriesPersistentBehavior()
                } ||
                cardEffects(card).any { effect ->
                  when {
                    effect.trigger.isEndTrigger() -> false
                    !effect.trigger.isSelfGainTrigger() -> true
                    else ->
                        effect.instruction.descendantsOfType<Gain>().any { gain ->
                          table.getClass(gain.gaining.className).carriesPersistentBehavior()
                        }
                  }
                }
        val active = card.isSubtypeOf(activeCard)
        val automated = card.isSubtypeOf(automatedCard)
        require(active == hasNontrivialBehavior && automated == !hasNontrivialBehavior) {
          "${card.className} must be ActiveCard exactly when it has actions or persistent effects; " +
              "otherwise it must be AutomatedCard"
        }
      }
    }
  }

  private fun Trigger.isEndTrigger(): Boolean =
      when (this) {
        is OnGainOf -> expression.className == TfmClasses.END
        is Trigger.Or -> triggers.all { it.isEndTrigger() }
        is Trigger.WrappingTrigger -> inner.isEndTrigger()
        is Trigger.OnRemoveOf,
        WhenGain,
        Trigger.WhenRemove -> false
      }

  private fun Trigger.isSelfGainTrigger(): Boolean =
      when (this) {
        WhenGain -> true
        is Trigger.Or -> triggers.all { it.isSelfGainTrigger() }
        is Trigger.WrappingTrigger -> inner.isSelfGainTrigger()
        is OnGainOf,
        is Trigger.OnRemoveOf,
        Trigger.WhenRemove -> false
      }

  private fun PetClass.carriesPersistentBehavior(): Boolean =
      allSuperclasses().any { superclass ->
        superclass.declaration.authoredActions.isNotEmpty() ||
            superclass.declaration.authoredEffects.any { effect ->
              !effect.trigger.isSelfGainTrigger() && !effect.trigger.isEndTrigger()
            }
      }

  /** Organizational bundles from which this Catalog is assembled. */
  public open val bundles: List<Bundle> =
      catalogs.filterIsInstance<TfmCatalog>().flatMap { it.bundles }

  /** Concrete subclasses of [superclass] whose declarations live in [bundleName]. */
  public fun classNamesInBundle(
      bundleName: ClassName,
      superclass: ClassName,
  ): Set<ClassName> {
    val matchingBundles = bundles.filter { it.bundleName == bundleName }
    require(matchingBundles.size == 1) {
      "expected one bundle named $bundleName; found ${matchingBundles.size}"
    }
    return bundleClassesBelow(matchingBundles.single(), superclass)
        .mapTo(linkedSetOf(), ClassDeclaration::className)
  }

  final override val classAvailabilityModules: Map<ClassName, Set<ClassName>> by lazy {
    buildMap<ClassName, MutableSet<ClassName>> {
          bundles.forEach { bundle ->
            val availabilityModules = buildSet {
              val sameNamedModule =
                  bundle.bundleName in allClassNames && isSubtypeOf(bundle.bundleName, MODULE_CLASS)
              if (sameNamedModule) {
                add(bundle.bundleName)
              } else {
                bundle.marsMapDefinitions.mapTo(this, MarsMapDefinition::className)
              }
            }
            if (availabilityModules.isNotEmpty()) {
              val goalDeclarations =
                  listOf(TfmClasses.MILESTONE, TfmClasses.AWARD).flatMap { goalClass ->
                    bundleClassesBelow(bundle, goalClass, includeAbstract = true)
                  }
              val customClassNames =
                  customClassDeclarations.mapTo(hashSetOf(), ClassDeclaration::className)
              val goalSupportClassNames =
                  goalDeclarations
                      .flatMap(ClassDeclaration::allNodes)
                      .flatMapTo(linkedSetOf()) { node -> node.descendantsOfType<ClassName>() }
                      .filterTo(linkedSetOf()) { it in customClassNames }
              val contentClassNames = buildSet {
                addAll(bundle.cardResourceClassNames)
                bundle.marsMapDefinitions.forEach { map ->
                  add(map.className)
                  map.areas.mapTo(this) { area -> area.className }
                }
                goalDeclarations.mapTo(this, ClassDeclaration::className)
                addAll(goalSupportClassNames)
              }
              val ambientClassNames =
                  bundle.explicitClassDeclarations
                      .map(ClassDeclaration::className)
                      .filterNot(contentClassNames::contains)
              ambientClassNames
                  .filterNot { isSubtypeOf(it, MODULE_CLASS) }
                  .forEach { className ->
                    getOrPut(className, ::linkedSetOf).addAll(availabilityModules)
                  }
            }
          }
        }
        .mapValues { (_, modules) -> modules.toSet() }
  }

  /**
   * Cooks user-facing Module and setup selections into an exact game premise by applying Catalog
   * defaults and selection policies.
   *
   * Structured inputs use canonical Class Names. Naming any milestones or awards selects the exact
   * configured pool for that category; an explicitly named milestone bypasses automatic pool
   * preferences. A playable Terraforming Mars Catalog requires at least one player name in seat
   * order. Missing names and the generated concrete `Premise` Class belong to the premise-local
   * declaration table; their immediate effects create the resolved Modules, Players, and exact
   * starting Components without recompiling this Catalog's master table.
   */
  override fun gamePremise(
      config: GameConfig,
      additionalInitialComponentTypes: Set<Expression>,
      additionalClassDeclarations: Set<ClassDeclaration>,
  ): GamePremise {
    if (PLAYER in allClassNames && config.playerNames.isEmpty()) {
      throw InvalidGameConfigException(
          "a Terraforming Mars configuration must have at least one player name"
      )
    }
    val builder =
        GamePremiseBuilder(
            this,
            config,
            additionalInitialComponentTypes,
            additionalClassDeclarations,
        )
    val explicitlyIncluded = builder.explicitlyIncluded
    val explicitlyExcluded = builder.explicitlyExcluded
    val included = builder.included
    val configurationTable = builder.configurationTable
    cards
        .filter { it.className in explicitlyIncluded }
        .forEach { card ->
          cardCompatibilityRequirement(card)?.let { requirement ->
            if (
                !requirement.isMetBy { metric ->
                  builder.countConfigured(metric, included - card.className)
                }
            ) {
              throw InvalidGameConfigException(
                  "configured content ${card.className} is unavailable: $requirement"
              )
            }
          }
        }
    marsMapDefinitions.forEach { map ->
      (listOf(map.className) + map.areas.map { area -> area.className })
          .filter { it in explicitlyIncluded }
          .forEach { className ->
            contentCompatibilityRequirement(className)?.let { requirement ->
              if (
                  !requirement.isMetBy { metric ->
                    builder.countConfigured(metric, included - className)
                  }
              ) {
                throw InvalidGameConfigException(
                    "configured content $className is unavailable: $requirement"
                )
              }
            }
          }
    }
    (explicitlyIncluded intersect goalClassNames(TfmClasses.AWARD)).forEach { awardName ->
      awardCompatibilityRequirement(awardName)?.let { requirement ->
        if (
            !requirement.isMetBy { metric ->
              builder.countConfigured(metric, included - awardName)
            }
        ) {
          throw InvalidGameConfigException(
              "configured class $awardName is unavailable: $requirement"
          )
        }
      }
    }
    val moduleNames = builder.moduleNames
    val selectedMilestoneNames =
        selectGoalPool(
            moduleNames,
            included,
            explicitlyIncluded,
            explicitlyExcluded,
            TfmClasses.MILESTONE,
            configurationTable,
        )
    val selectedAwardNames =
        selectGoalPool(
            moduleNames,
            included,
            explicitlyIncluded,
            explicitlyExcluded,
            TfmClasses.AWARD,
            configurationTable,
        )
    included.addAll(selectedMilestoneNames + selectedAwardNames)
    val individualNames = included - moduleNames
    if (selectedMilestoneNames.isNotEmpty()) {
      builder.excluded.addAll(goalClassNames(TfmClasses.MILESTONE) - selectedMilestoneNames)
    }
    if (selectedAwardNames.isNotEmpty()) {
      builder.excluded.addAll(goalClassNames(TfmClasses.AWARD) - selectedAwardNames)
    }
    val selectedByModules =
        moduleNames
            .flatMap { modules.getValue(it) }
            .filter { it.included && it.appliesTo(included, configurationTable) }
            .mapTo(hashSetOf(), ClassSelection::className)
    if (individualNames.intersect(colonyTileClassNames).any { it !in selectedByModules }) {
      throw InvalidGameConfigException("selected ColonyTiles must be provided by a selected Module")
    }
    val initialTypes =
        individualNames
            .filter { it in colonyTileClassNames }
            .mapTo(builder.initialComponentTypes) {
              SELECTED_COLONY_TILE.of(it.classExpression())
            }
    config.playerNames.firstOrNull()?.let { firstPlayer ->
      initialTypes.add(TfmClasses.START_TOKEN.of(firstPlayer.expression))
      config.playerNames.zip(config.playerNames.drop(1) + firstPlayer).mapTo(initialTypes) {
          (player, nextPlayer) ->
        TfmClasses.AFTER_ME.of(player.expression, nextPlayer.expression)
      }
    }
    builder.bootstrapClassName = BOOTSTRAP_PHASE.takeIf {
      moduleNames.isNotEmpty() && it in allClassNames
    }
    if (MODULES_READY in allClassNames) {
      builder.initializationEffects.add(parse("This: ModulesReady"))
    }
    return builder.build()
  }

  private fun selectGoalPool(
      moduleNames: Set<ClassName>,
      configuredClassNames: Set<ClassName>,
      explicitlyIncluded: Set<ClassName>,
      explicitlyExcluded: Set<ClassName>,
      goalClass: ClassName,
      configurationTable: PremiseClassTable,
  ): Set<ClassName> {
    val knownGoals = goalClassNames(goalClass)
    val explicitlySelected = explicitlyIncluded intersect knownGoals
    if (explicitlySelected.isNotEmpty()) return explicitlySelected
    if (MULTIPLAYER_MODE !in moduleNames) return emptySet()

    return bundles
        .filter { bundle -> bundle.bundleName in moduleNames }
        .flatMap { bundle -> classNamesInBundle(bundle.bundleName, goalClass) }
        .map { className ->
          val declaration = classDeclaration(className)
          ClassSelection(
              className,
              requirement = goalAutomaticSelectionRequirement(declaration),
          )
        }
        .filter { selection -> selection.appliesTo(configuredClassNames, configurationTable) }
        .mapTo(linkedSetOf(), ClassSelection::className)
        .minus(explicitlyExcluded)
  }

  /** Catalog-known concrete subclasses of the ordinary Pets `ColonyTile` class. */
  public val colonyTileClassNames: Set<ClassName> by lazy {
    val colonyTile = universe.findClass(COLONY_TILE) ?: return@lazy emptySet()
    universe
        .allSubclasses(colonyTile)
        .filterNot { it.abstract }
        .mapTo(linkedSetOf()) { it.className }
  }

  override fun withDeclarations(declarations: Set<ClassDeclaration>): TfmCatalog =
      TfmCatalog(
          this,
          object : Catalog() {
            override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
          },
      )

  private fun goalClassNames(goalClass: ClassName): Set<ClassName> =
      bundles
          .flatMap { bundle -> bundleClassesBelow(bundle, goalClass) }
          .mapTo(linkedSetOf(), ClassDeclaration::className)

  private fun bundleClassesBelow(
      bundle: Bundle,
      superclass: ClassName,
      includeAbstract: Boolean = false,
  ): List<ClassDeclaration> =
      bundle.explicitClassDeclarations.filter { declaration ->
        (includeAbstract || !declaration.abstract) && isSubtypeOf(declaration.className, superclass)
      }

  // CLASS DECLARATIONS

  override val contributedClassDeclarations: List<ClassDeclaration> by lazy {
    val explicit = explicitClassDeclarations.map(TfmActionLowerer::lower)
    val explicitNames = explicit.mapTo(hashSetOf(), ClassDeclaration::className)
    val requiredNames = buildSet {
      marsMapDefinitions.forEach { map ->
        add(map.className)
        map.areas.mapTo(this) { area -> area.className }
      }
    }
    val missing = requiredNames - explicitNames
    require(missing.isEmpty()) {
      "Structured content lacks explicit Pets declarations: ${missing.sortedBy(ClassName::toString)}"
    }
    explicit
  }

  // MODULES

  final override val modules: Map<ClassName, Set<ClassSelection>> by lazy {
    allClassDeclarations.values
        .filter { declaration ->
          !declaration.abstract && isSubtypeOf(declaration.className, MODULE_CLASS)
        }
        .associate { declaration ->
          declaration.className to selectionsFor(declaration.className)
        }
  }

  private fun owningBundle(className: ClassName): Bundle? {
    val owners = bundles.filter { bundle ->
      bundle.explicitClassDeclarations.any { it.className == className }
    }
    require(owners.size <= 1) {
      "$className has ambiguous bundle ownership: ${owners.map(Bundle::bundleName)}"
    }
    return owners.singleOrNull()
  }

  private fun selectionsFor(moduleName: ClassName): Set<ClassSelection> {
    val owner = owningBundle(moduleName) ?: return emptySet()
    owner.marsMapDefinitions
        .singleOrNull { it.className == moduleName }
        ?.let { map ->
          return buildSet {
            add(ClassSelection(map.className))
            map.areas.forEach { area -> add(ClassSelection(area.className)) }
          }
        }
    val selections = linkedSetOf<ClassSelection>()
    owner.moduleCardClassNames[moduleName]?.let { names ->
      val cards = cards.filterTo(linkedSetOf()) { it.className in names }
      selections.addCards(cards)
      selections.addCardResourceRoots(owner.moduleCardClassNames.getValue(moduleName))
    }
    if (moduleName == owner.bundleName) {
      owner.explicitClassDeclarations
          .filter { declaration ->
            !declaration.abstract &&
                (isSubtypeOf(declaration.className, COLONY_TILE) ||
                    isSubtypeOf(declaration.className, COLONY_TILE_SELECTION))
          }
          .mapTo(selections) { declaration -> ClassSelection(declaration.className) }
    }
    return selections
  }

  private fun MutableSet<ClassSelection>.addCards(
      cards: Collection<PetClass>,
      sharedRequirement: Requirement? = null,
  ) {
    cards.mapTo(this) { card ->
      ClassSelection(
          card.className,
          requirement = Requirement.join(sharedRequirement, automaticSelectionRequirement(card)),
      )
    }
  }

  private fun MutableSet<ClassSelection>.addCardResourceRoots(resourceClassNames: Set<ClassName>) {
    val referencedNames =
        resourceClassNames.flatMapTo(linkedSetOf()) { sourceName ->
          classDeclaration(sourceName)
              .allNodes
              .flatMap { node -> node.descendantsOfType<ClassName>() }
              .filter { referencedName ->
                referencedName != sourceName && referencedName in resourceClassNames
              }
        }
    (resourceClassNames - cardClassNames - referencedNames).mapTo(this) { className ->
      ClassSelection(className, requirement = contentCompatibilityRequirement(className))
    }
  }

  private fun automaticSelectionRequirement(card: PetClass): Requirement? {
    return Requirement.join(
        listOf(card, cardBack(card))
            .mapNotNull { selectedClass ->
              (selectedClass?.properties?.get(AUTO_SELECT_WHEN) as? RequirementValue)?.value
            }
            .fold<Requirement, Requirement?>(null, Requirement::join),
        cardBundleCompatibilityRequirement(card),
    )
  }

  private fun goalAutomaticSelectionRequirement(
      declaration: ClassDeclaration,
  ): Requirement? =
      Requirement.join(
          Requirement.join(
              Requirement.join(
                  MULTIPLAYER_ONLY,
                  (universe.getClass(declaration.className).properties[AUTO_SELECT_WHEN]
                          as? RequirementValue)
                      ?.value,
              ),
              declaration.invariants.fold<Requirement, Requirement?>(null, Requirement::join),
          ),
          bundleCompatibilityRequirement(declaration.className, listOf(declaration)),
      )

  private fun awardCompatibilityRequirement(
      className: ClassName,
  ): Requirement? =
      Requirement.join(
          MULTIPLAYER_ONLY,
          bundleCompatibilityRequirement(className, listOf(classDeclaration(className))),
      )

  private fun cardCompatibilityRequirement(card: PetClass): Requirement? =
      cardBundleCompatibilityRequirement(card)

  private fun cardBundleCompatibilityRequirement(card: PetClass): Requirement? {
    val declarations = linkedMapOf<ClassName, ClassDeclaration>()
    val pending = ArrayDeque<ClassName>().apply { add(card.className) }
    while (pending.isNotEmpty()) {
      val declaration = classDeclaration(pending.removeFirst())
      if (declarations.put(declaration.className, declaration) != null) continue
      declaration.allNodes
          .flatMap { node -> node.descendantsOfType<ClassName>() }
          .filterTo(pending) { referencedName -> referencedName in cardAuxiliaryClassNames }
    }
    return bundleCompatibilityRequirement(card.className, declarations.values.toList())
  }

  private val cardAuxiliaryClassNames: Set<ClassName> by lazy {
    bundles.flatMapTo(linkedSetOf()) { bundle -> bundle.cardResourceClassNames } - cardClassNames
  }

  private fun contentCompatibilityRequirement(className: ClassName): Requirement? =
      bundleCompatibilityRequirement(className, listOf(classDeclaration(className)))

  private fun bundleCompatibilityRequirement(
      className: ClassName,
      declarations: List<ClassDeclaration>,
  ): Requirement? {
    val referencedClassNames =
        declarations
            .flatMap { it.allNodes }
            .flatMapTo(linkedSetOf()) { node -> node.descendantsOfType<ClassName>() }
            .filter { it != className && it in allClassNames }
    val derived =
        listOfNotNull(availabilityRequirement(className)) +
            referencedClassNames.mapNotNull(::availabilityRequirement)
    return derived.fold<Requirement, Requirement?>(null, Requirement::join)
  }

  private fun availabilityRequirement(className: ClassName): Requirement? =
      classAvailabilityModules[className]
          ?.map { moduleName -> Min(1, Count(moduleName.expression)) }
          ?.let(Or::create)

  // STRUCTURED CONTENT DATA

  public fun card(name: ClassName): PetClass =
      cardsByClassName[name] ?: throw IllegalArgumentException("No card named $name")

  private val cardClassNames: Set<ClassName> by lazy {
    if (TfmClasses.CARD_FRONT !in allClassNames) return@lazy emptySet()
    explicitClassDeclarations
        .asSequence()
        .filterNot(ClassDeclaration::abstract)
        .map(ClassDeclaration::className)
        .filter { className -> isSubtypeOf(className, TfmClasses.CARD_FRONT) }
        .toCollection(linkedSetOf())
  }

  /** Every concrete card face in this Catalog's loaded class universe. */
  public val cards: Set<PetClass> by lazy {
    cardClassNames.mapTo(linkedSetOf(), classTable::getClass)
  }

  private val cardsByClassName by lazy {
    cards.associateByStrict(PetClass::className)
  }

  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.firstOrNull { it.className == name }
          ?: throw IllegalArgumentException("No `$name` in this Catalog")

  public open val marsMapDefinitions: Set<MarsMapDefinition> =
      catalogs.filterIsInstance<TfmCatalog>().flatMapTo(linkedSetOf()) { it.marsMapDefinitions }

  private companion object {
    private val TFM_CUSTOM_CLASS_DEPENDENCIES: Map<ClassName, Set<ClassName>> =
        mapOf(
            cn("PartyDistance") to setOf(cn("AfterParty")),
            cn("PlayerDistance") to setOf(cn("AfterMe")),
            cn("PartyRequirement") to setOf(cn("PartyDelegate"), cn("Ruling")),
            cn("PriceAspectCount") to setOf(cn("ProjectCard")),
        )

    private val BOOTSTRAP_PHASE = cn("BootstrapPhase")
    private val MODULE_CLASS = cn("Module")
    private val MODULES_READY = cn("ModulesReady")
    private val MULTIPLAYER_MODE = cn("MultiplayerMode")
    private val TAG_CLASS = cn("Tag")
    private val COLONY_TILE = cn("ColonyTile")
    private val COLONY_TILE_SELECTION = cn("ColonyTileSelection")
    private val SELECTED_COLONY_TILE = cn("SelectedColonyTile")
    private val MULTIPLAYER_ONLY: Requirement = parse("MultiplayerMode")
  }
}
