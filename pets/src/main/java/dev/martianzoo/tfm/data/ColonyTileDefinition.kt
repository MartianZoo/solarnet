package dev.martianzoo.tfm.data

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction

public class ColonyTileDefinition(data: ColonyTileData) : Definition {
  override val className = cn(data.name)
  override val shortName = cn(data.name)
  override val bundle = data.bundle

  val placementBonus: Instruction = parse(data.placementBonus)
  val colonyBonus: Instruction = parse(data.colonyBonus)
  val tradeIncome: List<Instruction> = data.tradeIncome.map(::parse)
  val resourceType: ClassName? = data.resourceType?.let(::cn)

  override val asClassDeclaration: ClassDeclaration by lazy {
    with(data) {
      Parsing.parseClasses(
          """
            CLASS $name : ColonyTile {
              Colony<This>: $placementBonus
              GiveColonyBonus<This>: $colonyBonus

              Trade<This> IF =0 ColonyProduction<This>: ${tradeIncome[0]}
              Trade<This> IF =1 ColonyProduction<This>: ${tradeIncome[1]}
              Trade<This> IF =2 ColonyProduction<This>: ${tradeIncome[2]}
              Trade<This> IF =3 ColonyProduction<This>: ${tradeIncome[3]}
              Trade<This> IF =4 ColonyProduction<This>: ${tradeIncome[4]}
              Trade<This> IF =5 ColonyProduction<This>: ${tradeIncome[5]}
              Trade<This> IF =6 ColonyProduction<This>: ${tradeIncome[6]}
              Trade<This>: ResetProduction<This>
            }
          """).single()
    }
  }

  data class ColonyTileData(
      val name: String,
      val bundle: String,
      val placementBonus: String,
      val colonyBonus: String,
      val tradeIncome: List<String>,
      val resourceType: String?,
  ) {
    init {
      require(bundle.isNotEmpty())
      require(tradeIncome.size == 7)
    }
  }
}
