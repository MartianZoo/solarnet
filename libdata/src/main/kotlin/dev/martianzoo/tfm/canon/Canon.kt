package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ComponentDefinition
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.MoshiReader
import dev.martianzoo.tfm.petaform.api.ComponentClassDeclaration
import dev.martianzoo.tfm.petaform.api.ComponentDecls
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.util.Grid

object Canon {
  val cardDefinitions: Map<String, CardDefinition> by lazy {
    MoshiReader.readCards(readResource("cards.json5"))
  }

  val auxiliaryComponentDefinitions: Map<String, ComponentDefinition> by lazy {
    cardDefinitions.values
        .flatMap { it.componentsPetaform }
        .map { parse<ComponentClassDeclaration>(it) }
        .map { vanillify(it) }
        .associateBy { it.name }
  }

  private fun vanillify(it: ComponentClassDeclaration): ComponentDefinition {
    return ComponentDefinition(
        it.expression.className,
        it.abstract,
        it.supertypes.map(Any::toString).toSet(),
        it.expression.specializations.map(Any::toString),
        null,
        it.actions.map(Any::toString).toSet(),
        it.effects.map(Any::toString).toSet(),
    )
  }

  val mapAreaDefinitions: Map<String, Grid<MarsAreaDefinition>> by lazy {
    MoshiReader.readMaps(readResource("maps.json5"))
  }

  // You wouldn't normally use this, but have only a single map in play.
  val allDefinitions: Map<String, ComponentDefinition> by lazy {
    val ct = componentDefinitions.values
    val cards = cardDefinitions.values
    val aux = auxiliaryComponentDefinitions.values
    val areas =  mapAreaDefinitions.values.flatMap { it }
    val expected = ct.size + cards.size + aux.size + areas.size
    (ct + cards + aux + areas).map { it.asComponentDefinition }.associateBy { it.name }.also {
      require(it.size == expected)
    }
  }

  val componentDefnsString by lazy { readResource("components.pets") }

  fun pad(s: Any, width: Int) = ("$s" + " ".repeat(width)).substring(0, width)

  val componentDefinitions: Map<String, ComponentDefinition> by lazy {
    val decls = parse<ComponentDecls>(componentDefnsString)
    decls.decls.map { comp ->
      val name = comp.expression.className
      // println("${pad(name, 19)} ${pad(comp.expression.specializations, 29)} ${comp.supertypes}")

      ComponentDefinition(
          name,
          comp.abstract,
          comp.supertypes.map(Any::toString).toSet(),
          comp.expression.specializations.map(Any::toString),
          immediatePetaform = null,
          comp.actions.map(Any::toString).toSet(),
          comp.effects.map(Any::toString).toSet(),
      )
    }.associateBy { it.name }
  }

  private fun readResource(filename: String): String {
    val dir = javaClass.packageName.replace('.', '/')
    return javaClass.getResource("/$dir/$filename")!!.readText()
  }
}
