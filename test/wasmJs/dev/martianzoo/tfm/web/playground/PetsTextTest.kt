package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.Effect
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PetsTextTest {
  private val players = listOf("Blue", "Red")

  @Test
  fun effectsAndTasksTrayEachTypeRatherThanTheWholeLine() {
    assertEquals(
        listOf(
            TrayRun("ProductionPhase: 2 ", null),
            TrayRun("MC", "Blue"),
            TrayRun(", 3 ", null),
            TrayRun("Plant", "Blue"),
            TrayRun("!", null),
        ),
        petsPieces("ProductionPhase: 2 MC<Blue>, 3 Plant<Blue>!", players),
    )
    assertEquals(
        listOf(
            TrayRun("StandardAction", "Blue"),
            TrayRun("! OR ", null),
            TrayRun("Pass", "Blue"),
            TrayRun("! THEN ", null),
            TrayRun("MC", "Red"),
        ),
        petsPieces("StandardAction<Blue>! OR Pass<Blue>! THEN MC<Red>", players),
    )
  }

  @Test
  fun nestedPlayerArgumentsBelongToTheCompleteOuterType() {
    assertEquals(
        listOf(TrayRun("Animal<Pets>", "Blue")),
        petsPieces("Animal<Pets<Blue>>", players),
    )
    assertEquals(
        listOf(TrayRun("ProjectCard<Hand>", "Blue")),
        petsPieces("ProjectCard<Blue, Hand>", players),
    )
    assertEquals(
        listOf(TrayRun("ProjectCard<Hand>", "Blue")),
        petsPieces("ProjectCard<Hand, Blue>", players),
    )
    assertEquals(
        listOf(TrayRun("Animal<Pets>", "Blue")),
        petsPieces("Animal<Blue, Pets<Blue>>", players),
    )
  }

  @Test
  fun effectBindingsKeepTheirScopeWhenRemovingPlayerArguments() {
    for ((text, expected) in
        listOf(
            "LikesTag<Red, Class<@Tag>>: @Tag<WildTag<Red>>" to
                listOf(
                    TrayRun("LikesTag<Class<@Tag>>", "Red"),
                    TrayRun(": ", null),
                    TrayRun("@Tag<WildTag>", "Red"),
                ),
            "BioTag<@CardFront<Black>>: Plant<Black> OR Animal<@CardFront>" to
                listOf(
                    TrayRun("BioTag<@CardFront>", "Black"),
                    TrayRun(": ", null),
                    TrayRun("Plant", "Black"),
                    TrayRun(" OR ", null),
                    TrayRun("Animal<@CardFront>", "Black"),
                ),
        )) {
      val effect = Parsing.parse<Effect>(text)
      assertEquals(
          expected,
          petsPieces(effect.toString().replace("!", ""), players + "Black", effect),
      )
    }
  }

  @Test
  fun noTrayWithoutAnUnambiguousRemovedPlayerArgument() {
    for (text in
        listOf(
            "Blue",
            "MC",
            "MC<Anyone>",
            "Animal<Blue, Pets<Red>>",
            "MC<Blue FROM Red>",
            "ProjectCard<Blue(HAS MC)>",
        )) {
      assertEquals(listOf(TrayRun(text, null)), petsPieces(text, players))
    }
    assertEquals(
        listOf(TrayRun("MC<Blue>, Plant<Blue>", null)),
        petsPieces("MC<Blue>, Plant<Blue>", emptyList()),
    )
  }
}
