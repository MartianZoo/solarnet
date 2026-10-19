package dev.martianzoo.tfm.web.playground

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.tfm.canon.Canon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class PlaySessionTest {
  @Test
  fun displayedHexSeedRoundTripsAll32Bits() {
    assertEquals("0x00000000", formatPlaySeed(0))
    assertEquals("0xffffffff", formatPlaySeed(-1))
    assertEquals(-1, parsePlaySeed("0xffffffff"))
    assertEquals(Int.MIN_VALUE, parsePlaySeed("0x80000000"))
    assertEquals(42, parsePlaySeed("42"))
    assertNull(parsePlaySeed("0x100000000"))
    assertNull(parsePlaySeed("0xnothex"))
  }

  @Test
  fun addingACardKeepsExistingCardsInTheSameRelativeOrder() {
    val cards =
        listOf(
                "AiCentral",
                "EarthOffice",
                "NoctisCity",
                "IndustrialComplex",
                "Donation",
                "AcquiredSpaceAgency",
                "AppliedScience",
                "SpaceLanes",
            )
            .map(::cn)
    val seed = 0x1a2b3c4d
    val original = orderedDeck(cards, seed)
    val added = cn("NewCard")

    assertEquals(
        listOf(
                "Donation",
                "SpaceLanes",
                "AiCentral",
                "AcquiredSpaceAgency",
                "EarthOffice",
                "AppliedScience",
                "IndustrialComplex",
                "NoctisCity",
            )
            .map(::cn),
        original,
    )
    assertEquals(original, orderedDeck(cards + added, seed).filterNot { it == added })
    assertEquals(original, orderedDeck(cards.reversed(), seed))
  }

  private fun session(): PlaySession {
    val session =
        PlaySession(
            Canon.gamePremise(GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")),
            seed = 42,
        )
    for (back in listOf("CorporationCard", "ProjectCard")) {
      val deal =
          session.prompt().options.single { option ->
            session.world.tasks
                .getTaskData(requireNotNull(option.taskId))
                .instruction
                .descendantsOfType<Change>()
                .any { it.gaining?.className == cn(back) && it.removing == null }
          }
      session.choose(deal)
      assertNull(session.problem)
    }
    return session
  }

  @Test
  fun theSameSeedReproducesOpeningCardsAndNeutralTiles() {
    val first = session()
    val second = session()

    assertEquals(first.held(cn("Hand")), second.held(cn("Hand")))
    assertEquals(first.held(cn("Selecting")), second.held(cn("Selecting")))
    assertEquals(
        first.agents[ADMIN].list("Tile").entries.associate { it.key.toString() to it.value },
        second.agents[ADMIN].list("Tile").entries.associate { it.key.toString() to it.value },
    )
    assertEquals(4, first.agents[ADMIN].count("Tile"))
    assertTrue(first.held(cn("Selecting")).isNotEmpty())
  }

  @Test
  fun cancellingCardNamingLeavesTheOfferAndHistoryIntact() {
    val session = session()
    // Exercise the client's naming protocol without tying it to a particular setup task order.
    val task = session.agents[session.player].addTasks("-ProjectCard<Selecting>").single()
    val before = session.world.timeline.checkpoint()
    val cards = session.held(cn("Selecting"))
    session.choose(session.prompt().options.single { it.taskId == task })
    assertEquals("CANCEL ATTEMPT", session.prompt().retreat)
    assertEquals(cards.toSet(), session.prompt().options.map { it.label }.toSet())

    session.retreat()

    assertNull(session.problem)
    assertEquals(before, session.world.timeline.checkpoint())
    assertEquals(cards, session.held(cn("Selecting")))
    session.choose(session.prompt().options.single { it.taskId == task })
    session.choose(session.prompt().options.first())
    assertNull(session.problem)
    assertEquals(cards.drop(1), session.held(cn("Selecting")))
  }

  @Test
  fun clickingAnOrArmSelectsAndNarrowsTheTask() {
    val session = session()
    val task = session.agents[session.player].addTasks("Heat OR Plant").single()
    val option = session.prompt().options.single { it.taskId == task }

    assertEquals(listOf("Heat<Green>!", "Plant<Green>!"), option.arms)
    session.chooseTaskArm(option, option.arms.last())

    assertNull(session.problem)
    assertEquals(1, session.agents[session.player].count("Plant"))
    assertEquals(0, session.agents[session.player].count("Heat"))
  }

  @Test
  fun truncatingBeforeAnAutomaticDealDoesNotImmediatelyDealAgain() {
    val session = session()
    val cards = session.held(cn("Hand"))
    // A successful player command followed by an Admin deal is the same client protocol used
    // when passing into research. The separate commands must remain separately undoable.
    val task = session.agents[session.player].addTasks("Heat").single()
    session.agents[ADMIN].addTasks("Heat<Green>: ProjectCard<Green>")
    session.world.timeline.recordPosition()
    val beforeCommand = session.world.timeline.checkpoint()
    val oldStops = session.stops.size
    session.choose(session.prompt().options.single { it.taskId == task })
    assertNull(session.problem)
    assertEquals(cards.size + 1, session.held(cn("Hand")).size)
    val dealtCards = session.held(cn("Hand"))
    assertEquals(oldStops + 2, session.stops.size)
    val beforeDeal = session.stops[session.stops.lastIndex - 1]

    session.seek(beforeDeal)
    session.truncate()

    assertEquals(beforeDeal, session.world.timeline.checkpoint())
    assertEquals(cards, session.held(cn("Hand")))
    assertEquals(1, session.agents[session.player].count("Heat"))

    val continuation = session.prompt().options.single()
    assertEquals(ADMIN.toString(), continuation.who)
    session.choose(continuation)
    assertNull(session.problem)
    assertEquals(dealtCards, session.held(cn("Hand")))
    assertEquals(1, session.agents[session.player].count("Heat"))

    session.seek(beforeDeal)
    session.truncate()
    session.seek(session.stops[session.stops.lastIndex - 1])
    session.truncate()
    assertEquals(beforeCommand, session.world.timeline.checkpoint())
    assertEquals(0, session.agents[session.player].count("Heat"))
    assertEquals(cards, session.held(cn("Hand")))
  }
}
