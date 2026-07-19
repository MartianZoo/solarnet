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
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.And
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.ast.Requirement.Transform
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

/**
 * A Terraforming Mars ruleset. One implementation (`Canon`) is provided by the `canon` module,
 * containing only officially published materials. Others might provide fan-made or test content.
 */
public abstract class TfmRuleset : Ruleset {

  /** Bundle contributions contained anywhere in this ruleset composition. */
  public open val bundleRulesets: List<Bundle> = emptyList()

  /**
   * Resolves this source to its always-included and explicitly selected bundle contributions.
   * Non-bundle contributions are retained.
   */
  public fun resolve(selectedBundles: Set<ClassName>): TfmRuleset {
    val available = bundleRulesets.map { it.bundleName }.toSet()
    require(available.containsAll(selectedBundles)) {
      "unknown bundles: ${selectedBundles - available}; available bundles: $available"
    }
    val selectedSource = selectedContribution(selectedBundles) ?: Empty()
    return Resolved(this, selectedSource)
  }

  private fun selectedContribution(selectedBundles: Set<ClassName>): TfmRuleset? =
      when (this) {
        is Bundle -> if (alwaysIncluded || bundleName in selectedBundles) this else null
        is Composite ->
            Composite(
                *rulesets.mapNotNull { it.selectedContribution(selectedBundles) }.toTypedArray()
            )
        else -> this
      }

  private class Resolved(
      private val underlying: TfmRuleset,
      private val selectedSource: TfmRuleset,
  ) : TfmRuleset() {
    private val presentBundles = selectedSource.bundleRulesets.map { it.bundleName }.toSet()

    override val bundleRulesets: List<Bundle> by selectedSource::bundleRulesets
    override val allBundles: Set<String> by selectedSource::allBundles
    override val explicitClassDeclarations: Set<ClassDeclaration> by
        selectedSource::explicitClassDeclarations
    override val customClasses: Set<CustomClass> by selectedSource::customClasses

    override val cardDefinitions: Set<CardDefinition> by lazy {
      applicable(selectedSource.cardDefinitions, underlying.cardDefinitions)
    }

    override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
      applicable(selectedSource.marsMapDefinitions, underlying.marsMapDefinitions)
    }

