package dev.martianzoo.pets.ast

import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.testlib.PetGenerator
import kotlin.test.Test

internal class AutomatedTest {
  @Test
  internal fun expressions() {
    PetGenerator().goNuts<Expression>()
  }

  @Test
  internal fun scaledExpressions() {
    PetGenerator().goNuts<ScaledExpression>()
  }

  @Test
  internal fun metrics() {
    PetGenerator(0.7).goNuts<Metric>()
  }

  @Test
  internal fun triggers() {
    PetGenerator(0.6).goNuts<Trigger>()
  }

  @Test
  internal fun requirements() {
    val gen = PetGenerator()
    gen.goNuts<Requirement>()
    // gen.printTestStringOfEachLength<Requirement>(60)
  }

  @Test
  internal fun instructionTrees() {
    val gen = PetGenerator()
    gen.goNuts<InstructionTree>()
    // gen.printTestStringOfEachLength<InstructionTree>(60)
  }

  @Test
  internal fun effects() {
    val gen = PetGenerator(0.9)
    gen.goNuts<Effect>()
    // gen.printTestStringOfEachLength<Effect>(60)
  }

  @Test
  internal fun costs() {
    PetGenerator().goNuts<Action.Cost>()
  }

  @Test
  internal fun actions() {
    val gen = PetGenerator()
    gen.goNuts<Action>()
    // gen.printTestStringOfEachLength<Action>(60)
  }
}
