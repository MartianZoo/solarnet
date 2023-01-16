package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.ActionDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.MapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.Grid

open class FakeAuthority() : Authority() {
  override val explicitClassDeclarations = listOf<ClassDeclaration>()
  override val mapDefinitions =
      listOf(MapDefinition(cn("FakeTharsis"), "M", Grid.empty()))
  override val actionDefinitions = listOf<ActionDefinition>()
  override val cardDefinitions = listOf<CardDefinition>()
  override val milestoneDefinitions = listOf<MilestoneDefinition>()
  override fun customInstructions() = listOf<CustomInstruction>()
}