    override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
      applicable(selectedSource.milestoneDefinitions, underlying.milestoneDefinitions)
    }

    override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
      applicable(selectedSource.colonyTileDefinitions, underlying.colonyTileDefinitions)
    }

    override val standardActionDefinitions: Set<StandardActionDefinition> by lazy {
      applicable(selectedSource.standardActionDefinitions, underlying.standardActionDefinitions)
    }

    private fun <D : Definition> applicable(selected: Set<D>, allKnown: Set<D>): Set<D> {
      val knownById = allKnown.associateByStrict { it.definitionId }
      allKnown.forEach { definition ->
        definition.replacesId?.let { target ->
          require(target in knownById) {
            "${definition.definitionId} replaces unknown ${definition::class.simpleName} $target"
          }
        }
      }
      checkReplacementCycles(knownById)

      val applicable = selected.filter { it.loadRequirement?.matches(presentBundles) != false }
      applicable
          .mapNotNull { definition -> definition.replacesId?.let { it to definition } }
          .groupBy({ it.first }, { it.second })
          .forEach { (target, replacements) ->
            require(replacements.size == 1) {
              "multiple applicable replacements for $target: ${replacements.map { it.definitionId }}"
            }
          }

      val removedIds = mutableSetOf<String>()
      applicable.forEach { replacement ->
        var target = replacement.replacesId
        while (target != null && removedIds.add(target)) {
          target = knownById.getValue(target).replacesId
        }
      }
      return applicable.filterTo(mutableSetOf()) { it.definitionId !in removedIds }
    }

    private fun <D : Definition> checkReplacementCycles(knownById: Map<String, D>) {
      knownById.values.forEach { start ->
        val path = mutableSetOf<String>()
        var current: D? = start
        while (current?.replacesId != null) {
          require(path.add(current.definitionId)) {
            "replacement cycle involving ${current.definitionId}"
          }
          current = knownById.getValue(current.replacesId!!)
        }
      }
    }

    private fun Requirement.matches(presentBundles: Set<ClassName>): Boolean =
        when (this) {
          is Counting -> {
            val expression = scaledEx.expression
            require(expression.simple) { "unsupported load requirement: $this" }
            val count = if (expression.className in presentBundles) 1 else 0
            count in range
          }
          is Or -> requirements.any { it.matches(presentBundles) }
          is And -> requirements.all { it.matches(presentBundles) }
          is Transform -> error("unsupported load requirement: $this")
        }
  }

  /** Returns every bundle code (e.g. `"B"`) this ruleset has any information on. */
  override val allBundles: Set<String> by lazy { allDefinitions.map { it.bundle }.toSet() }

  // CLASS DECLARATIONS

  /** Returns the class declaration having the full name [name]. */
  override fun classDeclaration(name: ClassName): ClassDeclaration {
    val decl: ClassDeclaration? = allClassDeclarations[name]
    require(decl != null) { "no class declaration by name $name" }
    return decl
  }

  override val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    // Dedups as long as class declarations are exactly identical
    val allDeclarations: Set<ClassDeclaration> =
        explicitClassDeclarations +
            allDefinitions.map { it.asClassDeclaration } +
            cardDefinitions.flatMap { it.extraClasses }

    try {
      allDeclarations.associateByStrict {
        validate(it)
        it.className
      }
    } catch (e: IllegalArgumentException) {
      throw PetException("Multiple class declarations must be identical: ${e.message}")
    }
  }

  private fun validate(decl: ClassDeclaration) {
    when (decl.className) {
      COMPONENT -> {
        require(decl.abstract)
        require(decl.supertypes.none())
        require(decl.dependencies.none())
      }
      CLASS -> {
        require(!decl.abstract)
        require(decl.dependencies.single() == COMPONENT.expression)
      }
    }
  }

  /**
   * Every class declaration this ruleset knows about, including explicit ones and those converted
   * from [Definition]s.
   */
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

  // CARDS

  /** Returns the applicable card definition having the full name [name]. */
  public fun card(name: ClassName): CardDefinition =
      cardsByClassName[name] ?: throw IllegalArgumentException("No card named $name")

  /** Every card this ruleset knows about. */
  public abstract val cardDefinitions: Set<CardDefinition>

  /** A map from [ClassName] to [CardDefinition], containing all cards known to this ruleset. */
  internal val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    cardDefinitions.associateByStrict { it.className }
  }

  // STANDARD ACTIONS

  /** Returns the standard action/project by the given [name]. */
  public fun action(name: ClassName): StandardActionDefinition = standardActionDefinitions.first {
    it.className == name
  }

  /** Every standard action (including standard projects) this ruleset knows about. */
  public abstract val standardActionDefinitions: Set<StandardActionDefinition>

  // MARS MAPS

  /** Returns the map by the given name, e.g. `Tharsis`. */
  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.firstOrNull { it.className == name }
          ?: throw IllegalArgumentException("No `$name` in: ${marsMapDefinitions.classNames()}")

  /** Every map this ruleset knows about. */
  public abstract val marsMapDefinitions: Set<MarsMapDefinition>

  // MILESTONES

  /** Returns the milestone by the given [name]. */
  public fun milestone(name: ClassName): MilestoneDefinition = milestonesByClassName[name]!!

  /** Every milestone this ruleset knows about. */
  public abstract val milestoneDefinitions: Set<MilestoneDefinition>

  private val milestonesByClassName: Map<ClassName, MilestoneDefinition> by lazy {
    milestoneDefinitions.associateByStrict { it.className }
  }

  // AWARDS

  // COLONY TILES

  /** Returns the milestone by the given [name]. */
  public fun colonyTile(name: ClassName): ColonyTileDefinition = colonyTileByClassName[name]!!

  /** Every colony tile this ruleset knows about. */
  public abstract val colonyTileDefinitions: Set<ColonyTileDefinition>

  private val colonyTileByClassName: Map<ClassName, ColonyTileDefinition> by lazy {
    colonyTileDefinitions.associateByStrict { it.className }
  }

  // CUSTOM CLASSES

  /** Returns the custom instruction implementation having the name [className]. */
  override fun customClass(className: ClassName): CustomClass {
    return customClasses.firstOrNull { it.className == className }
        ?: throw Exceptions.customClassNotFound(className)
  }

  // HELPERS

  /**
   * A ruleset providing nothing; intended for tests. Subclass it to supply any needed declarations
   * and definitions.
   */
  public open class Empty : TfmRuleset() {
    override val explicitClassDeclarations = setOf<ClassDeclaration>()
    override val cardDefinitions = setOf<CardDefinition>()
    override val marsMapDefinitions = setOf<MarsMapDefinition>()
    override val milestoneDefinitions = setOf<MilestoneDefinition>()
    override val colonyTileDefinitions = setOf<ColonyTileDefinition>()
    override val standardActionDefinitions = setOf<StandardActionDefinition>()
    override val customClasses = setOf<CustomClass>()
  }

  /**
   * A ruleset providing almost nothing, just a single (empty) Mars map, which is in some code paths
   * required.
   */
  public open class Minimal : Empty() {
    override val allBundles = setOf("B", "M")
    override val marsMapDefinitions = setOf(MarsMapDefinition(cn("FakeTharsis"), "M", Grid.empty()))
  }

  /** A composable contribution owned by one game bundle. */
  public abstract class Bundle(
      public val bundleName: ClassName,
      public val legacyCode: String?,
      public val alwaysIncluded: Boolean = false,
      public val hasComponent: Boolean = true,
  ) : Empty() {
    final override val bundleRulesets: List<Bundle> = listOf(this)
    final override val allBundles: Set<String> = setOfNotNull(legacyCode)
  }

  public companion object {
    /** Returns a ruleset containing all contributions from each of [rulesets]. */
    public fun compose(vararg rulesets: TfmRuleset): TfmRuleset = Composite(*rulesets)
  }

  /** A ruleset containing all contributions from [rulesets]. */
  public open class Composite(vararg rulesets: TfmRuleset) : TfmRuleset() {
    public val rulesets: List<TfmRuleset> = rulesets.toList()

    final override val bundleRulesets: List<Bundle> = rulesets.flatMap { it.bundleRulesets }

    override val allBundles: Set<String> by lazy { rulesets.flatMap { it.allBundles }.toSet() }

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
