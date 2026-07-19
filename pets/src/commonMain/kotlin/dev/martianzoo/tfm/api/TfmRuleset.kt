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

  /**
   * Returns the card definition having the full name [name]. If there are multiple, one must be
   * marked as `replaces` the other. (Not done, see #49)
   */
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

  public companion object {
    /** Returns a ruleset containing all contributions from each of [rulesets]. */
    public fun compose(vararg rulesets: TfmRuleset): TfmRuleset = Composite(*rulesets)
  }

  /** A ruleset containing all contributions from [rulesets]. */
  public open class Composite(vararg rulesets: TfmRuleset) : TfmRuleset() {
    public val rulesets: List<TfmRuleset> = rulesets.toList()

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
