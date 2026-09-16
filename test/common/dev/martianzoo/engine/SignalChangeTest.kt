package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.toComponent
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SignalChangeTest {
  @Test
  internal fun signalIsOneEventButNeverLiveState() {
    val game = Engine.newGame(premise)
    val admin = game.testAgent(ADMIN)
    admin.runOperation("Observer")
    val observedCounts = mutableListOf<Int>()
    val signalType = admin.resolve("Moment")
    game.components.listenToCount(signalType, game.reader, observedCounts::add)

    val result = admin.runOperation("2 Moment!")

    observedCounts.shouldContainExactly(0)
    admin.count("Moment") shouldBe 0
    admin.count("SelfGain") shouldBe 2
    admin.count("SelfRemoval") shouldBe 2
    admin.count("ExternalGain") shouldBe 2
    admin.count("ExternalRemoval") shouldBe 2
    admin.count("EitherSignalSide") shouldBe 2
    admin.count("GainOrSourceRemoval") shouldBe 2
    admin.count("Token") shouldBe 2
    admin.count("ObservedWhileLive") shouldBe 0
    admin.count("HeardTokenWhileLive") shouldBe 0

    val signalChanges =
        result.changes.filter {
          it.change.gaining?.type == signalType || it.change.removing?.type == signalType
        }
    signalChanges.size shouldBe 1
    signalChanges.single().change shouldBe
        ComponentChange.Transmute(2, signalType.toComponent(), signalType.toComponent())
  }

  @Test
  internal fun signalFromAnotherComponentIsTransmutedThenRemovesItself() {
    val game = Engine.newGame(premise)
    val admin = game.testAgent(ADMIN)
    admin.runOperation("Observer, Fuel")
    val signalType = admin.resolve("Moment")
    val fuelType = admin.resolve("Fuel")
    val observedCounts = mutableListOf<Int>()
    game.components.listenToCount(signalType, game.reader, observedCounts::add)

    val result = admin.runOperation("Moment FROM Fuel")

    observedCounts.shouldContainExactly(0, 1, 0)
    admin.count("Moment") shouldBe 0
    admin.count("Fuel") shouldBe 0
    admin.count("SelfGain") shouldBe 1
    admin.count("SelfRemoval") shouldBe 1
    admin.count("ExternalGain") shouldBe 1
    admin.count("ExternalRemoval") shouldBe 1
    admin.count("FuelRemoved") shouldBe 1
    admin.count("EitherSignalSide") shouldBe 2
    admin.count("GainOrSourceRemoval") shouldBe 1
    admin.count("ObservedWhileLive") shouldBe 1
    result.net().shouldContain(ComponentChange.Remove(1, fuelType.toComponent()))
    result.changes
        .map { it.change }
        .filter {
          it.gaining?.type == signalType || it.removing?.type == signalType
        }
        .shouldContainExactly(
            ComponentChange.Transmute(1, signalType.toComponent(), fuelType.toComponent()),
            ComponentChange.Remove(1, signalType.toComponent()),
        )
  }

  private companion object {
    val premise =
        testGamePremise(
            """
            CLASS Moment : Signal {
              This:: SelfGain
              -This:: SelfRemoval
              This:: Token
              Token:: HeardTokenWhileLive
            }
            CLASS Observer {
              Moment:: ExternalGain
              -Moment:: ExternalRemoval
              -Fuel:: FuelRemoved
              Moment OR -Moment:: EitherSignalSide
              Moment OR -Fuel:: GainOrSourceRemoval
              Moment IF =1 Moment:: ObservedWhileLive
            }
            CLASS Fuel, SelfGain, SelfRemoval, Token, HeardTokenWhileLive
            CLASS ExternalGain, ExternalRemoval, FuelRemoved, EitherSignalSide
            CLASS GainOrSourceRemoval, ObservedWhileLive
            """,
            players = 0,
        )
  }
}
