package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.TfmCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class OwnTransformTest {
  private fun testGamePremise(source: String, players: Int = 1): GamePremise {
    val declarations =
        Parsing.parseClasses(
            """
      ABSTRACT CLASS Player : Owner, Actor {
        HAS =1 This
        ${(1..players).joinToString("\n") { "CLASS Player$it" }}
      }
      $source
    """
                .trimIndent()
        )
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = declarations.toSet()
          override val transformHandlerFactories = TfmCatalog().transformHandlerFactories
        }
    return GamePremise(
        catalog = catalog,
        classSelections = declarations.map { ClassSelection(it.className) }.toSet(),
        playerNames = (1..players).map { ClassName.cn("Player$it") },
    )
  }

  @Test
  internal fun `use site distinguishes player count from global count of the same property`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS LocalPrize : Owned<Player>
      CLASS GlobalPrize : Owned<Player>
      CLASS Shared { score = COUNT "Score" }
      CLASS Grant : Owned<Player> {
        This: LocalPrize<Me@Player> / OWN[EVAL Shared.score], GlobalPrize<Me@Player> / EVAL Shared.score
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    p1.runOperation("2 Score<Player1>, 5 Score<Player2>")
    p1.runOperation("Grant<Player1>")
    p2.runOperation("Grant<Player2>")
    p1.count("LocalPrize<Player1>") shouldBe 2
    p1.count("LocalPrize<Player2>") shouldBe 5
    p1.count("GlobalPrize<Player1>") shouldBe 7
    p1.count("GlobalPrize<Player2>") shouldBe 7
  }

  @Test
  internal fun `use side mark survives deferred and nested property evaluation`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS Prize : Owned<Player>
      ABSTRACT CLASS Rule { score = Metric }
      CLASS Shared { score = COUNT "Score" }
      CLASS ConcreteRule : Rule { score = COUNT "EVAL Shared.score" }
      CLASS Grant : Owned<Player> {
        OWN[This: EACH @Rule { Prize / EVAL @Rule.score }]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("ConcreteRule")
    p1.runOperation("2 Score<Player1>, 5 Score<Player2>")
    p1.runOperation("Grant<Player1>")
    p2.runOperation("Grant<Player2>")
    p1.count("Prize<Player1>") shouldBe 2
    p1.count("Prize<Player2>") shouldBe 5
  }

  @Test
  internal fun `use side mark captures named fanout player`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS Prize : Owned<Player>
      CLASS Rule { score = COUNT "Score" }
      CLASS Grant : Owned<Player> {
        This: OWN[EACH Me@Player { Prize / EVAL Rule.score }]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    p1.runOperation("2 Score<Player1>, 5 Score<Player2>")
    p1.runOperation("Grant<Player1>")
    p1.count("Prize<Player1>") shouldBe 2
    p1.count("Prize<Player2>") shouldBe 5
  }

  @Test
  internal fun `explicit placement recipient survives system attribution`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Plant : Owned<Player>
      CLASS ActorPrize : Owned<Player>
      ABSTRACT CLASS Area { HAS MAX 1 This
        Placement<This, @Player>: Plant<@Player>
        Placement<This> BY @Player: ActorPrize<@Player>
      }
      CLASS Mars1 : Area
      CLASS Placement<Area> : Owned<Player>, Signal, System
      CLASS Place : Owned<Player> { This: Placement<Mars1, Me@Player> }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    game.testAgent(ADMIN).runOperation("Mars1")
    p1.runOperation("Place<Player2>")
    p1.count("Plant<Player1>") shouldBe 0
    p1.count("Plant<Player2>") shouldBe 1
    p1.count("ActorPrize") shouldBe 0
  }

  @Test
  internal fun `ordinary unowned placement can use the triggering actor`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Plant : Owned<Player>
      ABSTRACT CLASS Area { HAS MAX 1 This; Placement<This> BY @Player: Plant<@Player> }
      CLASS Mars1 : Area
      CLASS Placement<Area> : Signal
    """,
                players = 2,
            )
        )
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("Mars1")
    p2.runOperation("Placement<Mars1>")
    p2.count("Plant<Player1>") shouldBe 0
    p2.count("Plant<Player2>") shouldBe 1
  }

  @Test
  internal fun `deferred marked requirement cannot borrow the other player's score`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS Prize : Owned<Player>
      ABSTRACT CLASS Rule { requirement = Requirement }
      CLASS Qualified : Rule { requirement = HAS "2 Score" }
      CLASS Grant : Owned<Player> {
        This:: OWN[EACH @Rule { EVAL @Rule.requirement: Prize }]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("Qualified")
    p1.runOperation("2 Score<Player1>")
    p1.runOperation("Grant<Player1>")
    shouldThrow<RequirementException> { p2.runOperation("Grant<Player2>") }
    p1.count("Prize<Player1>") shouldBe 1
    p1.count("Prize<Player2>") shouldBe 0
  }

  @Test
  internal fun `a property can itself request ownership and unmarked explicit Me still works`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS Prize : Owned<Player>
      CLASS Rule {
        marked = COUNT "OWN[Score]"
        explicit = COUNT "Score<Me@Player>"
      }
      CLASS Grant : Owned<Player> {
        This: Prize<Me@Player> / EVAL Rule.marked, Prize<Me@Player> / EVAL Rule.explicit
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    p1.runOperation("2 Score<Player1>, 5 Score<Player2>")
    p1.runOperation("Grant<Player1>")
    p1.count("Prize<Player1>") shouldBe 4
  }

  @Test
  internal fun `whole effect mark scopes both subscription and result`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS LocalPrize : Owned<Player>
      CLASS GlobalPrize : Owned<Player>
      CLASS Rule : Owned<Player> {
        OWN[Score: LocalPrize]
        Score: GlobalPrize<Me@Player>
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    p1.runOperation("Rule<Player1>")
    p1.runOperation("Score<Player2>")
    p1.count("LocalPrize<Player1>") shouldBe 0
    p1.count("GlobalPrize<Player1>") shouldBe 1
    p1.runOperation("Score<Player1>")
    p1.count("LocalPrize<Player1>") shouldBe 1
    p1.count("GlobalPrize<Player1>") shouldBe 2
  }

  @Test
  internal fun `an ownerless marked rule binds its recipient from the event owner`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Plant : Owned<Player>
      CLASS GlobalPrize : Owned<Player>
      ABSTRACT CLASS Area {
        HAS MAX 1 This
        OWN[Placement<This>: Plant]
        Placement<This>: GlobalPrize<Player1>
      }
      CLASS Mars1 : Area
      CLASS Placement<Area> : Owned<Player>, Signal, System
      CLASS Place : Owned<Player> { This: Placement<Mars1, Me@Player> }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    game.testAgent(ADMIN).runOperation("Mars1")
    p1.runOperation("Place<Player2>")
    p1.count("Plant<Player1>") shouldBe 0
    p1.count("Plant<Player2>") shouldBe 1
    p1.count("GlobalPrize<Player1>") shouldBe 1
  }

  @Test
  internal fun `an ownerless marked rule uses BY when its event has no owner`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Plant : Owned<Player>
      CLASS Pulse : Signal
      CLASS Rule { OWN[Pulse: OWN[Plant]] }
    """,
                players = 2,
            )
        )
    game.testAgent(ADMIN).runOperation("Rule")
    val p2 = game.testAgent(PLAYER2)
    p2.runOperation("Pulse")
    p2.count("Plant<Player1>") shouldBe 0
    p2.count("Plant<Player2>") shouldBe 1
  }

  @Test
  internal fun `ownership composes with production and does not spread to unmarked metrics`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      ABSTRACT CLASS StandardResource : Owned<Player>
      CLASS Plant : StandardResource
      CLASS Production<Class<@StandardResource>> : Owned<Player>
      CLASS Grant : Owned<Player> { OWN[This: PROD[Plant]] }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    p1.runOperation("Grant<Player1>")
    p1.runOperation("OWN[PROD[2 Plant]]")
    p1.count("PROD[Plant<Player1>]") shouldBe 3
    p1.count("PROD[Plant<Player2>]") shouldBe 0
    p1.count("PROD[Plant]") shouldBe 3
  }

  @Test
  internal fun `only a marked subscription filters an ownerless event by the effect player`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Pulse
      CLASS LocalPrize : Owned<Player>
      CLASS GlobalPrize : Owned<Player>
      CLASS Rule : Owned<Player> {
        Pulse: GlobalPrize<Me@Player>
        OWN[X Pulse: X LocalPrize]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    p1.runOperation("Rule")
    p2.runOperation("Pulse")
    p1.count("GlobalPrize") shouldBe 1
    p1.count("LocalPrize") shouldBe 0
    p1.runOperation("2 Pulse")
    p1.count("GlobalPrize") shouldBe 3
    p1.count("LocalPrize") shouldBe 2
  }

  @Test
  internal fun `player input always owns siblings even when it already contains a mark`() {
    val game =
        Engine.newGame(
            testGamePremise("CLASS Plant : Owned<Player>\nCLASS Heat : Owned<Player>", players = 2)
        )
    val p1 = game.testAgent(PLAYER1)
    p1.runOperation("Plant, OWN[Heat]")
    p1.runOperation("OWN[Plant], Heat")
    p1.count("Plant") shouldBe 2
    p1.count("Heat") shouldBe 2
    p1.count("Plant<Player2>") shouldBe 0
    p1.count("Heat<Player2>") shouldBe 0
  }

  @Test
  internal fun `production and ownership both transform a deferred property value`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      ABSTRACT CLASS StandardResource : Owned<Player>
      CLASS Plant : StandardResource
      CLASS Heat : StandardResource
      CLASS Production<Class<@StandardResource>> : Owned<Player>
      CLASS Prize : Owned<Player>
      ABSTRACT CLASS Rule { score = Metric }
      CLASS Shared { score = COUNT "Plant"; production = COUNT "PROD[Plant]" }
      CLASS ConcreteRule : Rule { score = COUNT "EVAL Shared.score" }
      CLASS Grant : Owned<Player> {
        OWN[This: EACH @Rule { Prize / PROD[EVAL @Rule.score] }]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("ConcreteRule")
    p1.runOperation("17 Plant, PROD[2 Plant]")
    p2.runOperation("19 Plant, PROD[5 Plant]")
    p1.runOperation("Grant")
    p2.runOperation("Grant")
    p1.count("Prize") shouldBe 2
    p2.count("Prize") shouldBe 5
    p1.count("PROD[EVAL Shared.score]") shouldBe 2
    p2.count("PROD[EVAL Shared.score]") shouldBe 5
    p1.count("PROD[OWN[EVAL Shared.score]]") shouldBe 2
    shouldThrow<ExpressionException> { p1.count("PROD[EVAL Shared.production]") }
  }

  @Test
  internal fun `production preserves nonresource and numeric property scale factors`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      ABSTRACT CLASS StandardResource : Owned<Player>
      CLASS Plant : StandardResource
      CLASS Heat : StandardResource
      CLASS Production<Class<@StandardResource>> : Owned<Player>
      CLASS City : Owned<Player>
      CLASS Rule { cities = COUNT "City"; amount = 3 }
      CLASS Grant : Owned<Player> {
        OWN[This: PROD[Plant / EVAL Rule.cities], PROD[Heat / EVAL Rule.amount]]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    p1.runOperation("2 City")
    p2.runOperation("5 City")
    p1.runOperation("Grant")
    p2.runOperation("Grant")
    p1.count("PROD[Plant]") shouldBe 2
    p2.count("PROD[Plant]") shouldBe 5
    p1.count("PROD[Heat]") shouldBe 3
    p2.count("PROD[Heat]") shouldBe 3
  }

  @Test
  internal fun `an inline HAS requirement uses its candidate rather than the effect owner`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Token : Owned<Player>
      CLASS InlinePrize : Owned<Player>
      CLASS Grant : Owned<Player> {
        OWN[This: EACH Starter@Player(HAS Token) { InlinePrize<Starter@Player> }]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    game.testAgent(PLAYER2).runOperation("Token")
    p1.runOperation("Grant")
    p1.count("InlinePrize<Player2>") shouldBe 1
    p1.count("InlinePrize<Player1>") shouldBe 0
  }

  @Test
  internal fun `production works with outer ownership or an explicit trigger binding`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      ABSTRACT CLASS StandardResource : Owned<Player>
      CLASS Plant : StandardResource
      CLASS Heat : StandardResource
      CLASS Production<Class<@StandardResource>> : Owned<Player>
      CLASS Pulse : Signal
      CLASS Rule {
        OWN[Pulse: PROD[Plant]]
        OWN[Pulse BY Me@Player: PROD[OWN[Plant, Heat<Me@Player>]]]
      }
    """,
                players = 2,
            )
        )
    game.testAgent(ADMIN).runOperation("Rule")
    val p2 = game.testAgent(PLAYER2)
    p2.runOperation("Pulse")
    p2.count("PROD[Plant<Player1>]") shouldBe 0
    p2.count("PROD[Plant<Player2>]") shouldBe 2
    p2.count("PROD[Heat<Player2>]") shouldBe 1
  }

  @Test
  internal fun `rank defers a marked property until its candidate is known`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS OtherScore : Owned<Player>
      ABSTRACT CLASS Rule { HAS MAX 1 This; score = Metric }
      CLASS First : Rule { score = COUNT "Score" }
      CLASS Second : Rule { score = COUNT "OtherScore" }
      CLASS Prize<Rule> : Owned<Player>
      CLASS Grant : Owned<Player> {
        OWN[This: EACH Winner@Rule(HAS =1 (RANK Ranked@Rule { EVAL Ranked@Rule.score })) { Prize<Winner@Rule> }]
      }
    """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("First, Second")
    p1.runOperation("2 Score<Player1>, OtherScore<Player1>, Score<Player2>, 3 OtherScore<Player2>")
    p1.runOperation("Grant<Player1>")
    p2.runOperation("Grant<Player2>")
    p1.count("Prize<First, Player1>") shouldBe 1
    p1.count("Prize<Second, Player1>") shouldBe 0
    p1.count("Prize<First, Player2>") shouldBe 0
    p1.count("Prize<Second, Player2>") shouldBe 1
  }

  @Test
  internal fun `an ownerless marked rule captures its trigger player for property-only ownership`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
      CLASS Score : Owned<Player>
      CLASS Prize
      CLASS Pulse : Signal
      CLASS Rule {
        score = COUNT "Score"
        OWN[Pulse: Prize / EVAL This.score]
      }
    """,
                players = 2,
            )
        )
    game.testAgent(ADMIN).runOperation("Rule")
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    p1.runOperation("2 Score")
    p2.runOperation("5 Score")
    p1.runOperation("Pulse")
    p1.count("Prize") shouldBe 2
    p2.runOperation("Pulse")
    p1.count("Prize") shouldBe 7
  }

  @Test
  internal fun `compact changes also require an ownership binding`() {
    val table =
        testClassTable(
            """
      CLASS Land1
      CLASS Land2
      CLASS Tile<Component> : Owned
    """
        )
    val change = Parsing.parse<InstructionTree>("OWN[Tile<Land1 FROM Land2>]")
    shouldThrow<ExpressionException> { PetElaborator(table).elaborateInput(change) }
  }

  @Test
  internal fun `unmarked input leaves its owner open and OWN supplies it`() {
    val table =
        testClassTable(
            """
      ABSTRACT CLASS Player : Owner, Actor {
        CLASS Player1
        CLASS Player2
      }
      CLASS Plant : Owned<Player>
    """
        )
    val elaborator = PetElaborator(table)
    val bare = Parsing.parse<InstructionTree>("Plant")
    val marked = Parsing.parse<InstructionTree>("OWN[Plant]")
    elaborator.elaborateInput(bare, PLAYER1).toString() shouldBe "Plant!"
    elaborator.elaborateInput(marked, PLAYER1).toString() shouldBe "Plant<Player1>!"
    shouldThrow<ExpressionException> {
      elaborator.elaborateInput(marked)
    }
  }
}
