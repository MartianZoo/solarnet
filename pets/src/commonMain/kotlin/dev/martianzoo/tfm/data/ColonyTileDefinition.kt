package dev.martianzoo.tfm.data

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import kotlinx.serialization.Serializable

public class ColonyTileDefinition(data: ColonyTileData) : Definition {
  override val className: ClassName = cn(data.name)
  override val shortName: ClassName = cn(data.name)

  internal val placementBonus: Instruction = parse(data.placementBonus)
  internal val colonyBonus: Instruction = parse(data.colonyBonus)
  internal val tradeIncome: List<Instruction> = data.tradeIncome.map(::parse)
  public val resourceType: ClassName? = data.resourceType?.let(::cn)

  override val asClassDeclaration: ClassDeclaration by lazy {
    with(data) {
      Parsing.parseClasses(
              """
            CLASS $name : ColonyTile {
              Colony<This>: $placementBonus
              GiveColonyBonus<This>: $colonyBonus

              FlownTradeFleet<This> IF =0 ColonyProduction<This>: ${tradeIncome[0]}
              FlownTradeFleet<This> IF =1 ColonyProduction<This>: ${tradeIncome[1]}
              FlownTradeFleet<This> IF =2 ColonyProduction<This>: ${tradeIncome[2]}
              FlownTradeFleet<This> IF =3 ColonyProduction<This>: ${tradeIncome[3]}
              FlownTradeFleet<This> IF =4 ColonyProduction<This>: ${tradeIncome[4]}
              FlownTradeFleet<This> IF =5 ColonyProduction<This>: ${tradeIncome[5]}
              FlownTradeFleet<This> IF =6 ColonyProduction<This>: ${tradeIncome[6]}
              FlownTradeFleet<This>: ResetProduction<This>
            }
          """
          )
          .single()
    }
  }

  @Serializable
  public data class ColonyTileData(
      val name: String,
      val placementBonus: String,
      val colonyBonus: String,
      val tradeIncome: List<String>,
      val resourceType: String? = null,
  ) {
    init {
      require(tradeIncome.size == 7)
    }
  }
}
