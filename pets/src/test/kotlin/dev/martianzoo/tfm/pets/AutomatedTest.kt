package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.Effect.Trigger
import dev.martianzoo.tfm.testlib.PetsGenerator
import org.junit.jupiter.api.Test

class AutomatedTest {

  val FACTOR = 1

  @Test
  fun expressions() {
    val gen = PetsGenerator()
    // gen.printTestStrings<QuantifiedExpression>(10)
    gen.goNuts<QuantifiedExpression>(2000 * FACTOR)
  }

  @Test
  fun triggers() {
    val gen = PetsGenerator(0.6)
    // gen.printTestStrings<Effect.Trigger>(10)
    gen.goNuts<Trigger>(1000 * FACTOR)
  }

  @Test
  fun predicates() {
    val gen = PetsGenerator()
    // gen.printTestStrings<Predicate>(10)
    gen.goNuts<Predicate>(5000 * FACTOR)
    // gen.printTestStringOfEachLength<Predicate>(80)
  }

  @Test
  fun instructions() {
    val gen = PetsGenerator()
    // gen.printTestStrings<Instruction>(10)
    gen.goNuts<Instruction>(2000 * FACTOR)
    // gen.printTestStringOfEachLength<Instruction>(80)
  }

  @Test
  fun effects() {
    val gen = PetsGenerator(0.9)
    // gen.printTestStrings<Effect>(10)
    gen.goNuts<Effect>(2000 * FACTOR)
    // gen.printTestStringOfEachLength<Effect>(80)
  }

  @Test
  fun costs() {
    val gen = PetsGenerator()
    // gen.printTestStrings<Action.Cost>(10)
    gen.goNuts<Action.Cost>(1000 * FACTOR)
  }

  @Test
  fun actions() {
    val gen = PetsGenerator()
    // gen.printTestStrings<Action>(10)
    gen.goNuts<Action>(2000)
    // gen.printTestStringOfEachLength<Action>(80)
  }
}
