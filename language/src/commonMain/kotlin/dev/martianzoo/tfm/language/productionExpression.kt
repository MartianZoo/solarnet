package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Prod

internal fun lowerProductionSyntax(instructionTree: InstructionTree): InstructionTree =
    productionSyntaxLowerer.transformInstructionTree(instructionTree)

internal fun lowerProductionSyntax(action: Action): Action =
    productionSyntaxLowerer.transformAction(action)

internal fun lowerProductionSyntax(requirement: Requirement): Requirement =
    productionSyntaxLowerer.transformRequirement(requirement)

private val productionSyntaxLowerer by lazy { Prod.deprodify(Canon.classTable) }
