package dev.martianzoo.tfm.web.playground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.text.EnglishInstructionTextRenderer

internal val LocalInstructionEnglish =
    staticCompositionLocalOf<EnglishInstructionTextRenderer?> { null }

/** Uses the loaded game's vocabulary and the task assignee's perspective. */
@Composable
internal fun TaskTooltip(
    instruction: String?,
    assignee: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
  val renderer = LocalInstructionEnglish.current
  val english =
      remember(renderer, instruction, assignee) {
        instruction?.let {
          renderer?.render(
              parse<InstructionTree>(it),
              assignee?.takeUnless { it == "Admin" }?.let(::cn),
          )
        }
      }
  Tooltip(
      english,
      modifier,
      maxLines = Int.MAX_VALUE,
      rightOfContent = true,
      content = content,
  )
}
