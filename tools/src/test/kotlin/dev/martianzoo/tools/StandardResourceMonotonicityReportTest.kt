package dev.martianzoo.tools

import dev.martianzoo.data.ClassSelection
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tools.StandardResourceMonotonicityReport.RuleLocation
import dev.martianzoo.tools.StandardResourceMonotonicityReport.RuleLocationKind.ACTION
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class StandardResourceMonotonicityReportTest {
  @Test
  fun scansCountScaledInstructionsAfterDeprodification() {
    val probe =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS ProductionPerProbe : AutoLoad {
                        HAS =1 This
                        This: PROD[Energy / Plant]
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val basePremise = StandardResourceMonotonicityReport.maximalSoloPremise()
    val premise =
        basePremise.copy(
            authority = TfmAuthority.Composite(Canon, probe),
            classSelections =
                basePremise.classSelections + ClassSelection(cn("ProductionPerProbe")),
        )

    val analysis = StandardResourceMonotonicityReport.analyze(premise)

    val probeFindings = analysis.findings.filter { it.subjectClass == "ProductionPerProbe" }
    assertTrue(
        probeFindings.any {
          it.quantity == "Plant production" &&
              it.kind == "count-scaled instruction" &&
              it.evidence == "Production<Class<Energy>> / Production<Class<Plant>>"
        },
        probeFindings.toString(),
    )
  }

  @Test
  fun separatesSoloResourceAndProductionHazards() {
    val premise = StandardResourceMonotonicityReport.maximalSoloPremise()
    Engine.newGame(premise)
    val analysis = StandardResourceMonotonicityReport.analyze(premise)

    assertTrue(
        analysis.findings.any {
          it.quantity == "Energy" &&
              it.subject == "Factorum" &&
              it.location == RuleLocation(ACTION, 1) &&
              it.kind == "maximum requirement" &&
              it.evidence == "MAX 0 Energy"
        }
    )
    assertTrue("Energy production" in analysis.quantities)
    assertFalse(
        analysis.findings.any { it.quantity == "Energy production" && it.subject == "Factorum" }
    )
    assertFalse(analysis.findings.any { it.subject == "Manutech" })
    assertFalse(analysis.findings.any { it.subject == "Pharmacy Union" })
    assertFalse(analysis.findings.any { it.subject == "Protected Habitats" })
    assertFalse(analysis.findings.any { it.subject == "Asteroid Deflection System" })
    assertFalse(
        analysis.findings.any { it.subject == "Law Suit" || it.subject == "Mons Insurance" }
    )
    assertTrue(
        analysis.opaqueUsages.any {
          it.subject == "Double Down" && it.evidence == "CopyPrelude"
        }
    )
  }
}
