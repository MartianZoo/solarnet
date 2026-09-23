package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** Wording for physical procedures that count-only Pets cannot represent. */
internal data class CardPrintedProcedureText(
    val immediate: String? = null,
    val actions: String? = null,
)

/** Sparse authored facts; all other card regions are derived from Pets. */
internal val cardPrintedProcedureTextByName: Map<ClassName, CardPrintedProcedureText> =
    mapOf(
        cn("InventorsGuild") to
            CardPrintedProcedureText(actions = "Look at 1 project card. You may buy it."),
        cn("BusinessNetwork") to
            CardPrintedProcedureText(actions = "Look at 1 project card. You may buy it."),
        cn("BusinessContacts") to
            CardPrintedProcedureText(immediate = "Look at 4 project cards. Draw 2 of them."),
        cn("InventionContest") to
            CardPrintedProcedureText(immediate = "Look at 3 project cards. Draw one of them."),
        cn("Celestic") to
            CardPrintedProcedureText(
                immediate = "Gain 42 M€. As your first action, draw 2 cards with floater icons."
            ),
        cn("ValleyTrust") to
            CardPrintedProcedureText(
                immediate =
                    "Gain 37 M€. As your first action, draw 3 prelude cards, then discard 2 prelude cards, then play a prelude card."
            ),
        cn("SagittaFrontierServices") to
            CardPrintedProcedureText(
                immediate =
                    "Gain 31 M€. Increase your energy production 1 step and your M€ production 2 steps. Draw 1 card with no tags."
            ),
        cn("AtmosphericEnhancers") to
            CardPrintedProcedureText(
                immediate =
                    "Raise temperature 2 steps, oxygen 2 steps, or Venus 2 steps. Draw 2 cards with floater icons."
            ),
        cn("VenusOrbitalSurvey") to
            CardPrintedProcedureText(
                actions =
                    "Reveal 2 project cards. Draw any Venus cards for free. You may buy each other card."
            ),
        cn("TychoMagnetics") to
            CardPrintedProcedureText(
                actions = "Spend 1 or more energy to look at X project cards and draw one of them."
            ),
        cn("CorporateArchives") to
            CardPrintedProcedureText(
                immediate = "Gain 13 M€. Look at 7 project cards. Draw 2 of them."
            ),
        cn("Merger") to
            CardPrintedProcedureText(
                immediate =
                    "Draw 4 corporation cards, then discard 3 corporation cards, then play a corporation card. Remove 42 M€."
            ),
        cn("NewPartner") to
            CardPrintedProcedureText(
                immediate =
                    "Increase your M€ production 1 step. Draw 2 prelude cards, then discard 1 prelude card, then play a prelude card."
            ),
        cn("HiTechLab") to
            CardPrintedProcedureText(
                actions = "Spend 1 or more energy to look at X project cards and draw one of them."
            ),
        cn("AsteroidDeflectionSystem") to
            CardPrintedProcedureText(
                actions =
                    "Reveal 1 project card. If it has a space tag, add 1 asteroid to this card."
            ),
        cn("PublicPlans") to
            CardPrintedProcedureText(
                immediate =
                    "Reveal any number of cards from your hand, then gain 1 M€ per revealed card."
            ),
        cn("HighCircles") to
            CardPrintedProcedureText(
                immediate =
                    "Raise your terraform rating 1 step. Draw 1 card with a party requirement. Place 2 delegates."
            ),
        cn("WgProject") to
            CardPrintedProcedureText(
                immediate = "Draw 3 prelude cards, then play one of them, then discard the other 2."
            ),
    )
