package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.toComponent
import dev.martianzoo.testsupport.count
import dev.martianzoo.testsupport.resolve
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SignalChangeTest {
  @Test
  internal fun authoredReflexiveSignalTransmutationIsNotASignalGain() {
    val admin = Engine.newGame(premise).testAgents()

    shouldThrow<ExpressionException> { admin.runSystem("Moment FROM Moment!") }
    admin.world.reader.count("Moment") shouldBe 0
    admin.world.reader.count("SelfGain") shouldBe 0
    admin.world.reader.count("SelfRemoval") shouldBe 0
  }

  @Test
  internal fun signalIsOneEventButNeverLiveState() {
    val game = Engine.newGame(premise)
    val admin = game.testAgents()
    admin.runSystem("Observer")
    val signalType = admin.world.reader.resolve("Moment")

    val result = admin.runSystem("Moment!")

    admin.world.reader.count("Moment") shouldBe 0
    admin.world.reader.count("SelfGain") shouldBe 1
    admin.world.reader.count("SelfRemoval") shouldBe 1
    admin.world.reader.count("ExternalGain") shouldBe 1
    admin.world.reader.count("ExternalRemoval") shouldBe 1
    admin.world.reader.count("EitherSignalSide") shouldBe 1
    admin.world.reader.count("GainOrSourceRemoval") shouldBe 1
    admin.world.reader.count("Token") shouldBe 1
    admin.world.reader.count("ObservedWhileLive") shouldBe 0
    admin.world.reader.count("HeardTokenWhileLive") shouldBe 0

    val signalChanges =
        result.changes.filter {
          it.change.gaining?.type == signalType || it.change.removing?.type == signalType
        }
    signalChanges.size shouldBe 1
    signalChanges.single().change shouldBe
        ComponentChange.Transmute(1, signalType.toComponent(), signalType.toComponent())
  }

  @Test
  internal fun signalFromAnotherComponentIsTransmutedThenRemovesItself() {
    val game = Engine.newGame(premise)
    val admin = game.testAgents()
    admin.runSystem("Observer, Fuel")
    val signalType = admin.world.reader.resolve("Moment")
    val fuelType = admin.world.reader.resolve("Fuel")

    val result = admin.runSystem("Moment FROM Fuel")

    admin.world.reader.count("Moment") shouldBe 0
    admin.world.reader.count("Fuel") shouldBe 0
    admin.world.reader.count("SelfGain") shouldBe 1
    admin.world.reader.count("SelfRemoval") shouldBe 1
    admin.world.reader.count("ExternalGain") shouldBe 1
    admin.world.reader.count("ExternalRemoval") shouldBe 1
    admin.world.reader.count("FuelRemoved") shouldBe 1
    admin.world.reader.count("EitherSignalSide") shouldBe 2
    admin.world.reader.count("GainOrSourceRemoval") shouldBe 1
    admin.world.reader.count("ObservedWhileLive") shouldBe 1
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
            CLASS Fuel
            CLASS SelfGain
            CLASS SelfRemoval
            CLASS Token
            CLASS HeardTokenWhileLive
            CLASS ExternalGain
            CLASS ExternalRemoval
            CLASS FuelRemoved
            CLASS EitherSignalSide
            CLASS GainOrSourceRemoval
            CLASS ObservedWhileLive
            """,
            players = 0,
        )
  }
}
