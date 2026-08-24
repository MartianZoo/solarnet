package dev.martianzoo.tools

import dev.martianzoo.data.ClassSelection
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import kotlin.test.Test
import kotlin.test.assertTrue

internal class StandardResourceMonotonicityReportTest {
  @Test
  internal fun scansCountScaledInstructionsAfterDeprodification() {
    val probe =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS ProductionPerProbe {
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
}
