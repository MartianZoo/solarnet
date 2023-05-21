package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Test

private class AutomatedTest {
  val reps = 300 // it's been stable a while

  @Test
  fun scaledExpressions() {
    val gen = PetGenerator()
    gen.goNuts<ScaledExpression>(reps)
  }

  @Test
  fun metrics() {
    val gen = PetGenerator(0.7)
    gen.goNuts<Metric>(reps)
  }

  @Test
  fun triggers() {
    val gen = PetGenerator(0.6)
    gen.goNuts<Trigger>(reps)
  }

  @Test
  fun requirements() {
    val gen = PetGenerator()
    gen.goNuts<Requirement>(reps)
    // gen.printTestStringOfEachLength<Requirement>(60)
  }

  @Test
  fun instructions() {
    val gen = PetGenerator()
    gen.goNuts<Instruction>(reps)
    // gen.printTestStringOfEachLength<Instruction>(60)
  }

  @Test
  fun effects() {
    val gen = PetGenerator(0.9)
    gen.goNuts<Effect>(reps)
    // gen.printTestStringOfEachLength<Effect>(60)
  }

  @Test
  fun costs() {
    val gen = PetGenerator()
    gen.goNuts<Action.Cost>(reps)
  }

  @Test
  fun actions() {
    val gen = PetGenerator()
    gen.goNuts<Action>(reps)
    // gen.printTestStringOfEachLength<Action>(60)
  }
}
