package dev.martianzoo.state

/**
 * An input supplied to a game in chronological order, separate from its calculated [GameEvent]s.
 * The first input starts the workflow. A [Command] retains the Actor's submitted instruction and
 * any external card identities needed before that command can run. Engine-owned effects are not
 * commands. The exact event recording, when present, describes the consequences of these inputs.
 */
public sealed class RecordedInput {
  /** Starts the named game workflow before any Actor command is replayed. */
  public data class StartWorkflow(public val name: String) : RecordedInput()

  /**
   * A successful Actor submission. [kind] preserves how the instruction was submitted; [cardInputs]
   * supply card identities consumed while executing this command.
   */
  public data class Command(
      public val actor: Actor,
      public val kind: Kind,
      public val instruction: String,
      public val cardInputs: List<CardInput> = emptyList(),
      public val taskSelector: TaskSelector? = null,
  ) : RecordedInput()

  /** Named cards supplied before a task, grouped by Player, back, and destination. */
  public data class CardInput(
      public val player: Player,
      public val back: String,
      public val movement: CardMovement,
      public val names: List<String>,
  ) {
    init {
      require(back.isNotBlank())
      require(names.isNotEmpty() && names.all(String::isNotBlank))
    }
  }

  /** External card movement for a Player's hand or current Selecting offer. */
  public enum class CardMovement {
    HAND_ARRIVAL,
    HAND_DEPARTURE,
    SELECTING_ARRIVAL,
    SELECTING_DEPARTURE,
  }

  /** Distinguishes otherwise identical pending tasks by causal ancestry, without event ordinals. */
  public data class TaskSelector(
      public val instruction: String,
      public val cause: List<CauseStep>,
      public val occurrence: Int,
  ) {
    init {
      require(occurrence >= 0)
    }
  }

  /** A triggering change and the component context whose effect it activated. */
  public data class CauseStep(
      public val context: String,
      public val actor: String,
      public val change: String,
  )

  /** Agent operations whose identical instruction text can have different effects. */
  public enum class Kind {
    DO_TASKS,
    DO_TASK,
    DO_TASK_ID,
    TRY_TASK,
    TRY_TASK_ID,
    CHOOSE,
    TRY_SELECTED,
    DROP,
    EX_MACHINA,
  }
}
