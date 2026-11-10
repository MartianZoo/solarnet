package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.cards.cardnames.RoboticWorkforce
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class CopyProductionBoxTest : TfmSandboxTest() {
  @Test
  internal fun `a production box with an external EACH binding reports the authoring error`() {
    // CopyProductionBox reads authored card metadata from the catalog, so premise-only
    // declarations cannot exercise this diagnostic.
    val fixture =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
        CLASS ConditionalBuilding : AutomatedCard {
          cost = 0
          HAS MAX 1 ConditionalBuilding
          HAS =1 BuildingTag<This>
          This: EACH Class<@StandardResource> { \
            PROD[@StandardResource / (Class<@StandardResource> OR ProdOffset<Class<@StandardResource>>) - Production<Class<@StandardResource>>] \
          }
        }
                  """
                  )
                  .toSet()
        }
    game = setUpGame(canonicalPremise(catalog = TfmCatalog(Canon.withPlayers(2), fixture)))
    val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    p1.runOperation("9 MC, 2 ProjectCard")
    admin.exMachina("ActionPhase FROM SetupPhase")
    p1.inTurn {
      doTasks(*playCardTasks("ConditionalBuilding"))
      autoExecNow(EAGER)
    }

    val error =
        shouldThrow<ExpressionException> {
          p1.inTurn {
            doTasks(
                *playCardTasks("$RoboticWorkforce"),
                "9 Pay<> FROM MC",
                "$RoboticWorkforce FROM ProjectCard",
                "CopyProductionBox<ConditionalBuilding>",
            )
          }
        }
    error.message shouldContain "PROD inside EACH"
    p1.assertProds(
        1 to "MC",
        1 to "Steel",
        1 to "Titanium",
        1 to "Plant",
        1 to "Energy",
        1 to "Heat",
    )
  }
}
