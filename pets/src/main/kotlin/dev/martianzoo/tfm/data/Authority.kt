package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.Instruction.CustomInstruction
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

/**
 * A source of data about Terraforming Mars components. This project provides
 * one, called `Canon`, containing only officially published materials.
 */
abstract class Authority {
  fun declaration(name: String): ClassDeclaration {
    val decl: ClassDeclaration? = allClassDeclarations[name]
    require(decl != null) { "no class declaration by name $name" }
    return decl
  }

  val allDefinitions : List<Definition> by lazy {
    listOf<Definition>() +
        cardDefinitions +
        milestoneDefinitions +
        actionDefinitions +
        mapAreaDefinitions.values.flatten()
  }

  /** Note that not every type returned here will automatically be loaded. */
  val allClassDeclarations: Map<String, ClassDeclaration> by lazy {
    val list: List<ClassDeclaration> =
        explicitClassDeclarations +
        extraClassDeclarationsFromCards.values +
        allDefinitions.map { it.asClassDeclaration }
    list.associateByStrict { it.className }
  }

  abstract val explicitClassDeclarations: Collection<ClassDeclaration>

  abstract val actionDefinitions: Collection<ActionDefinition>

  val actionsByComponentName: Map<String, ActionDefinition> by lazy {
    toMapByComponentName(actionDefinitions)
  }

  abstract val cardDefinitions: Collection<CardDefinition>

  val cardsByComponentName by lazy {
    toMapByComponentName(cardDefinitions)
  }

  abstract val mapAreaDefinitions: Map<String, Grid<MapAreaDefinition>>

  val mapAreasByComponentName by lazy {
    toMapByComponentName(mapAreaDefinitions.values.flatten())
  }

  abstract val milestoneDefinitions: Collection<MilestoneDefinition>

  val milestonesByComponentName by lazy {
    toMapByComponentName(milestoneDefinitions)
  }

  // val awardDefinitions: Map<String, AwardDefinition>
  // val colonyTileDefinitions: Map<String, ColonyTileDefinition>

  abstract val customInstructions: Map<String, CustomInstruction>

  private val extraClassDeclarationsFromCards: Map<String, ClassDeclaration> by lazy {
    cardDefinitions.flatMap { it.extraComponents }.associateBy { it.className }
  }

  private fun <D : Definition> toMapByComponentName(thing: Collection<D>): Map<String, D> =
      thing.associateByStrict { it.className }
}
