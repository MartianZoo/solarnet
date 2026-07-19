package dev.martianzoo.tfm.api

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Definition
import dev.martianzoo.data.Ruleset
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.util.associateByStrict

/**
 * A Terraforming Mars ruleset. One implementation (`Canon`) is provided by the `canon` module,
 * containing only officially published materials. Others might provide fan-made or test content.
 */
public abstract class TfmRuleset : Ruleset {

  /** A minimal two-player game using the base game and Tharsis map. */
  public val SIMPLE_GAME: GameSetup by lazy { simpleGame("BM", 2) }

  /** A minimal solo game using the base game, solo mode, and Tharsis map. */
  public val SIMPLE_SOLO_GAME: GameSetup by lazy { simpleGame("BSM", 1) }

  private fun simpleGame(bundleCodes: String, players: Int): GameSetup {
    val selectedCodes = bundleCodes.toSet()
    val selectedRuleset =
        Composite(*bundles.filter { it.legacyCode?.singleOrNull() in selectedCodes }.toTypedArray())
    return GameSetup(selectedRuleset, bundleCodes, players)
  }

  /** Bundle contributions contained anywhere in this ruleset composition. */
  public open val bundles: List<Bundle> = emptyList()

  /** Returns a ruleset containing only the selected bundles and non-bundle contributions. */
  public fun resolve(selectedBundles: Set<ClassName>): TfmRuleset {
    val available = bundles.map { it.bundleName }.toSet()
    require(available.containsAll(selectedBundles)) {
      "unknown bundles: ${selectedBundles - available}; available bundles: $available"
    }
    val selected = selectedContribution(selectedBundles) ?: Empty()
    return Resolved(selected, this)
  }

  private fun selectedContribution(selectedBundles: Set<ClassName>): TfmRuleset? =
      when (this) {
        is Bundle -> takeIf { bundleName in selectedBundles }
        is Composite ->
            Composite(
                *rulesets.mapNotNull { it.selectedContribution(selectedBundles) }.toTypedArray()
            )
        else -> this
      }

  private class Resolved(
      private val selected: TfmRuleset,
      private val allKnown: TfmRuleset,
  ) : Composite(selected) {
    private val presentBundles = bundles.map { it.bundleName }.toSet()

    override val cardDefinitions: Set<CardDefinition> by lazy {
      applicable(
          selected = super.cardDefinitions,
          allKnown = allKnown.cardDefinitions,
          id = CardDefinition::id,
          replaces = CardDefinition::replaces,
          requiredBundles = CardDefinition::requiredBundles,
      )
    }

    override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
      applicable(
          selected = super.milestoneDefinitions,
          allKnown = allKnown.milestoneDefinitions,
          id = MilestoneDefinition::id,
          replaces = MilestoneDefinition::replaces,
          requiredBundles = MilestoneDefinition::requiredBundleNames,
      )
    }

    override val classDeclarationBundles: Map<ClassName, Set<ClassName>> by lazy {
      val survivingDefinitions = allDefinitions
      val survivingCards = cardDefinitions
      val contributions = mutableMapOf<ClassName, MutableSet<ClassName>>()

      selected.bundles.forEach { bundle ->
        val declarations =
            bundle.explicitClassDeclarations +
                bundle.allDefinitions
                    .filter { it in survivingDefinitions }
                    .map { it.asClassDeclaration } +
                bundle.cardDefinitions.filter { it in survivingCards }.flatMap { it.extraClasses }
        declarations.forEach { declaration ->
          contributions.getOrPut(declaration.className, ::mutableSetOf).add(bundle.bundleName)
        }
      }

      allClassNames.associateWith { contributions[it].orEmpty() }
    }

    private fun <D> applicable(
        selected: Set<D>,
        allKnown: Set<D>,
        id: (D) -> String,
        replaces: (D) -> String?,
        requiredBundles: (D) -> Set<ClassName>,
    ): Set<D> {
      val knownById = allKnown.associateByStrict(id)
      allKnown.forEach { definition ->
        replaces(definition)?.let { target ->
          require(target in knownById) { "${id(definition)} replaces unknown definition $target" }
        }
      }
      checkReplacementCycles(knownById, id, replaces)

      val applicable = selected.filter { presentBundles.containsAll(requiredBundles(it)) }
      applicable
          .mapNotNull { replacement -> replaces(replacement)?.let { it to replacement } }
          .groupBy({ it.first }, { it.second })
          .forEach { (target, replacements) ->
            require(replacements.size == 1) {
              "multiple applicable replacements for $target: ${replacements.map(id)}"
            }
          }

      val removedIds = mutableSetOf<String>()
      applicable.forEach { replacement ->
        var target = replaces(replacement)
        while (target != null && removedIds.add(target)) {
          target = replaces(knownById.getValue(target))
        }
      }
      return applicable.filterTo(linkedSetOf()) { id(it) !in removedIds }
    }

