package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** Configuration relationships between each map and its default milestone and award pools. */
internal object MapGoalSets {
  internal data class Goals(
      val milestones: Set<ClassName>,
      val awards: Set<ClassName>,
  )

  internal fun forMap(mapName: ClassName): Goals = byMap[mapName] ?: EMPTY

  private val EMPTY = Goals(emptySet(), emptySet())

  private val byMap =
      mapOf(
          cn("TharsisMap") to
              Goals(
                  names("Terraformer35", "Mayor", "Gardener", "Builder8", "Planner"),
                  names("Landlord", "Banker", "Scientist", "Thermalist", "Miner"),
              ),
          cn("HellasMap") to
              Goals(
                  names("Diversifier", "Tactician5", "PolarExplorer", "Energizer", "RimSettler"),
                  names("Cultivator", "Magnate", "SpaceBaron", "Excentric", "Contractor"),
              ),
          cn("ElysiumMap") to
              Goals(
                  names(
                      "Generalist",
                      "Generalist2",
                      "Specialist",
                      "Ecologist",
                      "Tycoon15",
                      "Legend5",
                  ),
                  names(
                      "Celebrity",
                      "Industrialist",
                      "DesertSettler",
                      "EstateDealer",
                      "Benefactor",
                  ),
              ),
          cn("UtopiaMap") to
              Goals(
                  names("Manager", "Pioneer3", "Trader", "Metallurgist", "Researcher"),
                  names("Suburbian", "Investor", "Botanist", "Incorporator", "Metropolist"),
              ),
          cn("CimmeriaMap") to
              Goals(
                  names("Planetologist", "Architect", "Coastguard", "Forester", "Fundraiser"),
                  names("Electrician", "Founder", "Mogul", "Zoologist", "Forecaster"),
              ),
      )

  private fun names(vararg names: String): Set<ClassName> = names.mapTo(linkedSetOf(), ::cn)
}
