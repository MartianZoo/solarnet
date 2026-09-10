package dev.martianzoo.pets.types

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Passing characterizations of type-system behavior believed to be wrong. Each test's name says
 * what currently happens; the rule it violates is stated in `docs/type-system-spec.md`. When one is
 * fixed, move the scenario into the `Spec*Test` for its section.
 */
internal class BugsTest {

  /**
   * Rule T8-3 says a candidate substituted into a refinement should fill a dependency the authored
   * arguments left open. It instead takes the first dependency that accepts it, which may be one an
   * argument was already written into. See `docs/type-system-spec.md`, "A known gap" under T8-3.
   *
   * Fixing this is not just a matter of reserving the written keys: real cards rely on the current
   * reading, in which a written argument *constrains* the candidate rather than reserving a slot
   * away from it. `Viron`'s `ActionCard(HAS ActionUsedMarker<ActionCard(NOT Viron)>)` and Mons
   * Insurance's `Player(HAS MAX 0 This<Anyone>)` both break under the reserving rule.
   */
  @Test
  internal fun `a refinement candidate incorrectly displaces an authored argument`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2, Tharsis_2_3 }",
            "ABSTRACT CLASS Adjacency<Area, Area>",
        )
    val world = RecordingWorld(answer = true)

    table
        .resolve(te("Tharsis_2_2"))
        .narrows(table.resolve(te("Area(HAS Adjacency<Tharsis_2_2>)")), world) shouldBe true

    // The candidate should have filled the second slot, asking `Adjacency<Tharsis_2_2,
    // Tharsis_2_2>`; instead it merged into the first, and the second stayed unconstrained.
    world.questions shouldContainExactly listOf("Adjacency<Tharsis_2_2, Area>")
  }
}
