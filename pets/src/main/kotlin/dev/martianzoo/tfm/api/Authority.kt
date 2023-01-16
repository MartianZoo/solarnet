package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.Definition
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.util.associateByStrict

/**
 * A source of data about Terraforming Mars components. This project provides
 * one, called `Canon`, containing only officially published materials.
 */
abstract class Authority {

// CLASS DECLARATIONS

  /** Returns the class declaration having the full name [name]. */
  fun declaration(name: ClassName): ClassDeclaration {
    val decl: ClassDeclaration? = allClassDeclarations[name]
    require(decl != null) { "no class declaration by name $name" }
    return decl
  }

  /**
   * Every class declarations this authority knows about, including explicit ones and those
   * converted from [Definition]s.
   */
  val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    val extraFromCards = cardDefinitions.flatMap { it.extraClasses }

    val list = explicitClassDeclarations +
        allDefinitions.map { it.asClassDeclaration } +
        extraFromCards
    list.associateByStrict { it.name }
  }

  /**
   * All class declarations that were provided directly in source form (i.e., `CLASS Foo...` as
   * opposed to being converted from [Definition] objects.
   */
  abstract val explicitClassDeclarations: Collection<ClassDeclaration>

  /** Everything implementing [Definition] this authority knows about. */
  val allDefinitions : List<Definition> by lazy {
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
  fun card(name: ClassName): CardDefinition = cardsByClassName[name]!!

  /** Every card definition this authority knows about. */
  abstract val cardDefinitions: Collection<CardDefinition>

  /** A map from [ClassName] to [CardDefinition], containing all cards known to this authority. */
  val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    associateByClassName(cardDefinitions)
  }

// STANDARD ACTIONS

  fun action(name: ClassName): StandardActionDefinition =
      standardActionDefinitions.first { it.name == name }

  abstract val standardActionDefinitions: Collection<StandardActionDefinition>

// MARS MAPS

  fun marsMap(name: ClassName): MarsMapDefinition = marsMapDefinitions.first { it.name == name }

  abstract val marsMapDefinitions: Collection<MarsMapDefinition>

// MILESTONES

  fun milestone(name: ClassName): MilestoneDefinition = milestonesByClassName[name]!!

  abstract val milestoneDefinitions: Collection<MilestoneDefinition>

  val milestonesByClassName: Map<ClassName, MilestoneDefinition> by lazy {
    associateByClassName(milestoneDefinitions)
  }

// AWARDS

// COLONY TILES

// CUSTOM INSTRUCTIONS

  fun customInstruction(functionName: String): CustomInstruction =
      customInstructions.first { it.functionName == functionName }

  abstract val customInstructions: Collection<CustomInstruction>

// HELPERS

  private fun <D : Definition> associateByClassName(defs: Collection<D>) =
      defs.associateByStrict { it.name }

  open class Empty : Authority() {
    override val explicitClassDeclarations = listOf<ClassDeclaration>()
    override val cardDefinitions = listOf<CardDefinition>()
    override val marsMapDefinitions = listOf<MarsMapDefinition>()
    override val milestoneDefinitions = listOf<MilestoneDefinition>()
    override val standardActionDefinitions = listOf<StandardActionDefinition>()
    override val customInstructions = listOf<CustomInstruction>()
  }

  abstract class Forwarding(val delegate: Authority) : Authority() {
    override val explicitClassDeclarations by delegate::explicitClassDeclarations
    override val standardActionDefinitions by delegate::standardActionDefinitions
    override val cardDefinitions by delegate::cardDefinitions
    override val marsMapDefinitions by delegate::marsMapDefinitions
    override val milestoneDefinitions by delegate::milestoneDefinitions
    override val customInstructions by delegate::customInstructions
  }
}