    private fun <D> checkReplacementCycles(
        knownById: Map<String, D>,
        id: (D) -> String,
        replaces: (D) -> String?,
    ) {
      knownById.values.forEach { start ->
        val path = mutableSetOf<String>()
        var current: D? = start
        while (current != null && replaces(current) != null) {
          require(path.add(id(current))) { "replacement cycle involving ${id(current)}" }
          current = knownById.getValue(replaces(current)!!)
        }
      }
    }
  }

  // CLASS DECLARATIONS

  /** Returns the class declaration having the full name [name]. */
  override fun classDeclaration(name: ClassName): ClassDeclaration =
      allClassDeclarations[name]
          ?: throw IllegalArgumentException("no class declaration by name $name")

  protected val contributedClassDeclarations: Set<ClassDeclaration> by lazy {
    explicitClassDeclarations +
        allDefinitions.map { it.asClassDeclaration } +
        cardDefinitions.flatMap { it.extraClasses }
  }

  final override val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    try {
      (systemClassDeclarations + contributedClassDeclarations).associateByStrict {
        validate(it)
        it.className
      }
    } catch (e: IllegalArgumentException) {
      throw PetException("Multiple class declarations must be identical: ${e.message}")
    }
  }

  override val classDeclarationBundles: Map<ClassName, Set<ClassName>> by lazy {
    allClassNames.associateWith { emptySet() }
  }

  private fun validate(declaration: ClassDeclaration) {
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

  override val allClassNames: Set<ClassName> by lazy { allClassDeclarations.keys }

  /** Everything implementing [Definition] this ruleset knows about. */
  override val allDefinitions: Set<Definition> by lazy {
    setOf<Definition>() +
        cardDefinitions +
        milestoneDefinitions +
        colonyTileDefinitions +
        standardActionDefinitions +
        marsMapDefinitions +
        marsMapDefinitions.flatMap { it.areas }
  }

  // DEFINITIONS

  public fun card(name: ClassName): CardDefinition =
      cardsByClassName[name] ?: throw IllegalArgumentException("No card named $name")

  public abstract val cardDefinitions: Set<CardDefinition>

  private val cardsByClassName by lazy { cardDefinitions.associateByStrict { it.className } }

  public fun action(name: ClassName): StandardActionDefinition = standardActionDefinitions.first {
    it.className == name
  }

  public abstract val standardActionDefinitions: Set<StandardActionDefinition>

  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.firstOrNull { it.className == name }
          ?: throw IllegalArgumentException("No `$name` in: ${marsMapDefinitions.classNames()}")

  public abstract val marsMapDefinitions: Set<MarsMapDefinition>

  public fun milestone(name: ClassName): MilestoneDefinition = milestoneDefinitions.first {
    it.className == name
  }

  public abstract val milestoneDefinitions: Set<MilestoneDefinition>

  public fun colonyTile(name: ClassName): ColonyTileDefinition = colonyTileDefinitions.first {
    it.className == name
  }

  public abstract val colonyTileDefinitions: Set<ColonyTileDefinition>

  // CUSTOM CLASSES

  override fun customClass(className: ClassName): CustomClass =
      customClasses.firstOrNull { it.className == className }
          ?: throw Exceptions.customClassNotFound(className)

  /** A ruleset providing no game-specific content; intended for tests. */
  public open class Empty : TfmRuleset() {
    override val explicitClassDeclarations = emptySet<ClassDeclaration>()
    override val cardDefinitions = emptySet<CardDefinition>()
    override val marsMapDefinitions = emptySet<MarsMapDefinition>()
    override val milestoneDefinitions = emptySet<MilestoneDefinition>()
    override val colonyTileDefinitions = emptySet<ColonyTileDefinition>()
    override val standardActionDefinitions = emptySet<StandardActionDefinition>()
    override val customClasses = emptySet<CustomClass>()
  }

  /** A composable contribution owned by one game bundle. */
  public abstract class Bundle(
      public val bundleName: ClassName,
      public val legacyCode: String?,
  ) : Empty() {
    final override val bundles: List<Bundle> = listOf(this)

    final override val classDeclarationBundles: Map<ClassName, Set<ClassName>> by lazy {
      val contributedNames = contributedClassDeclarations.map { it.className }.toSet()
      allClassNames.associateWith { name ->
        if (name in contributedNames) setOf(bundleName) else emptySet()
      }
    }
  }

  public companion object {
    /** Returns a ruleset containing all contributions from each of [rulesets]. */
    public fun compose(vararg rulesets: TfmRuleset): TfmRuleset = Composite(*rulesets)
  }

  /** A ruleset containing all contributions from [rulesets]. */
  public open class Composite(vararg rulesets: TfmRuleset) : TfmRuleset() {
    public val rulesets: List<TfmRuleset> = rulesets.toList()

    final override val bundles: List<Bundle> = rulesets.flatMap { it.bundles }

    override val classDeclarationBundles: Map<ClassName, Set<ClassName>> by lazy {
      rulesets
          .flatMap { it.classDeclarationBundles.entries }
          .groupBy({ it.key }, { it.value })
          .mapValues { (_, bundleSets) -> bundleSets.flatten().toSet() }
    }

    override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
      rulesets.flatMap { it.explicitClassDeclarations }.toSet()
    }

    override val cardDefinitions: Set<CardDefinition> by lazy {
      rulesets.flatMap { it.cardDefinitions }.toSet()
    }

    override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
      rulesets.flatMap { it.marsMapDefinitions }.toSet()
    }

    override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
      rulesets.flatMap { it.milestoneDefinitions }.toSet()
    }

    override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
      rulesets.flatMap { it.colonyTileDefinitions }.toSet()
    }

    override val standardActionDefinitions: Set<StandardActionDefinition> by lazy {
      rulesets.flatMap { it.standardActionDefinitions }.toSet()
    }

    override val customClasses: Set<CustomClass> by lazy {
      rulesets.flatMap { it.customClasses }.toSet()
    }
  }
}
