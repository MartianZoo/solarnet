package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class EnglishPlayerInstructionTest {
  private val players =
      object : TfmCatalog() {
        override val explicitClassDeclarations: Set<ClassDeclaration> =
            parseClasses("CLASS Green : Player\nCLASS Red : Player").toSet()
      }
  private val catalog = TfmCatalog(Canon, players)
  private val describers = Describers(catalog.classTable, TerraformingMarsDescribers.descriptions)
  private val english = English(catalog.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  fun addressesTheSelectedPlayerAcrossChoicesAndProduction() {
    english.describe(parse<InstructionTree>("3! Plant<Green> OR Ok"), cn("Green")) shouldBe
        "You may gain 3 plants."
    english.describe(
        parse<InstructionTree>("3! Production<Green, Class<Plant>>"),
        cn("Green"),
    ) shouldBe "Increase your plant production 3 steps."
    english.describe(parse<InstructionTree>("3! Plant<Red>"), cn("Green")) shouldContain
        "Plant<Red>"
  }

  @Test
  fun preservesCardTargetsAndTransferDirection() {
    for ((source, expected) in
        listOf(
            "-3! Microbe<Psychrophiles<Green>>" to "-3! Microbe<Psychrophiles>",
            "Plant<Green FROM Red>!" to "Plant FROM Plant<Red>!",
            "Plant<Red FROM Green>!" to "Plant<Red> FROM Plant!",
        )) {
      playerRelativeInstruction(parse(source), cn("Green"), describers) shouldBe
          parse<InstructionTree>(expected)
    }
  }

  @Test
  fun keepsThePerformerDistinctFromResourceOwnership() {
    english.describe(parse<InstructionTree>("OxygenStep! BY Green"), cn("Green")) shouldBe
        "Raise oxygen 1 step (performed by you)."
    english.describe(parse<InstructionTree>("OxygenStep! BY Admin"), cn("Green")) shouldBe
        "Raise oxygen 1 step (performed by Admin)."
    english.describe(parse<InstructionTree>("3! Plant<Red> BY Green"), cn("Green")) shouldContain
        "Plant<Red>"
  }

  @Test
  fun preservesDistinctPlayerRolesAndExistingLexicalOwners() {
    english.describe(
        parse<InstructionTree>("MyResourceWasRemoved<Green, Player>: Plant<Green>"),
        cn("Green"),
    ) shouldBe "If a player removed your resources this generation, gain 1 plant."
    val scoped = parse<InstructionTree>("EACH Me@Player { Plant<Green>, Heat }")
    playerRelativeInstruction(scoped, cn("Green"), describers) shouldBe scoped
  }

  @Test
  fun keepsConcreteTargetsInTaskMessages() {
    for ((source, expected) in
        listOf(
            "Colony<Green, Titan>!" to "Place a colony on Titan.",
            "PartyDelegate<Green, Scientists>!" to "Place a delegate in Scientists.",
            "Trade<Green, Titan>!" to "Trade with Titan.",
            "CopyProductionBox<Psychrophiles<Green>>!" to
                "Copy the production box in the direct effect of Psychrophiles.",
            "UseAction<Green, Psychrophiles<Green>, Action1>!" to "Use Action1 on Psychrophiles.",
            "3! Microbe<Psychrophiles<Green>>" to "Add 3 microbes to Psychrophiles.",
            "UseAction<Green, GreeneryProject, Action1>!" to "Use GreeneryProject.",
            "Colony<Green, ColonyTile(HAS MAX 0 Colony<Green>)>!" to
                "Place a colony on a colony tile with none of your colonies.",
        )) {
      english.describe(parse<InstructionTree>(source), cn("Green")) shouldBe expected
    }
    english.describe(
        parse<InstructionTree>("3! PartyDelegate<Red, Scientists>"),
        cn("Green"),
    ) shouldContain "["
  }

  @Test
  fun rendersOptionalChangesTypedHoldersAndExplicitSpending() {
    for ((source, expected) in
        listOf(
            "-3? Plant<Green>" to "You may remove up to 3 plants.",
            "3? Microbe<Psychrophiles<Green>>" to "You may add up to 3 microbes to Psychrophiles.",
            "-3? Microbe<Psychrophiles<Green>>" to
                "You may remove up to 3 microbes from Psychrophiles.",
            "3! Microbe<ResourceHolder<Green, Class<Microbe>>>" to "Add 3 microbes to any card.",
            "-3! Microbe<ResourceHolder<Green, Class<Microbe>>>" to
                "Remove 3 microbes from any of your cards.",
            "Microbe<ResourceCard<Green, Class<Microbe>>(HAS ScienceTag)>!" to
                "Add 1 microbe to a science card.",
            "3! Pay<Green, Class<Plant>> FROM Plant<Green>" to "Spend 3 plants.",
            "3! PayFromCard<Psychrophiles<Green>> FROM Microbe<Psychrophiles<Green>>" to
                "Spend 3 microbes from Psychrophiles.",
        )) {
      english.describe(parse<InstructionTree>(source), cn("Green")) shouldBe expected
    }
    for (source in
        listOf(
            "Microbe<ResourceHolder<Red, Class<Microbe>>>!",
            "3! Pay<Green, Class<Plant>> FROM Plant<Red>",
            "3! PayFromCard<Psychrophiles<Green>> FROM Microbe<Ants<Green>>",
        )) {
      english.describe(parse<InstructionTree>(source), cn("Green")) shouldContain "["
    }
  }

  @Test
  fun rendersActionChoicesAndKeepsSequencesInsideTheirAlternative() {
    for ((source, expected) in
        listOf(
            "StandardAction<Green>! OR Pass<Green>!" to "Take an action or pass.",
            "StandardAction<Green>?" to "You may take an action.",
            "UseCardAction<Psychrophiles<Green>, Action1>!" to "Use Action1 on Psychrophiles.",
            "UseStandardProject<Green, PowerPlantProject>!" to "Use PowerPlantProject.",
            "UseAction<Green, Psychrophiles<Green>, Action1>?" to
                "You may use Action1 on Psychrophiles.",
            "Plant<Green>! OR (Heat<Green>!, Steel<Green>!)" to
                "Gain 1 plant or gain 1 heat and 1 steel.",
            "ProjectCard<Green, Hand> FROM Microbe<Psychrophiles<Green>>!" to
                "Remove 1 microbe from Psychrophiles to draw 1 card.",
            "Plant<Green>! OR (Heat<Green>! THEN Steel<Green>!)" to
                "Gain 1 plant or (gain 1 heat, then gain 1 steel).",
        )) {
      english.describe(parse<InstructionTree>(source), cn("Green")) shouldBe expected
    }
  }

  @Test
  fun omitsAuditsButKeepsUnknownWorkAndPaymentObligations() {
    english.describe(parse<InstructionTree>("Audit<Green>!"), cn("Green")) shouldBe ""
    english.describe(parse<InstructionTree>("Ok"), cn("Green")) shouldBe "Do nothing."
    english.describe(listOf(parse<Action>("2 Steel -> Audit"))) shouldBe "Spend 2 steel."
    english.describe(
        parse<InstructionTree>(
            "-ProjectCard<Green, Selecting>! THEN Audit<Green>! THEN 3! Plant<Green>"
        ),
        cn("Green"),
    ) shouldBe "Discard 1 card from the selection to gain 3 plants."
    english.describe(parse<InstructionTree>("Audit<Green>!, 3! Plant<Green>"), cn("Green")) shouldBe
        "Gain 3 plants."
    english.describe(
        parse<InstructionTree>("-2! Steel<Green> THEN Audit<Green>!"),
        cn("Green"),
    ) shouldBe "Lose 2 steel."
    english.describe(
        parse<InstructionTree>("3! Owed<Green, Class<Plant>> THEN Billing<Green>!"),
        cn("Green"),
    ) shouldContain "Billing"
  }

  @Test
  fun keepsRewardsUnconditionalAfterOptionalDiscards() {
    english.describe(parse<InstructionTree>("-ProjectCard? THEN 3 Plant")) shouldBe
        "You may discard up to 1 card, then gain 3 plants."
    english.describe(
        parse<InstructionTree>(
            "-3? ProjectCard<Green, Hand> THEN Audit<Green>! THEN 3! Plant<Green>"
        ),
        cn("Green"),
    ) shouldBe "You may discard up to 3 cards, then gain 3 plants."
    english.describe(
        parse<InstructionTree>("-ProjectCard<Green, Hand>! THEN 3! Plant<Green>"),
        cn("Green"),
    ) shouldBe "Discard 1 card to gain 3 plants."
  }

  @Test
  fun distinguishesHandOperationsFromOtherCardLocationsAndOwners() {
    for ((source, expected) in
        listOf(
            "3! ProjectCard<Green, Hand>" to "Draw 3 cards.",
            "-3! ProjectCard<Green, Hand>" to "Discard 3 cards.",
            "-3? ProjectCard<Green, Hand>" to "You may discard up to 3 cards.",
            "-3! ProjectCard<Green, Selecting>" to "Discard 3 cards from the selection.",
            "-3? ProjectCard<Green, Selecting>" to
                "You may discard up to 3 cards from the selection.",
            "-ProjectCard<Green, Revealed>!" to "Discard 1 card from the revealed cards.",
            "3! ProjectCard<Green, Selecting>" to "Look at 3 cards.",
            "3! ProjectCard<Green, Hand> FROM ProjectCard<Green, Selecting>" to
                "Keep 3 cards from the selection.",
        )) {
      english.describe(parse<InstructionTree>(source), cn("Green")) shouldBe expected
    }
    for (source in
        listOf(
            "ProjectCard<Red, Hand>!",
            "ProjectCard<Green, Revealed>!",
        )) {
      english.describe(parse<InstructionTree>(source), cn("Green")) shouldContain "["
    }
  }
}
