package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.Definition
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

/**
 * A source of data about Terraforming Mars components. This project provides one, called `Canon`,
 * containing only officially published materials.
 */
public abstract class Authority {

  public open val allBundles: Set<String> by lazy { allDefinitions.map { it.bundle }.toSet() }

  // CLASS DECLARATIONS

  /** Returns the class declaration having the full name [name]. */
  public fun classDeclaration(name: ClassName): ClassDeclaration {
    val decl: ClassDeclaration? = allClassDeclarations[name]
    require(decl != null) { "no class declaration by name $name" }
    return decl
  }

  /**
   * Every class declarations this authority knows about, including explicit ones and those
   * converted from [Definition]s.
   */
  public val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    val extraFromCards = cardDefinitions.flatMap { it.extraClasses }

    val list =
        explicitClassDeclarations + allDefinitions.map { it.asClassDeclaration } + extraFromCards
    list.associateByStrict { it.name }
  }

  public val allClassNames: Set<ClassName> by lazy { allClassDeclarations.keys }

  /**
   * All class declarations that were provided directly in source form (i.e., `CLASS Foo...`) as
   * opposed to being converted from [Definition] objects.
   */
  public abstract val explicitClassDeclarations: Collection<ClassDeclaration>

  /** Everything implementing [Definition] this authority knows about. */
  public val allDefinitions: List<Definition> by lazy {
    listOf<Definition>() +
        cardDefinitions +
        milestoneDefinitions +
        standardActionDefinitions +
        marsMapDefinitions +
        marsMapDefinitions.flatMap { it.areas }
  }

  // CARDS

  /**
   * Returns the card definition having the full name [name]. If there are multiple, one must be
   * marked as `replaces` the other. (TODO)
   */
  public fun card(name: ClassName): CardDefinition = cardsByClassName[name]!!

  /** Every card definition this authority knows about. */
  public abstract val cardDefinitions: Collection<CardDefinition>

  /** A map from [ClassName] to [CardDefinition], containing all cards known to this authority. */
  public val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    cardDefinitions.associateByStrict { it.className }
  }

  // STANDARD ACTIONS

  public fun action(name: ClassName): StandardActionDefinition =
      standardActionDefinitions.first { it.className == name }

  public abstract val standardActionDefinitions: Collection<StandardActionDefinition>

  // MARS MAPS

  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.first { it.className == name }

  public abstract val marsMapDefinitions: Collection<MarsMapDefinition>

  // MILESTONES

  public fun milestone(name: ClassName): MilestoneDefinition = milestonesByClassName[name]!!

  public abstract val milestoneDefinitions: Collection<MilestoneDefinition>

  public val milestonesByClassName: Map<ClassName, MilestoneDefinition> by lazy {
    milestoneDefinitions.associateByStrict { it.className }
  }

  // AWARDS

  // COLONY TILES

  // CUSTOM INSTRUCTIONS

  public fun customInstruction(functionName: String): CustomInstruction {
    return customInstructions
        .firstOrNull { it.functionName == functionName }
        .also { require(it != null) { "no instruction named $$functionName" } }!!
  }

  public abstract val customInstructions: Collection<CustomInstruction>

  // HELPERS

  /**
   * An authority providing nothing; intended for tests. Subclass it to supply any needed
   * declarations and definitions.
   */
  public open class Empty : Authority() {
    override val explicitClassDeclarations = listOf<ClassDeclaration>()
    override val cardDefinitions = listOf<CardDefinition>()
    override val marsMapDefinitions = listOf<MarsMapDefinition>()
    override val milestoneDefinitions = listOf<MilestoneDefinition>()
    override val standardActionDefinitions = listOf<StandardActionDefinition>()
    override val customInstructions = listOf<CustomInstruction>()
  }

  /**
   * An authority providing almost nothing, just a single (empty) Mars map, which is in some code
   * paths required.
   */
  public open class Minimal : Empty() {
    override val allBundles = setOf("B", "M")
    override val marsMapDefinitions =
        listOf(MarsMapDefinition(cn("FakeTharsis"), "M", Grid.empty()))
  }
}
