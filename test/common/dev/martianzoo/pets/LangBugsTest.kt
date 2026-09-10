package dev.martianzoo.pets

import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Passing characterizations of Pets language behavior believed to be wrong. Each test's name says
 * what currently happens; the rule it violates is stated in `docs/pets-language-spec.md`. When one
 * is fixed, move the scenario into the `Lang*Test` for its section.
 */
internal class LangBugsTest {

  /**
   * Rule L7-8 says a repeated abstract expression takes one value everywhere it appears. Writing
   * the repeated expression with an empty argument list declares the variable but never binds it,
   * so the stages are free to diverge.
   *
   * The recorded variable keeps the spelling it had before use-specific defaults were inserted —
   * `Tile<Player1>`, not the `Tile<Player1, LandArea>` the stages became — so no occurrence of it
   * is found in the elaborated instruction and no value is ever captured. Spelling the same type
   * `Tile<LandArea>`, or using a class with no gain dependency defaults, links the stages
   * correctly. `docs/agents/TYPES.md` section 4 gates changes to Type-variable identity behind an
   * observable failure; this is one.
   */
  @Test
  internal fun `an empty argument list breaks the link between THEN stages`() {
    val authored = elaborate("Tile<> THEN Tile<>")

    // Both stages elaborated to the same type, so the two occurrences are one variable...
    "$authored" shouldBe "Tile<Player1, LandArea>! THEN Tile<Player1, LandArea>!"

    // ...but the stages may still be narrowed to different tiles, which L7-8 forbids.
    elaborate("GreeneryTile<Land1> THEN OceanTile<Land1>").narrows(authored, langWorld) shouldBe
        true

    // The equivalent spellings behave correctly.
    elaborate("GreeneryTile<Land1> THEN OceanTile<Land1>")
        .narrows(elaborate("Tile<LandArea> THEN Tile<LandArea>"), langWorld) shouldBe false
    elaborate("RedToken THEN BlueToken").narrows(elaborate("Token THEN Token"), langWorld) shouldBe
        false
  }
}
