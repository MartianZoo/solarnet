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
internal class PlayChoices(val prompt: Prompt, val choose: (Prompt.Option) -> Unit) {
  fun find(names: List<String>): Prompt.Option? =
      if (!prompt.enabled) null else prompt.options.firstOrNull { it.enabled && it.label in names }
}

internal val LocalPlayChoices = compositionLocalOf<PlayChoices?> { null }

/** A physical widget accepts the same exact option text as the Play list. */
@Composable
internal fun choiceModifier(
    names: List<String>,
    modifier: Modifier = Modifier,
    onOtherClick: (() -> Unit)? = null,
): Modifier {
  val choices = LocalPlayChoices.current
  val option = choices?.find(names)
  if (option == null)
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
      .clickable { choices.choose(option) }
}
