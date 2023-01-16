package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.ActionDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.util.Grid

class FakeAuthority(classes: List<ClassDeclaration> = listOf()) : Authority() {
  override fun mapAreaDefinition(name: String) = TODO()
  override val explicitClassDeclarations = classes
  override val mapAreaDefinitions = mapOf<String, Grid<MapAreaDefinition>>()
  override val actionDefinitions = listOf<ActionDefinition>()
  override val cardDefinitions = listOf<CardDefinition>()
  override val milestoneDefinitions = listOf<MilestoneDefinition>()
  override fun customInstructions() = listOf<CustomInstruction>()
}
