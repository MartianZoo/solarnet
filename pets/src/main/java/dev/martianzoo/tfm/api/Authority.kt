package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.Definition
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

/**
 * A source of data about Terraforming Mars components. One implementation (`Canon`) is provided by
 * the `canon` module, containing only officially published materials. Others might provide fan-made
 * content or test content.
 */
public abstract class Authority {

  /** Returns every bundle code (e.g. `"B"`) this authority has any information on. */
  public open val allBundles: Set<String> by lazy { allDefinitions.map { it.bundle }.toSet() }

  // CLASS DECLARATIONS

  /** Returns the class declaration having the full name [name]. */
  public fun classDeclaration(name: ClassName): ClassDeclaration {
    val decl: ClassDeclaration? = allClassDeclarations[name]
    require(decl != null) { "no class declaration by name $name" }
    return decl
  }

  /** TODO */
  public val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    val allDeclarations =
        explicitClassDeclarations +
            allDefinitions.map { it.asClassDeclaration } +
            cardDefinitions.flatMap { it.extraClasses }

    allDeclarations.associateByStrict {
      validate(it)
      it.className
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
        require(decl.dependencies.single() == COMPONENT.expr)
      }
    }
    decl.effects
        .flatMap { it.effect.unprocessed.descendantsOfType<Custom>() }
        .forEach { customInstruction(it.functionName) }
  }

  /**
   * Every class declaration this authority knows about, including explicit ones and those converted
   * from [Definition]s.
   */
  public val allClassNames: Set<ClassName> by lazy { allClassDeclarations.keys }

  /**
   * All class declarations that were provided "directly" in source form (i.e., `CLASS Foo...`) as
   * opposed to being converted from [Definition] objects.
   */
  public abstract val explicitClassDeclarations: Set<ClassDeclaration>

  /** Everything implementing [Definition] this authority knows about. */
  public val allDefinitions: Set<Definition> by lazy {
    setOf<Definition>() +
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
  public fun card(name: ClassName): CardDefinition =
      cardsByClassName[name] ?: throw IllegalArgumentException("No card named $name")

  /** Every card this authority knows about. */
  public abstract val cardDefinitions: Set<CardDefinition>

  /** A map from [ClassName] to [CardDefinition], containing all cards known to this authority. */
  internal val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    cardDefinitions.associateByStrict { it.className }
  }

  // STANDARD ACTIONS

  /** Returns the standard action/project by the given [name]. */
  public fun action(name: ClassName): StandardActionDefinition =
      standardActionDefinitions.first { it.className == name }

  /** Every standard action (including standard projects) this authority knows about. */
  public abstract val standardActionDefinitions: Set<StandardActionDefinition>

  // MARS MAPS

  /** Returns the map by the given name, e.g. `Tharsis`. */
  public fun marsMap(name: ClassName): MarsMapDefinition =
      marsMapDefinitions.firstOrNull { it.className == name }
          ?: throw IllegalArgumentException("No `$name` in: ${marsMapDefinitions.classNames()}")

  /** Every map this authority knows about. */
  public abstract val marsMapDefinitions: Set<MarsMapDefinition>

  // MILESTONES

  /** Returns the milestone by the given [name]. */
  public fun milestone(name: ClassName): MilestoneDefinition = milestonesByClassName[name]!!

  /** Every milestone this authority knows about. */
  public abstract val milestoneDefinitions: Set<MilestoneDefinition>

  private val milestonesByClassName: Map<ClassName, MilestoneDefinition> by lazy {
    milestoneDefinitions.associateByStrict { it.className }
  }

  // AWARDS

  // COLONY TILES

  // CUSTOM INSTRUCTIONS

  /** Returns the custom instruction implementation having the name [functionName]. */
  public fun customInstruction(functionName: String): CustomInstruction {
    return customInstructions.firstOrNull { it.functionName == functionName }
        ?: throw UserException.customInstructionNotFound(functionName)
  }

  /** Every custom instruction this authority knows about. */
  public abstract val customInstructions: Set<CustomInstruction>

  // HELPERS

  /**
   * An authority providing nothing; intended for tests. Subclass it to supply any needed
   * declarations and definitions.
   */
  public open class Empty : Authority() {
    override val explicitClassDeclarations = setOf<ClassDeclaration>()
    override val cardDefinitions = setOf<CardDefinition>()
    override val marsMapDefinitions = setOf<MarsMapDefinition>()
    override val milestoneDefinitions = setOf<MilestoneDefinition>()
    override val standardActionDefinitions = setOf<StandardActionDefinition>()
    override val customInstructions = setOf<CustomInstruction>()
  }

  /**
   * An authority providing almost nothing, just a single (empty) Mars map, which is in some code
   * paths required.
   */
  public open class Minimal : Empty() {
    override val allBundles = setOf("B", "M")
    override val marsMapDefinitions = setOf(MarsMapDefinition(cn("FakeTharsis"), "M", Grid.empty()))
  }
}
