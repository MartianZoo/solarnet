package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The holder composition and trigger defaults independently of the Canon card set. */
internal class WildTagHolderTest {
  private val game =
      Engine.newGame(
          testGamePremise(
              """
              ABSTRACT CLASS TagHolder : Owned<Player>
              ABSTRACT CLASS CardFront : TagHolder {
                HAS MAX 1 This
                CLASS FirstCard { HAS =1 WildTag<This> }
                CLASS SecondCard { HAS =1 WildTag<This> }
                CLASS PrintedCard
              }
              CLASS ActionScope<Player> : Scope, System {
                HAS MAX 1 ActionScope
                This:: EACH @WildTag<Player> { WildTagElection<@WildTag, This> }
                @WildTag<Player>:: WildTagElection<@WildTag, This>
              }
              CLASS WildTag<@CardFront<@Player>> : Owned<@Player> { HAS MAX 1 This }
              CLASS WildTagElection<@WildTag<@CardFront<@Player>>, ActionScope> : TagHolder<@Player>, System {
                HAS MAX 1 This, MAX 1 Tag<This>
                LikesTag<Class<@Tag>> IF MAX 0 Tag<This>:: @Tag<WildTagElection<@WildTag, ActionScope>>
              }
              ABSTRACT CLASS Tag<TagHolder<@Player>> : Owned<@Player> {
                DEFAULT Tag<CardFront>:
                CLASS EarthTag
                CLASS ScienceTag
              }
              CLASS PrintedReward : Owned<Player>
              CLASS RemovedPrintedReward : Owned<Player>
              CLASS Benefit : Owned<Player>
              CLASS Listener : Owned<Player> {
                EarthTag<> OR ScienceTag<>:: PrintedReward
                -EarthTag<>:: RemovedPrintedReward
              }
              CLASS CountEarth : Owned<Player>, Signal {
                This:: Benefit / EarthTag
              }
              ABSTRACT CLASS LikesTag<Class<Tag>> : CardFront
              CLASS BenefitCard : LikesTag<Class<EarthTag>> {
                This: Benefit / EarthTag
              }
              """,
              players = 2,
          )
      )
  private val player = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
  private val admin = game.testAgent(ADMIN)

  init {
    admin.runOperation("ActionScope<Player1>")
  }

  @Test
  internal fun `wild holder counts without printed tag reactions and survives until cleanup`() {
    player.addTasks(
        "Listener, FirstCard, PrintedCard, EarthTag<PrintedCard>, EarthTag<WildTagElection<WildTag<FirstCard>>>, CountEarth"
    )
    player.doTasks("Listener", "FirstCard", "PrintedCard", "EarthTag<PrintedCard>")
    player.count("PrintedReward") shouldBe 1
    player.doTask("EarthTag<WildTagElection<WildTag<FirstCard>>>")
    player.count("PrintedReward") shouldBe 1
    player.count("EarthTag") shouldBe 2
    player.count("EarthTag<CardFront>") shouldBe 1
    player.count("CardFront(HAS WildTag(HAS WildTagElection(HAS EarthTag)))") shouldBe 1
    player.count("EarthTag<WildTagElection<WildTag<FirstCard>>>") shouldBe 1
    player.doTask("CountEarth")
    player.count("Benefit") shouldBe 2
    player.count("EarthTag") shouldBe 2
    admin.runOperation("-ActionScope<Player1>")
    player.count("WildTagElection") shouldBe 0
    player.count("EarthTag") shouldBe 1
    player.count("WildTag") shouldBe 1
    player.count("RemovedPrintedReward") shouldBe 0
    player.addTasks("-EarthTag<PrintedCard>")
    player.doTask("-EarthTag<PrintedCard>")
    player.count("RemovedPrintedReward") shouldBe 1
    admin.runOperation("ActionScope<Player1>")
    player.addTasks("ScienceTag<WildTagElection<WildTag<FirstCard>>>, CountEarth")
    player.doTask("ScienceTag<WildTagElection<WildTag<FirstCard>>>")
    player.count("ScienceTag<WildTagElection<WildTag<FirstCard>>>") shouldBe 1
  }

  @Test
  internal fun `a second meaning is rejected until the first meaning is cleaned up`() {
    player.addTasks(
        "FirstCard, EarthTag<WildTagElection<WildTag<FirstCard>>>, ScienceTag<WildTagElection<WildTag<FirstCard>>>"
    )
    player.doTasks("FirstCard", "EarthTag<WildTagElection<WildTag<FirstCard>>>")
    shouldThrow<LimitsException> {
      player.doTask("ScienceTag<WildTagElection<WildTag<FirstCard>>>")
    }
    player.count("EarthTag") shouldBe 1
    player.count("ScienceTag") shouldBe 0
  }

  @Test
  internal fun `greedy benefit allocation uses only unassigned wild holders`() {
    player.addTasks(
        "FirstCard, SecondCard, ScienceTag<WildTagElection<WildTag<FirstCard>>>, BenefitCard"
    )
    player.doTasks(
        "FirstCard",
        "SecondCard",
        "ScienceTag<WildTagElection<WildTag<FirstCard>>>",
        "BenefitCard",
    )
    player.count("ScienceTag") shouldBe 1
    player.count("EarthTag<WildTagElection<WildTag<SecondCard>>>") shouldBe 1
    player.doTask("Benefit")
    player.count("Benefit") shouldBe 1
    player.count("Tag<WildTagElection<WildTag>>") shouldBe 2
    admin.runOperation("-ActionScope<Player1>")
    player.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `a liked tag allocates only the acting players wilds`() {
    val other = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    other.addTasks("SecondCard")
    other.doTask("SecondCard")
    player.addTasks("FirstCard, BenefitCard")
    player.doTasks("FirstCard", "BenefitCard")
    player.count("EarthTag<WildTagElection<WildTag>>") shouldBe 1
    other.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }
}
