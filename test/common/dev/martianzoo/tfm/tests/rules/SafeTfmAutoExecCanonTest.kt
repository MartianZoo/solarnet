package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.AutoExecPolicy.SAFE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.engine.DecisionSearch
import dev.martianzoo.tfm.engine.SafeTasks
import dev.martianzoo.tfm.engine.TfmAutoExec
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Checks the live Canon predicate while another choice still competes in the same action. */
internal class SafeTfmAutoExecCanonTest : TfmSandboxTest() {
  @Test
  internal fun `production income and ProdOffset loss both settle without a player choice`() {
    newTestGame()
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(1, "PROD[MC]")
    kim.autoExecPolicy = SAFE

    admin.beginOperation("ProductionPhase FROM ActionPhase")
    kim.autoExecNow(SAFE)
    kim.count("MC") shouldBe 21
    kim.tasks
        .extract { it.instruction }
        .filterIsInstance<Remove>()
        .any { it.removing.className == cn("MC") } shouldBe false
  }

  @Test
  internal fun `unplayed Pharmacy Union does not block a fixed MC gain`() {
    newTestGame(addOptions = "PharmacyUnion, MediaGroup, MiningExpedition")
    kim.exMachina("$MediaGroup")
    kim.setToExMachina(12, "MC")
    stan.setToExMachina(2, "Plant")
    kim.autoExecPolicy = SAFE

    kim.inTurn {
      doTasks("PlayProject<Class<MiningExpedition>>", "12 Pay<> FROM MC")
      kim.count("MC") shouldBe 3
      abort()
    }
  }

  @Test
  internal fun `active Pharmacy Union holds the fixed MC gain`() {
    newTestGame(addOptions = "PharmacyUnion, MediaGroup, MiningExpedition")
    kim.exMachina("$PharmacyUnion, $MediaGroup")
    kim.setToExMachina(12, "MC")
    stan.setToExMachina(2, "Plant")
    kim.autoExecPolicy = SAFE

    kim.inTurn {
      doTasks("PlayProject<Class<MiningExpedition>>", "12 Pay<> FROM MC")
      autoExecNow(SAFE)
      kim.count("MC") shouldBe 0
      abort()
    }
  }

  @Test
  internal fun `two ordinary fixed production gains run beside each other`() {
    newTestGame(addOptions = "PreludeExpansion, AntiDesertificationTechniques")
    val priorPlantProduction = kim.count("PROD[Plant]")
    val priorSteelProduction = kim.count("PROD[Steel]")
    kim.autoExecPolicy = SAFE

    kim.inTurn {
      doTask("PlayCard<Class<PreludeCard>, Class<AntiDesertificationTechniques>>")
      kim.count("PROD[Plant]") shouldBe priorPlantProduction + 1
      kim.count("PROD[Steel]") shouldBe priorSteelProduction + 1
    }
  }

