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
    /** A provisional choice of named cards from Hand or Selecting, committed on the Play tab. */
    val cards: Cards? = null,
    /** A search failure that leaves offered choices available. */
    val warning: String? = null,
) {
  data class Cards(
      val drawing: Boolean,
      val fromHand: Boolean,
      val available: Set<String>,
      val chosen: Set<String>,
      val canConfirm: Boolean,
      /** Cards whose selection has no witnessed completion. */
      val unproven: Set<String> = emptySet(),
      val confirmUnproven: Boolean = false,
  )

  data class Option(
      val label: String,
      val who: String? = null,
      val held: Boolean = false,
      val enabled: Boolean = true,
      /** The original task, even when [label] shows its evaluated instruction. */
      val taskId: TaskId? = null,
      /** Available OR arms of a task, shown as separate click targets. */
      val arms: List<String> = emptyList(),
      /** The search did not find a completion through this choice. */
      val unproven: Boolean = false,
      val unprovenArms: Set<String> = emptySet(),
  )
}
