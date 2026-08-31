package dev.martianzoo.tfm.canon

import dev.martianzoo.engine.Routine
import dev.martianzoo.engine.RoutineProvider
import dev.martianzoo.engine.RoutineReplayEncoder
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.And
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.ModuleProperties.AUTO_SELECT_WHEN
import dev.martianzoo.pets.data.ModuleProvenance
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.pets.types.Class as PetClass
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.util.associateByStrict
import dev.martianzoo.tfm.canon.BundleContentSelection.Kind

/** A Terraforming Mars Catalog with declarations, structured card/map data, and selection rules. */
public open class TfmCatalog : Catalog, RoutineProvider {
  override val routines: Map<String, Routine> = emptyMap()
  final override val replayEncoder: RoutineReplayEncoder = TerraformingMarsReplayEncoder

  final override val transformHandlerFactories: Map<String, (ClassTable) -> TransformHandler> =
      mapOf(
          TfmClasses.PROD to Prod::handler,
          CardOperation.TRANSFORM_KIND to { FollowModeNeutralizer },
      )

  final override val classTable: ClassTable by lazy {
    ClassLoader(this).loadEverything().also(::validateCards)
  }

  private val universe: ClassTable
    get() = classTable

  private fun validateCards(table: ClassTable) {
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
                card.declaration.authoredEffects.any { effect ->
                  !effect.trigger.isSelfGainTrigger() && !effect.trigger.isEndTrigger()
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

  final override val derivedPetsNameClassNames: Set<ClassName> by lazy {
    buildSet {
      addAll(cardClassNames)
      addAll(goalClassNames(TfmClasses.MILESTONE))
      addAll(goalClassNames(TfmClasses.AWARD))
      addAll(colonyTileClassNames)
    }
  }

  /** Organizational bundles from which this Catalog is assembled. */
  public open val bundles: List<Bundle> = emptyList()

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
              val contentClassNames = buildSet {
                addAll(bundle.cardResourceClassNames)
                bundle.marsMapDefinitions.forEach { map ->
                  add(map.className)
                  map.areas.mapTo(this) { area -> area.className }
                }
              }
                  .toMutableSet()
              bundleClassesBelow(bundle, TfmClasses.MILESTONE, includeAbstract = true)
                  .mapTo(contentClassNames, ClassDeclaration::className)
              bundleClassesBelow(bundle, TfmClasses.AWARD, includeAbstract = true)
                  .mapTo(contentClassNames, ClassDeclaration::className)
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
   * Structured inputs may use unambiguous English Pets names. Naming any milestones or awards
   * selects the exact configured pool for that category. A playable Terraforming Mars Catalog
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

    var included = explicitlyIncluded
    val seenSelections = mutableSetOf<Set<ClassName>>()
    while (seenSelections.add(included)) {
      val next = explicitlyIncluded.toMutableSet()
      modules.keys
          .filter { moduleName -> moduleName !in explicitlyExcluded }
          .forEach { moduleName ->
            val property = universe.getClass(moduleName).properties[AUTO_SELECT_WHEN]
            val requirement = (property as? RequirementValue)?.value ?: return@forEach
            if (requirement.isMetBy { metric -> countConfigured(metric, included - moduleName) }) {
              next.add(moduleName)
            }
          }
      included
          .filter { it in modules }
          .flatMap { source -> constructivelySelectedModules(source, included) }
          .filter { target -> target !in explicitlyExcluded }
          .forEach(next::add)
      if (next == included) break
      included = next
    }
    require(seenSelections.last() == included) {
      "Module defaults do not converge: ${seenSelections.joinToString(" -> ")}"
    }

    included
        .filter { it in modules }
        .flatMap { source -> constructivelySelectedModules(source, included) }
        .forEach { target ->
          require(target !in explicitlyExcluded) {
            "active Module provenance selects excluded Module $target"
          }
        }

    cards
        .filter { it.className in explicitlyIncluded }
        .forEach { card ->
          cardCompatibilityRequirement(card)?.let { requirement ->
            require(
                requirement.isMetBy { metric -> countConfigured(metric, included - card.className) }
            ) {
              "configured content ${card.className} is unavailable: $requirement"
            }
          }
        }
    marsMapDefinitions.forEach { map ->
      (listOf(map.className) + map.areas.map { area -> area.className })
          .filter { it in explicitlyIncluded }
          .forEach { className ->
            contentCompatibilityRequirement(className)?.let { requirement ->
              require(
                  requirement.isMetBy { metric -> countConfigured(metric, included - className) }
              ) {
                "configured content $className is unavailable: $requirement"
              }
            }
          }
    }
    listOf(TfmClasses.MILESTONE, TfmClasses.AWARD).forEach { goalClass ->
      (explicitlyIncluded intersect goalClassNames(goalClass)).forEach { goalName ->
        goalCompatibilityRequirement(goalName, goalClass)?.let { requirement ->
          require(requirement.isMetBy { metric -> countConfigured(metric, included - goalName) }) {
            "configured class $goalName is unavailable: $requirement"
          }
        }
      }
    }

    val moduleNames = included.filterTo(linkedSetOf()) { it in modules }
    val colonyNames = colonyTileClassNames
    val individualNames = included - moduleNames
    val individualSelections = linkedMapOf<ClassName, Boolean>()
    individualNames.forEach { individualSelections[it] = true }
    (explicitlyExcluded - modules.keys).forEach { individualSelections[it] = false }
    addExactGoalSelections(
        individualSelections,
        goalClassNames(TfmClasses.MILESTONE),
        explicitlyIncluded,
    )
    addExactGoalSelections(
        individualSelections,
        goalClassNames(TfmClasses.AWARD),
        explicitlyIncluded,
    )
    val classSelections =
        individualSelections
            .filterKeys { it !in canonicalPlayerNames }
            .mapTo(linkedSetOf()) { (className, included) -> ClassSelection(className, included) }
    val initialTypes =
        individualNames.filter { it in colonyNames }.mapTo(linkedSetOf(), ::initialColonyTileType)
    if (canonicalPlayerNames.size == 1 && initialTypes.isNotEmpty()) {
      initialTypes.add(SOLO_COLONIES_SETUP.of(canonicalPlayerNames.single().expression))
    }
    val selectedByModules =
        moduleNames
            .flatMap { modules.getValue(it) }
            .filter { it.included && it.appliesTo(included, universe) }
            .mapTo(hashSetOf(), ClassSelection::className)
    if (MULTIPLAYER_MODE in moduleNames) {
      requireGoalPoolSize(
          moduleNames,
          explicitlyIncluded,
          explicitlyExcluded,
          TfmClasses.MILESTONE,
      )
      requireGoalPoolSize(
          moduleNames,
          explicitlyIncluded,
          explicitlyExcluded,
          TfmClasses.AWARD,
      )
    }
    require(individualNames.intersect(colonyNames).all { it in selectedByModules }) {
      "initial ColonyTiles must be provided by a selected Module"
    }
    return GamePremise(
        this,
        moduleNames,
        classSelections,
        initialTypes,
        configuredPlayerNames,
        config,
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
                  requirement.isMetBy { metric ->
                    countConfigured(metric, configuredClassNames - gain.target)
                  }
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

  private fun requireGoalPoolSize(
      moduleNames: Set<ClassName>,
      explicitlyIncluded: Set<ClassName>,
      explicitlyExcluded: Set<ClassName>,
      goalClass: ClassName,
  ) {
    val knownGoals = goalClassNames(goalClass)
    val explicitlySelected = explicitlyIncluded intersect knownGoals
    val selectedGoals =
        ((explicitlySelected.takeIf { it.isNotEmpty() }
            ?: moduleNames
                .flatMap { modules.getValue(it) }
                .filter(ClassSelection::included)
                .mapTo(linkedSetOf(), ClassSelection::className)) intersect knownGoals) -
            explicitlyExcluded
    require(selectedGoals.size >= MINIMUM_GOAL_POOL_SIZE) {
      "a multiplayer game requires at least $MINIMUM_GOAL_POOL_SIZE $goalClass classes; " +
          "found ${selectedGoals.size}: ${selectedGoals.sortedBy(ClassName::toString)}"
    }
  }

  /** Catalog-known concrete subclasses of the ordinary Pets `ColonyTile` class. */
  public val colonyTileClassNames: Set<ClassName> by lazy {
    val colonyTile = universe.findClass(COLONY_TILE) ?: return@lazy emptySet()
    colonyTile.allSubclasses().filterNot { it.abstract }.mapTo(linkedSetOf()) { it.className }
  }

  private fun initialColonyTileType(className: ClassName) =
      requireNotNull(
              universe
                  .resolve(COLONY_TILE_SELECTION.of(className.classExpression()))
                  .singleConcreteSubtype(NoGameState)
          ) {
            "ColonyTileSelection<Class<$className>> must have exactly one concrete representation"
          }
          .expression

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

  private fun addExactGoalSelections(
      selections: MutableMap<ClassName, Boolean>,
      goalNames: Set<ClassName>,
      explicitlyIncluded: Set<ClassName>,
  ) {
    val selectedNames = explicitlyIncluded intersect goalNames
    if (selectedNames.isEmpty()) return
    goalNames.forEach { selections[it] = it in selectedNames }
  }

  private fun goalClassNames(goalClass: ClassName): Set<ClassName> =
      bundles
          .flatMap { bundle -> bundleClassesBelow(bundle, goalClass) }
          .mapTo(linkedSetOf(), ClassDeclaration::className)

  private fun bundleClassesBelow(
      bundle: Bundle,
      superclass: ClassName,
      directOnly: Boolean = false,
      includeAbstract: Boolean = false,
  ): List<ClassDeclaration> =
      bundle.explicitClassDeclarations.filter { declaration ->
        (includeAbstract || !declaration.abstract) &&
            if (directOnly) declaration.supertypes.any { it.className == superclass }
            else isSubtypeOf(declaration.className, superclass)
      }

  // CLASS DECLARATIONS

  internal open val contributedClassDeclarations: List<ClassDeclaration> by lazy {
    val explicit = explicitClassDeclarations.map(FollowModeNeutralizer::neutralize)
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

  final override val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    val declarations = systemClassDeclarations.toList() + contributedClassDeclarations
    try {
      declarations.distinct().associateByStrict { declaration ->
        validateSystemDeclaration(declaration)
        declaration.className
      }
    } catch (e: IllegalArgumentException) {
      throw PetException("Multiple class declarations must be identical: ${e.message}")
    }
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
    owner.marsMapDefinitions
        .singleOrNull { it.className == moduleName }
        ?.let { map ->
          return buildSet {
            add(ClassSelection(map.className))
            map.areas.forEach { area -> add(ClassSelection(area.className)) }
            map.defaultMilestones
                .takeIf { it.isNotEmpty() }
                ?.let { names ->
                  val goals = names.map(::classDeclaration)
                  addGoals(
                      goals,
                      TfmClasses.MILESTONE,
                      parse("MultiplayerMode, MAX 0 Milestone"),
                  )
                }
            map.defaultAwards
                .takeIf { it.isNotEmpty() }
                ?.let { names ->
                  val goals = names.map(::classDeclaration)
                  addGoals(
                      goals,
                      TfmClasses.AWARD,
                      parse("MultiplayerMode, MAX 0 Award"),
                  )
                }
          }
        }
    val ordinaryCards =
        if (moduleName in owner.moduleContentSelections) {
          null
        } else {
          owner.moduleCardClassNames[moduleName]?.let { names ->
            cards.filterTo(linkedSetOf()) { it.className in names }
          }
        }
    val contentSelections =
        owner.moduleContentSelections[moduleName]
            ?: if (moduleName == owner.bundleName) {
              setOf(
                  BundleContentSelection(
                      owner.bundleName,
                      if (ordinaryCards == null) setOf(Kind.CARDS, Kind.COLONY_TILES)
                      else setOf(Kind.COLONY_TILES),
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
    owner.moduleClassExclusions[moduleName].orEmpty().forEach { className ->
      selections.add(ClassSelection(className, included = false))
    }
    ordinaryCards?.let { cards ->
      selections.addCards(cards)
      selections.addCardResourceRoots(owner.moduleCardClassNames.getValue(moduleName))
    }
    return selections
  }

  private fun selectionsFrom(
      bundle: Bundle,
      selection: BundleContentSelection,
  ): Set<ClassSelection> = buildSet {
    val kinds = selection.kinds
    if (Kind.CARDS in kinds) {
      val selectedCards = bundleCards(bundle)
      addCards(selectedCards)
      addCardResourceRoots(bundle.cardResourceClassNames)
    }
    if (Kind.MAPS in kinds) {
      bundle.marsMapDefinitions.forEach { map ->
        val requirement = contentCompatibilityRequirement(map.className)
        add(ClassSelection(map.className, requirement = requirement))
        map.areas.forEach { area ->
          add(
              ClassSelection(
                  area.className,
                  requirement =
                      Requirement.join(
                          requirement,
                          contentCompatibilityRequirement(area.className),
                      ),
              )
          )
        }
      }
    }
    if (Kind.MILESTONES in kinds) {
      val goals = bundleClassesBelow(bundle, TfmClasses.MILESTONE, directOnly = true)
      addGoals(goals, TfmClasses.MILESTONE)
    }
    if (Kind.AWARDS in kinds) {
      val goals = bundleClassesBelow(bundle, TfmClasses.AWARD, directOnly = true)
      addGoals(goals, TfmClasses.AWARD)
    }
    if (Kind.COLONY_TILES in kinds) {
      bundle.explicitClassDeclarations
          .filter { declaration ->
            !declaration.abstract &&
                (isSubtypeOf(declaration.className, COLONY_TILE) ||
                    isSubtypeOf(declaration.className, COLONY_TILE_SELECTION))
          }
          .mapTo(this) { declaration -> ClassSelection(declaration.className) }
    }
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

  private fun MutableSet<ClassSelection>.addGoals(
      goals: Collection<ClassDeclaration>,
      goalClass: ClassName,
      sharedRequirement: Requirement? = null,
  ) {
    goals.mapTo(this) { declaration ->
      ClassSelection(
          declaration.className,
          requirement =
              Requirement.join(
                  sharedRequirement,
                  goalAutomaticSelectionRequirement(declaration, goalClass),
              ),
      )
    }
  }

  private fun automaticSelectionRequirement(card: PetClass): Requirement? {
    return Requirement.join(
        PRELUDE_DECK_ONLY.takeIf { cardBack(card)?.className == TfmClasses.PRELUDE_CARD },
        cardBundleCompatibilityRequirement(card),
    )
  }

  private fun goalAutomaticSelectionRequirement(
      declaration: ClassDeclaration,
      goalClass: ClassName,
  ): Requirement? =
      Requirement.join(
          Requirement.join(
              if (goalClass == TfmClasses.AWARD) MULTIPLAYER_ONLY else null,
              declaration.invariants.fold<Requirement, Requirement?>(null, Requirement::join),
          ),
          bundleCompatibilityRequirement(declaration.className, listOf(declaration)),
      )

  private fun goalCompatibilityRequirement(
      className: ClassName,
      goalClass: ClassName,
  ): Requirement? =
      Requirement.join(
          if (goalClass == TfmClasses.AWARD) MULTIPLAYER_ONLY else null,
          bundleCompatibilityRequirement(className, listOf(classDeclaration(className))),
      )

  private fun cardCompatibilityRequirement(card: PetClass): Requirement? =
      cardBundleCompatibilityRequirement(card)

  private fun cardBundleCompatibilityRequirement(card: PetClass): Requirement? {
    return bundleCompatibilityRequirement(
        card.className,
        listOf(classDeclaration(card.className)),
    )
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

  private fun isSubtypeOf(className: ClassName, possibleSupertype: ClassName): Boolean {
    if (className == possibleSupertype) return true
    return classDeclaration(className).supertypes.any { supertype ->
      isSubtypeOf(supertype.className, possibleSupertype)
    }
  }

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

  private fun bundleCards(bundle: Bundle): Set<PetClass> {
    val names = bundle.explicitClassDeclarations.mapTo(hashSetOf(), ClassDeclaration::className)
    return cards.filterTo(linkedSetOf()) { it.className in names }
  }

  private val cardsByClassName by lazy {
    cards.associateByStrict(PetClass::className)
  }

  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.firstOrNull { it.className == name }
          ?: throw IllegalArgumentException("No `$name` in this Catalog")

  public open val marsMapDefinitions: Set<MarsMapDefinition> = emptySet()

  // CUSTOM CLASSES

  override val explicitClassDeclarations: Set<ClassDeclaration> = emptySet()

  override val customClasses: Set<CustomClass> = emptySet()

  public companion object {
    /** Returns one Catalog containing the unique contributions from [catalogs]. */
    public fun compose(vararg catalogs: TfmCatalog): TfmCatalog = Composite(*catalogs)

    private val MODULE_CLASS = cn("Module")
    private val PLAYER_CLASS = cn("Player")
    private val MULTIPLAYER_MODE = cn("MultiplayerMode")
    private val TAG_CLASS = cn("Tag")
    private val COLONY_TILE = cn("ColonyTile")
    private val COLONY_TILE_SELECTION = cn("ColonyTileSelection")
    private val PRELUDE_DECK_ONLY: Requirement = parse("PreludeDeck")
    private val SOLO_COLONIES_SETUP = cn("SoloColoniesSetup")
    private val MULTIPLAYER_ONLY: Requirement = parse("MultiplayerMode")
    private const val MINIMUM_GOAL_POOL_SIZE = 3
  }

  /** One Catalog assembled from several providers before it is exposed to callers. */
  public open class Composite(vararg catalogs: TfmCatalog) : TfmCatalog() {
    private val catalogs: List<TfmCatalog> = catalogs.toList()

    final override val bundles: List<Bundle> = catalogs.flatMap(TfmCatalog::bundles)

    override val displayNamesByLanguage: Map<String, Map<ClassName, String>> = run {
      val combined = mutableMapOf<String, MutableMap<ClassName, String>>()
      catalogs.forEach { catalog ->
        catalog.displayNamesByLanguage.forEach { (language, names) ->
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

    override val explicitClassDeclarations: Set<ClassDeclaration> =
        catalogs.flatMapTo(linkedSetOf(), Catalog::explicitClassDeclarations)

    override val marsMapDefinitions: Set<MarsMapDefinition> =
        catalogs.flatMapTo(linkedSetOf(), TfmCatalog::marsMapDefinitions)

    override val customClasses: Set<CustomClass> =
        catalogs.flatMapTo(linkedSetOf(), Catalog::customClasses)

    override val routines: Map<String, Routine> = buildMap {
      catalogs.forEach { catalog ->
        catalog.routines.forEach { (name, routine) ->
          require(put(name, routine) == null) { "Multiple Routines named $name" }
        }
      }
    }
  }
}
