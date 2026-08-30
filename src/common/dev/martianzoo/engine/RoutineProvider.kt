package dev.martianzoo.engine

/** Optional Catalog capability supplying the Routines understood by that game. */
public interface RoutineProvider {
  public val routines: Map<String, Routine>
}
