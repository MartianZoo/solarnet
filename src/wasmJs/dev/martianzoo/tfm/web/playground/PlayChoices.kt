package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

/** The live Play list supplies both the choices and their action to physical widgets. */
internal class PlayChoices(
    val prompt: Prompt,
    val choose: (Prompt.Option) -> Unit,
    val chooseArm: (Prompt.Option, String) -> Unit,
    private val currentPrompt: () -> Prompt? = { prompt },
    private val cardActionAvailable: (String, Int) -> Boolean = { _, _ -> false },
    private val projectCardAvailable: (String) -> Boolean = { false },
    private val playProjectCard: (String) -> Unit = {},
) {
  private fun refreshed(): PlayChoices? =
      currentPrompt()?.let {
        PlayChoices(
            it,
            choose,
            chooseArm,
            currentPrompt,
            cardActionAvailable,
            projectCardAvailable,
            playProjectCard,
        )
      }

  fun find(names: List<String>): Prompt.Option? =
      if (!prompt.enabled) null else prompt.options.firstOrNull { it.enabled && it.label in names }

  /** Choose both the resource type and its card through that card's counter. */
  fun resourceCard(card: String, resourceType: String, resources: Int?): (() -> Unit)? {
    if (resources == 0 && prompt.detail?.substringAfter(": ")?.trimStart()?.startsWith("-") == true)
        return null
    val typeChoice =
        if (prompt.enabled && prompt.heading == "Narrow CardResource")
            prompt.options.firstOrNull { it.enabled && actionName(it.label) == resourceType }
        else null
    val cardChoice = resourceCardChoice(card, resourceType)
    if (typeChoice == null && cardChoice == null) return null
    return {
      typeChoice?.let(choose)
      refreshed()?.let { menu -> menu.resourceCardChoice(card, resourceType)?.let(menu.choose) }
    }
  }

  private fun resourceCardChoice(card: String, resourceType: String): Prompt.Option? =
      if (!prompt.heading.startsWith("Narrow ResourceCard")) null
      else
          find(
              listOf(
                  card,
                  "Class<$card>",
                  "$resourceType<$card>",
                  "CardResource<$card>",
              )
          )

  /** A hand-card click opens PlayProject and chooses that card. */
  fun playProject(card: String): (() -> Unit)? {
    if (!projectCardAvailable(card)) return null
    return { playProjectCard(card) }
  }

  /** Select an offered action, including one still inside an unselected turn offer. */
  fun action(name: String): (() -> Unit)? {
    if (!prompt.enabled) return null
    prompt.options
        .firstOrNull { it.enabled && actionName(it.label) == name }
        ?.let { option ->
          return { choose(option) }
        }
    for (option in prompt.options) {
      if (!option.enabled) continue
      option.arms
          .firstOrNull { actionName(it) == name }
          ?.let { arm ->
            return { chooseArm(option, arm) }
          }
    }
    return null
  }

  /** A printed card action follows the menu's action, card, and slot choices in order. */
  fun cardAction(card: String, slot: Int): (() -> Unit)? {
    if (!cardActionAvailable(card, slot)) return null
    val actionChoice = action("UseCardAction")
    if (actionChoice == null && cardChoice(card) == null && slotChoice(card, slot) == null)
        return null
    return {
      actionChoice?.invoke()
      refreshed()?.let { menu -> menu.cardChoice(card)?.let(menu.choose) }
      refreshed()?.let { menu -> menu.slotChoice(card, slot)?.let(menu.choose) }
    }
  }

  private fun cardChoice(card: String): Prompt.Option? =
      if (!prompt.enabled || prompt.detail?.contains("UseCardAction") != true) null
      else prompt.options.firstOrNull { it.enabled && actionName(it.label) == card }

  private fun slotChoice(card: String, slot: Int): Prompt.Option? =
      if (
          !prompt.enabled ||
              prompt.detail?.contains("UseCardAction") != true ||
              prompt.detail?.contains(card) != true
      )
          null
      else prompt.options.firstOrNull { it.enabled && actionName(it.label) == "Action$slot" }
}

internal fun actionName(label: String): String =
    label.removePrefix("Class<").substringBefore('<').substringBefore('>').removeSuffix("!")

internal val LocalPlayChoices = compositionLocalOf<PlayChoices?> { null }

/** A physical widget accepts the same exact option text as the Play list. */
@Composable
internal fun choiceModifier(
    names: List<String>,
    modifier: Modifier = Modifier,
    action: String? = null,
    onChoice: (() -> Unit)? = null,
    onOtherClick: (() -> Unit)? = null,
): Modifier {
  val choices = LocalPlayChoices.current
  val select =
      onChoice
          ?: choices?.let { menu ->
            menu.find(names)?.let { option -> { menu.choose(option) } } ?: action?.let(menu::action)
          }
  if (select == null)
      return if (onOtherClick == null) modifier else modifier.clickable(onClick = onOtherClick)
  val palette = LocalPalette.current
  val source = remember { MutableInteractionSource() }
  val hovered by source.collectIsHoveredAsState()
  val shape = RoundedCornerShape(4.dp)
  return modifier
      .then(if (hovered) Modifier.shadow(8.dp, shape) else Modifier)
      .background(
          if (hovered) palette.accent.copy(alpha = 0.25f) else palette.accent.copy(alpha = 0.07f),
          shape,
      )
      .border(
          if (hovered) 2.dp else 1.dp,
          palette.accent.copy(alpha = if (hovered) 0.95f else 0.4f),
          shape,
      )
      .hoverable(source)
      .pointerHoverIcon(PointerIcon.Hand)
      .clickable(onClick = select)
}
