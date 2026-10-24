package dev.martianzoo.tfm.tests

/** Fixed command abbreviations: arguments alone determine every submitted task. */
internal fun playCardTasks(card: String, source: String = "Hand"): Array<String> =
    arrayOf(
        "UseAction<PlayCardAction, Action1>",
        "PlayCard<Class<ProjectCard>, Class<$card>, $source>",
    )

internal fun cardActionTasks(card: String, action: String = "Action1"): Array<String> =
    arrayOf("UseAction<UseActionOnCardAction, Action1>", "UseAction<$card, $action>")

internal fun standardProjectTasks(project: String): Array<String> =
    arrayOf("UseAction<UseStandardProjectAction, Action1>", "UseAction<$project, Action1>")
