package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

/** Catalog-contributed Kotlin knowledge for carrying out one named player interaction. */
public fun interface Routine {
  public fun execute(context: RoutineContext, arguments: List<String>): TaskResult
}
