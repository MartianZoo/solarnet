package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.testlib.PetsGenerator
import org.junit.jupiter.api.Test

const val FACTOR = 2

class AutomatedTest {

  @Test
  fun expressions() {
    val gen = PetsGenerator()
    gen.goNuts<QuantifiedExpression>(200 * FACTOR)
  }

  @Test
  fun triggers() {
    val gen = PetsGenerator(0.6)
    gen.goNuts<Trigger>(100 * FACTOR)
  }

  @Test
  fun requirements() {
    val gen = PetsGenerator()
    gen.goNuts<Requirement>(500 * FACTOR)
    // gen.printTestStringOfEachLength<Requirement>(60)
  }

  @Test
  fun instructions() {
    val gen = PetsGenerator()
    gen.goNuts<Instruction>(200 * FACTOR)
    // gen.printTestStringOfEachLength<Instruction>(60)
  }

  @Test
  fun effects() {
    val gen = PetsGenerator(0.9)
    gen.goNuts<Effect>(200 * FACTOR)
    // gen.printTestStringOfEachLength<Effect>(60)
  }

  @Test
  fun costs() {
    val gen = PetsGenerator()
    gen.goNuts<Action.Cost>(100 * FACTOR)
  }

  @Test
  fun actions() {
    val gen = PetsGenerator()
    gen.goNuts<Action>(200 * FACTOR)
    // gen.printTestStringOfEachLength<Action>(60)
  }
}
