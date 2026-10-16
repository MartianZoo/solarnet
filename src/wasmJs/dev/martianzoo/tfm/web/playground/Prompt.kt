package dev.martianzoo.tfm.web.playground

import dev.martianzoo.state.Task.TaskId

/** What the live game is waiting on, and the choices the Play tab offers for it. */
internal data class Prompt(
    val heading: String,
    val detail: String? = null,
    val options: List<Option> = emptyList(),
    /** Whether more options exist beyond [options]; the tab offers MORE. */
    val more: Boolean = false,
    /** Whether the draft form can be reset or the open naming attempt cancelled. */
    val retreat: String? = null,
    /** False for history or unsupported Admin work: this prompt cannot accept input. */
    val enabled: Boolean = true,
) {
  data class Option(
      val label: String,
      val who: String? = null,
      val held: Boolean = false,
      val enabled: Boolean = true,
      /** The original task, even when [label] shows its evaluated instruction. */
      val taskId: TaskId? = null,
  )
}
