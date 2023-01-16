package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.ActionDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.Definition
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict

/**
 * A source of data about Terraforming Mars components. This project provides
 * one, called `Canon`, containing only officially published materials.
 */
abstract class Authority {
  fun declaration(name: ClassName): ClassDeclaration {
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
  val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    val list: List<ClassDeclaration> =
        explicitClassDeclarations +
        extraClassDeclarationsFromCards.values +
        allDefinitions.map { it.asClassDeclaration }
    list.associateByStrict { it.name }
  }

  abstract val explicitClassDeclarations: Collection<ClassDeclaration>

  abstract val actionDefinitions: Collection<ActionDefinition>

  val actionsByClassName: Map<ClassName, ActionDefinition> by lazy {
    associateByClassName(actionDefinitions)
  }

  abstract val cardDefinitions: Collection<CardDefinition>

  val cardsByClassName by lazy {
    associateByClassName(cardDefinitions)
  }

  abstract val mapAreaDefinitions: Map<String, Grid<MapAreaDefinition>>

  abstract fun mapAreaDefinition(name: String): Grid<MapAreaDefinition>

  abstract val milestoneDefinitions: Collection<MilestoneDefinition>

  val milestonesByClassName by lazy {
    associateByClassName(milestoneDefinitions)
  }

  // val awardDefinitions: Map<String, AwardDefinition>
  // val colonyTileDefinitions: Map<String, ColonyTileDefinition>

  abstract fun customInstructions(): Collection<CustomInstruction>

  val customInstructionsByName: Map<String, CustomInstruction> =
      customInstructions().associateByStrict { it.functionName }

  private val extraClassDeclarationsFromCards: Map<ClassName, ClassDeclaration> by lazy {
    cardDefinitions.flatMap { it.extraClasses }.associateBy { it.name }
  }

  private fun <D : Definition> associateByClassName(thing: Collection<D>): Map<ClassName, D> =
      thing.associateByStrict { it.name }
}
