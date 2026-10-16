package dev.martianzoo.tfm.web.playground

internal fun availableTabs(specimen: Specimen, playing: Boolean): List<GlobalTab> {
  val classes =
      specimen.components.filter { it.className == "Class" }.mapTo(hashSetOf()) { it.type }
  return GlobalTab.entries.filter {
    when (it) {
      GlobalTab.Play -> playing
      GlobalTab.Colonies -> "Class<ColoniesExpansion>" in classes
      GlobalTab.Turmoil -> "Class<TurmoilExpansion>" in classes
      GlobalTab.Goals ->
          (specimen.milestones.map { it.name } + specimen.awards.map { it.name }).any { name ->
            "Class<$name>" in classes
          }
      else -> true
    }
  }
}
