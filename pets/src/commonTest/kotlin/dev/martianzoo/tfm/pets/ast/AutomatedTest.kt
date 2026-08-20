package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression
import dev.martianzoo.tfm.testlib.PetGenerator
import kotlin.test.Test

internal class AutomatedTest {
  @Test
  fun expressions() {
    PetGenerator().goNuts<Expression>()
  }

  @Test
  fun scaledExpressions() {
    PetGenerator().goNuts<ScaledExpression>()
  }

  @Test
  fun metrics() {
    PetGenerator(0.7).goNuts<Metric>()
  }

  @Test
  fun triggers() {
    PetGenerator(0.6).goNuts<Trigger>()
  }

  @Test
  fun requirements() {
    val gen = PetGenerator()
    gen.goNuts<Requirement>()
    // gen.printTestStringOfEachLength<Requirement>(60)
  }

  @Test
  fun instructionTrees() {
    val gen = PetGenerator()
    gen.goNuts<InstructionTree>()
    // gen.printTestStringOfEachLength<InstructionTree>(60)
  }

  @Test
  fun effects() {
    val gen = PetGenerator(0.9)
    gen.goNuts<Effect>()
    // gen.printTestStringOfEachLength<Effect>(60)
  }

  @Test
  fun costs() {
    PetGenerator().goNuts<Action.Cost>()
  }

  @Test
  fun actions() {
    val gen = PetGenerator()
    gen.goNuts<Action>()
    // gen.printTestStringOfEachLength<Action>(60)
  }
}