  @Test
  internal fun `Manutech already in play permits a fixed production gain`() {
    newTestGame(addOptions = "PreludeExpansion, AntiDesertificationTechniques, Manutech")
    kim.exMachina("$Manutech")
    val priorPlantProduction = kim.count("PROD[Plant]")
    val priorSteelProduction = kim.count("PROD[Steel]")
    val priorPlants = kim.count("Plant")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<AntiDesertificationTechniques>>",
          "AntiDesertificationTechniques FROM PreludeCard",
      )
      val gain =
          kim.tasks
              .extract { it }
              .single {
                val target = (it.instruction as? Gain)?.gaining?.let(kim.reader::resolve)
                target?.className == cn("Production") &&
                    target.typeDependencies.any { dependency ->
                      dependency.boundType.representedClass?.className == cn("Plant")
                    }
              }
      TfmAutoExec.safeTask(game, gain) shouldBe true
      doTask("PROD[Plant]")
      doTask("Plant")
      kim.count("PROD[Plant]") shouldBe priorPlantProduction + 1
      kim.count("Plant") shouldBe priorPlants + 1
      kim.count("PROD[Steel]") shouldBe priorSteelProduction
      abort()
    }
  }

  @Test
  internal fun `solo reserve does not block the Players stock gain`() {
    newTestGame(addOptions = "Tr63SoloObjective, MiningExpedition", playerCount = 1)
    kim.setToExMachina(12, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<MiningExpedition>>",
          "12 Pay<> FROM MC",
          "MiningExpedition FROM ProjectCard",
      )
      val gain =
          kim.tasks
              .extract { it }
              .single {
                (it.instruction as? Gain)?.gaining?.className == cn("Steel")
              }
      TfmAutoExec.safeTask(game, gain) shouldBe true
      doTask("2 Steel")
      kim.count("Steel") shouldBe 2
      abort()
    }
  }

  @Test
  internal fun `Suitable Infrastructure already in play permits production gain`() {
    newTestGame(
        addOptions = "PreludeExpansion, AntiDesertificationTechniques, SuitableInfrastructure"
    )
    kim.exMachina("$SuitableInfrastructure")
    val priorProduction = kim.count("PROD[Plant]")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<AntiDesertificationTechniques>>",
          "AntiDesertificationTechniques FROM PreludeCard",
      )
      val gain =
          kim.tasks
              .extract { it }
              .single {
                val target = (it.instruction as? Gain)?.gaining?.let(kim.reader::resolve)
                target?.className == cn("Production") &&
                    target.typeDependencies.any { dependency ->
                      dependency.boundType.representedClass?.className == cn("Plant")
                    }
              }
      TfmAutoExec.safeTask(game, gain) shouldBe true
      doTask("PROD[Plant]")
      kim.count("PROD[Plant]") shouldBe priorProduction + 1
      abort()
    }
  }

  @Test
  internal fun `Biolab may draw before its independent production gain`() {
    newTestGame(addOptions = "PreludeExpansion, Biolab")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<Biolab>>",
          "Biolab FROM PreludeCard",
      )
      val draw =
          kim.tasks
              .extract { it }
              .single {
                (it.instruction as? Gain)?.gaining?.className == cn("ProjectCard")
              }
      TfmAutoExec.safeTask(game, draw) shouldBe true
      abort()
    }
  }

  @Test
  internal fun `Venus L1 Shade permits same player steps on the same track`() {
    newTestGame(addOptions = "PreludeExpansion, VenusNextExpansion, VenusL1Shade")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<VenusL1Shade>>",
          "VenusL1Shade FROM PreludeCard",
      )
      val steps =
          kim.tasks
              .extract { it }
              .filter {
                (it.instruction as? Gain)?.gaining?.className == cn("VenusStep")
              }
      steps.size shouldBe 3
      (steps.first().instruction as Gain).quantifier shouldBe AMAP
      TfmAutoExec.safeTask(game, steps.first()) shouldBe true
      abort()
    }
  }

  @Test
  internal fun `Giant Solar Collector has the same result in every short task order`() {
    newTestGame(addOptions = "PreludeExpansion, VenusNextExpansion, GiantSolarCollector")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<GiantSolarCollector>>",
          "GiantSolarCollector FROM PreludeCard",
      )
      kim.tasks.extract { it }.count { !it.deferred } shouldBe 2
      val oracle = DecisionSearch(agents, null, budget = 100)
      (oracle.uniformOutcome() != null) shouldBe true
      DecisionSearch(agents, null, budget = 1).uniformOutcome() shouldBe null
      abort()
    }
  }

  @Test
  internal fun `short search leaves different outcomes for the player`() {
    newTestGame()
    kim.addTasks("MC, -1? MC")

    DecisionSearch(agents, null, budget = 100).uniformOutcome() shouldBe null
  }

  @Test
  internal fun `development analyzer proposal preserves exhaustive Canon outcomes`() {
    newTestGame(addOptions = "PreludeExpansion, AntiDesertificationTechniques")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<AntiDesertificationTechniques>>",
          "AntiDesertificationTechniques FROM PreludeCard",
      )
      val gain =
          kim.tasks
              .extract { it }
              .single {
                val target = (it.instruction as? Gain)?.gaining?.let(kim.reader::resolve)
                target?.className == cn("Production") &&
                    target.typeDependencies.any { dependency ->
                      dependency.boundType.representedClass?.className == cn("Plant")
                    }
              }
      SafeTasks.isSafe(game, gain) shouldBe true
      val oracle = DecisionSearch(agents, null, playerPolicy = NONE)
      val all = checkNotNull(oracle.outcomes())
      oracle.outcomes { speculative -> speculative[kim.actor].doTask(gain.id) } shouldBe all
      abort()
    }
  }

  @Test
  internal fun `fixed stock removals and TR gain finish Shipment to Earth`() {
    newTestGame(addOptions = "ShipmentToEarth, EarthOffice")
    kim.exMachina("$EarthOffice")
    kim.setToExMachina(3, "Plant")
    kim.setToExMachina(3, "Steel")
    val priorTr = kim.count("TerraformRating")
    kim.autoExecPolicy = SAFE

    kim.inTurn {
      doTasks("PlayProject<Class<ShipmentToEarth>>", "17 Pay<> FROM MC")
      // The per-Earth-tag gain stays manual; after its amount resolves, the fixed changes compete.
      doTask("2 MC")
      autoExecNow(SAFE)
      kim.count("Plant") shouldBe 0
      kim.count("Steel") shouldBe 0
      kim.count("TerraformRating") shouldBe priorTr + 3
    }
  }

  @Test
  internal fun `Reds policy queues its charge after a TR gain`() {
    newTestGame(addOptions = "TurmoilExpansion, ShipmentToEarth, EarthOffice")
    kim.exMachina("$EarthOffice")
    kim.exMachina("Ruling<Reds FROM Greens>, RedsPolicy")
    kim.setToExMachina(40, "MC")
    kim.setToExMachina(3, "Plant")
    kim.setToExMachina(3, "Steel")
    val priorTr = kim.count("TerraformRating")

    kim.inTurn {
      doTasks("PlayProject<Class<ShipmentToEarth>>", "17 Pay<> FROM MC")
      doTask("ShipmentToEarth FROM ProjectCard")
      doTask("2 MC")
      val gain =
          kim.tasks
              .extract { it }
              .single {
                (it.instruction as? Gain)?.gaining?.className == cn("TerraformRating")
              }
      TfmAutoExec.safeTask(game, gain) shouldBe true
      doTask("3 TerraformRating")
      kim.count("TerraformRating") shouldBe priorTr + 3
      kim.tasks
          .extract { it.instruction }
          .filterIsInstance<Remove>()
          .any {
            it.removing.className == cn("MC")
          } shouldBe true
      abort()
    }
  }
}
