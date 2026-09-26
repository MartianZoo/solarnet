package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.displayName
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.fake.FakeCanon
import java.io.File

private object EnglishTextComparisonGenerator {
  @JvmStatic
  fun main(args: Array<String>) {
    require(args.size == 1)
    val output = File(args.single()).also { it.mkdirs() }
    val catalog = TfmCatalog.compose(Canon, FakeCanon)
    val english = English(catalog.classTable, TerraformingMarsDescribers.descriptions)
    val cards =
        EnglishCardTextData.parse(readEnglishCardText("english-published-wording-evidence.tsv"))
    val goals =
        EnglishTextData.parse(readEnglishCardText("english-goal-published-wording-evidence.tsv"))
    val events =
        EnglishTextData.parse(
            readEnglishCardText("english-global-event-published-wording-evidence.tsv")
        )
    val categories =
        listOf(
            Category("corporations", "Corporations", "CorporationCard"),
            Category("projects", "Projects", "ProjectCard"),
            Category("preludes", "Preludes", "PreludeCard"),
            Category("milestones", "Milestones", "Milestone", card = false),
            Category("awards", "Awards", "Award", card = false),
            Category("global-events", "Global events", "GlobalEvent", card = false),
            Category("other-cards", "Other cards", "CardBack"),
        )
    val components =
        (Canon.cards.map { it.className } +
                cards.keys +
                catalog.classTable.allClassNames.filter { name ->
                  val component = catalog.classTable.getClass(name)
                  !component.abstract &&
                      categories
                          .filterNot { it.card }
                          .any {
                            component.isSubtypeOf(catalog.classTable.getClass(cn(it.superclass)))
                          }
                })
            .distinct()
            .map(catalog.classTable::getClass)
    val grouped = components.groupBy { component ->
      categories.first { category ->
        val candidate = if (category.card) cardBack(component) else component
        candidate?.isSubtypeOf(catalog.classTable.getClass(cn(category.superclass))) == true
      }
    }
    // The evidence retains Thawer's printed identity; only its replay variant is modeled.
    val evidenceNames = mapOf(cn("FakeThawer") to cn("Thawer"))
    val missingComponents =
        (cards.keys + goals.keys + events.keys) -
            components
                .flatMap {
                  listOf(it.className, evidenceNames[it.className] ?: it.className)
                }
                .toSet()
    require(missingComponents.isEmpty()) { "Evidence has unlisted components: $missingComponents" }
    val bundleNames =
        listOf(
            "TerraformingMars",
            "CorporateEraExpansion",
            "TharsisMap",
            "HellasMap",
            "ElysiumMap",
            "VenusNextExpansion",
            "Prelude1CardPack",
            "ColoniesExpansion",
            "TurmoilExpansion",
            "PromoCardPack",
            "MilestonesAwardsExpansion",
            "Prelude2CardPack",
            "AmazonisMap",
            "VastitasMap",
            "UtopiaMap",
            "CimmeriaMap",
        )
    val bundles =
        Canon.bundles.sortedBy { bundle ->
          bundleNames.indexOf(bundle.bundleName.toString()).takeIf { it >= 0 } ?: bundleNames.size
        }
    val provenance =
        bundles
            .flatMap { bundle ->
              bundle.explicitClassDeclarations.map {
                it.className to displayName(Canon, bundle.bundleName)
              }
            }
            .toMap()
    val groupOrder = bundles.map { displayName(Canon, it.bundleName) } + "Replay-only models"
    val sourceOrder =
        bundles
            .flatMap { it.explicitClassDeclarations }
            .map { it.className }
            .withIndex()
            .associate { it.value to it.index }
    val indexRows = mutableListOf<String>()
    categories.forEach { category ->
      val entries = grouped[category] ?: return@forEach
      val evidenceFile =
          when (category.id) {
            "milestones",
            "awards" -> "english-goal-published-wording-evidence.tsv"
            "global-events" -> "english-global-event-published-wording-evidence.tsv"
            else -> "english-published-wording-evidence.tsv"
          }
      var missing = 0
      val document = buildString {
        appendLine("# ${category.title}: printed and generated wording")
        appendLine()
        appendLine("[All categories](README.md) · ${entries.size} entries")
        appendLine()
        appendLine(
            "Printed text: [wording evidence](../../src/jvm/dev/martianzoo/tfm/text/$evidenceFile)."
        )
        appendLine(
            "Generated text comes directly from the current English renderer. See the [reading notes](README.md#reading-the-comparisons)."
        )
        entries
            .groupBy { provenance[it.className] ?: "Replay-only models" }
            .toSortedMap(compareBy { groupOrder.indexOf(it) })
            .forEach { (bundle, members) ->
              appendLine()
              appendLine("## $bundle")
              members
                  .sortedBy { sourceOrder[it.className] ?: Int.MAX_VALUE }
                  .forEach { component ->
                    val name = component.className
                    val printedCard = cards[name]
                    val printedText =
                        (if (category.id == "global-events") events else goals)[
                            evidenceNames[name] ?: name]
                    val printedName = printedCard?.englishName ?: printedText?.englishName
                    val title = printedName ?: displayName(catalog, name)
                    val hasEvidence =
                        if (category.card) printedCard != null else printedText != null
                    if (!hasEvidence) missing++
                    appendLine()
                    appendLine("### ${escape(title)}")
                    appendLine()
                    appendLine("Class: `$name`")
                    appendLine()
                    if (category.card) {
                      val rendering = english.renderCard(component)
                      appendLine("| | Bottom | Top |")
                      appendLine("| --- | --- | --- |")
                      appendLine(
                          "| Printed text | ${cell(printedCard?.bottom)} | ${cell(printedCard?.top)} |"
                      )
                      appendLine(
                          "| Generated text | ${cell(rendering.bottom)} | ${cell(rendering.top)} |"
                      )
                    } else {
                      val rendering =
                          if (category.id == "global-events") english.renderGlobalEvent(component)
                          else english.renderGoal(component)
                      appendLine("| | Text |")
                      appendLine("| --- | --- |")
                      appendLine("| Printed text | ${cell(printedText?.text)} |")
                      appendLine("| Generated text | ${cell(rendering.text)} |")
                    }
                    appendLine()
                    appendLine("Pets declaration:")
                    appendLine()
                    appendLine("```pets")
                    appendLine(catalog.classDeclaration(name))
                    appendLine("```")
                  }
            }
      }
      File(output, "${category.id}.md").writeText(document)
      indexRows += "| [${category.title}](${category.id}.md) | ${entries.size} | $missing |"
    }
    File(output, "README.md")
        .writeText(
            buildString {
              appendLine("# English wording comparisons")
              appendLine()
              appendLine(
                  "Printed wording, current generated English, and authored Pets, grouped by category and source expansion."
              )
              appendLine()
              appendLine("| Category | Entries | Missing printed transcription |")
              appendLine("| --- | ---: | ---: |")
              indexRows.forEach(::appendLine)
              appendLine()
              appendLine("## Reading the comparisons")
              appendLine()
              appendLine(
                  "**Not transcribed** means there is no row for that class in the published evidence corpus. An em dash means an existing row or generated region has no text. Square-bracketed Pets in generated text are unresolved renderer fragments, not printed wording."
              )
              appendLine()
              appendLine(
                  "Coverage includes every canonical card, milestone, award, and global event, plus the abstract Beginner Corporation, replay-only card models named in the published evidence, and replay-only goals. Thawer’s printed evidence is paired with `FakeThawer`, its only modeled variant. Replay-only models are listed separately; they may deliberately differ from the physical cards. Classes distinguish names shared by different milestone or award variants."
              )
              appendLine()
              appendLine(
                  "Card tables retain the bottom/top text regions. Costs, tags, fixed victory-point icons, and other icon-only information are outside those text regions. Global events compare resolution text only. Pets blocks serialize each authored class declaration with normalized formatting; they are not transcriptions of the physical component. Inherited behavior is declared on the named superclass (including the five Beginner Corporation copies)."
              )
              appendLine()
              appendLine(
                  "These are all categories with published wording corpora and dedicated renderers currently available. Colony tiles, political parties, standard projects, and map bonuses do not yet have comparison corpora here."
              )
              appendLine()
              appendLine("## Regenerate")
              appendLine()
              appendLine("From the repository root:")
              appendLine()
              appendLine("```sh")
              appendLine("./gradlew :tfm-text:writeEnglishTextComparisons")
              appendLine("```")
              appendLine()
              appendLine(
                  "This renders directly from the current model, without relying on saved generated-text snapshots. Printed wording is read only from the published evidence TSVs."
              )
            }
        )
    println(
        "Wrote ${grouped.size} comparison documents covering ${components.size} entries to $output"
    )
  }

  private fun cell(text: String?): String =
      text?.let { escape(it).ifEmpty { "—" } } ?: "Not transcribed"

  private fun escape(text: String): String =
      text
          .replace("&", "&amp;")
          .replace("\\", "\\\\")
          .replace("|", "\\|")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("[", "\\[")
          .replace("]", "\\]")
          .replace("*", "\\*")
          .replace("_", "\\_")
          .replace("`", "\\`")
          .replace("\n", "<br>")

  private data class Category(
      val id: String,
      val title: String,
      val superclass: String,
      val card: Boolean = true,
  )
}
