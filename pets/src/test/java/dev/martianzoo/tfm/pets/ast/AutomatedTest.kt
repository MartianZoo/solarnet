package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Test

private class AutomatedTest {
  val factor = 10

  @Test
  fun scaledExpressions() {
    val gen = PetGenerator()
    gen.goNuts<ScaledExpression>(200 * factor)
  }

  @Test
  fun metrics() {
    val gen = PetGenerator(0.7)
    gen.goNuts<Metric>(500 * factor)
  }

  @Test
  fun triggers() {
    val gen = PetGenerator(0.6)
    gen.goNuts<Trigger>(100 * factor)
  }

  @Test
  fun requirements() {
    val gen = PetGenerator()
    gen.goNuts<Requirement>(500 * factor)
    // gen.printTestStringOfEachLength<Requirement>(60)
  }

  @Test
  fun instructions() {
    val gen = PetGenerator()
    gen.goNuts<Instruction>(200 * factor)
    // gen.printTestStringOfEachLength<Instruction>(60)
  }

  @Test
  fun effects() {
    val gen = PetGenerator(0.9)
    gen.goNuts<Effect>(200 * factor)
    // gen.printTestStringOfEachLength<Effect>(60)
  }

  @Test
  fun costs() {
    val gen = PetGenerator()
    gen.goNuts<Action.Cost>(100 * factor)
  }

  @Test
  fun actions() {
    val gen = PetGenerator()
    gen.goNuts<Action>(200 * factor)
    // gen.printTestStringOfEachLength<Action>(60)
  }
}
