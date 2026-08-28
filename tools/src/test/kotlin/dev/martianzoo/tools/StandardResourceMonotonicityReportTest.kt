package dev.martianzoo.tools

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import kotlin.test.Test
import kotlin.test.assertTrue

internal class StandardResourceMonotonicityReportTest {
  @Test
  internal fun scansCountScaledInstructionsAfterDeprodification() {
    val probe =
        object : TfmCatalog() {
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
            catalog = TfmCatalog.Composite(Canon, probe),
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
