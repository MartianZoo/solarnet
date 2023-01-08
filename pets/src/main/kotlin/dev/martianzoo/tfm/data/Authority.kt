package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.Instruction.CustomInstruction
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

/**
 * A source of data about Terraforming Mars components. This project provides
 * one, called [Canon], containing only officially published materials.
 */
abstract class Authority {
  fun declaration(name: String): ClassDeclaration {
    val decl: ClassDeclaration? = allClassDeclarations[name]
    require(decl != null) { "no class called $name" }
    return decl
  }

  /** Note that not every type will automatically be loaded. */
  val allClassDeclarations: Map<String, ClassDeclaration> by lazy {
    gatherDeclarations(
        explicitClassDeclarations,
        mapAreaDefinitions.values.flatten(),
        milestoneDefinitions,
        cardDefinitions,
        extraClassDeclarationsFromCards.values,
    )
  }

  protected abstract val explicitClassDeclarations: Collection<ClassDeclaration>

  abstract val mapAreaDefinitions: Map<String, Grid<MapAreaDefinition>>

  val mapAreasByComponentName by lazy {
    toMapByComponentName(mapAreaDefinitions.values.flatten())
  }

  abstract val cardDefinitions: Collection<CardDefinition>

  val cardsByComponentName by lazy {
    toMapByComponentName(cardDefinitions)
  }

  abstract val milestoneDefinitions: Collection<MilestoneDefinition>

  val milestonesByComponentName by lazy {
    toMapByComponentName(milestoneDefinitions)
  }

  // val colonyTileDefinitions: Map<String, ColonyTileDefinition>

  // val awardDefinitions: Map<String, AwardDefinition>

  abstract val customInstructions: Map<String, CustomInstruction>

  private val extraClassDeclarationsFromCards: Map<String, ClassDeclaration> by lazy {
    cardDefinitions.flatMap { it.extraComponents }.associateBy { it.className }
  }

  private fun toMapByComponentName(thing: Collection<Definition>) =
      thing.associateByStrict { it.className }

  private fun gatherDeclarations(vararg defs: Collection<Definition>) =
      defs.toList()
          .flatten()
          .map { it.asClassDeclaration }
          .associateByStrict { it.className }
}

