package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException

internal class CommentCommand(private val repl: ScriptSession) : ScriptCommand("comment") {
  override val usage = "comment <event-id> \"<message>\""
  override val help =
      """
        Replaces the free-form comment on an event. The message must be double-quoted, and the
        revised event is printed. Use an empty quoted message to clear the existing comment.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      if (context.argIndex == 0) context.eventIds() else emptyList()

  override fun withArgs(args: String): List<String> {
    val match = Regex("""^(\d+)\s+"([^"]*)"$""").matchEntire(args) ?: throw UsageException()
    val eventId = match.groupValues[1].toInt()
    val message = match.groupValues[2].ifEmpty { null }
    val event = repl.game.events.reviseComment(eventId, message)
    return listOf(repl.game.vocabulary.renderPets(event))
  }
}
