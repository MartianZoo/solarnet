package dev.martianzoo.tfm.script.commands

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.commands.TaskCommand
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.tfmCatalog

internal class TfmPlayCommand(private val repl: ScriptSession) : ScriptCommand("tfm_play") {
  override val usage: String = "tfm_play <CardName>[, <payment>...]"
  override val help: String =
      """
        Plays a Terraforming Mars card, selecting the Play Card standard action first when needed.
        Payment text after the first comma is passed to `tfm_pay`; for example,
        `tfm_play OlympusConference, 2 Steel, 1`.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      if (',' in context.args) context.paymentWords() else context.playableCardNames()

  override fun withArgs(args: String): List<String> {
    val cardText = args.substringBefore(',').trim()
    val cardName = repl.game.vocabulary.canonicalName(cn(cardText))
    val kind = cardBack(repl.game.reader.tfmCatalog.card(cardName))!!.className
    val payment = args.substringAfter(',', missingDelimiterValue = "").trim()
    val result =
        repl.game.timeline.atomic {
          val choosingStandardAction =
              repl.game.tasks
                  .matching { it.instruction.toString().contains("StandardAction") }
                  .any()
          if (choosingStandardAction) {
            TaskCommand(repl).withArgs("UseAction<PlayCardFromHand, Action1>")
          }
          TaskCommand(repl).withArgs("PlayCard<Class<$kind>, Class<$cardName>>")
          if (payment.isNotEmpty()) TfmPayCommand(repl).withArgs(payment)
        }
    return repl.describeExecutionResults(result)
  }
}
