package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ComponentClassDefinition
import dev.martianzoo.tfm.data.MarsAreaDefinition
import dev.martianzoo.tfm.data.MoshiReader
import dev.martianzoo.tfm.petaform.api.ComponentDecls
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.util.Grid

object Canon {
  val cardDefinitions: Map<String, CardDefinition> by lazy {
    MoshiReader.readCards(readResource("cards.json5"))
  }

  val mapAreaDefinitions: Map<String, Grid<MarsAreaDefinition>> by lazy {
    MoshiReader.readMaps(readResource("maps.json5"))
  }

  // You wouldn't normally use this, but have only a single map in play.
  val allDefinitions: Map<String, ComponentClassDefinition> by lazy {
    val ct = newComponentClassDefinitions.values
    val cards = cardDefinitions.values
    val areas =  mapAreaDefinitions.values.flatMap { it }
    val expected = ct.size + cards.size + areas.size
    (ct + cards + areas).map { it.asComponentClassDefinition }.associateBy { it.name }.also {
      require(it.size == expected)
    }
  }

  val newStyleComponents by lazy { readResource("components.pets") }

  fun pad(s: Any, width: Int) = ("$s" + " ".repeat(width)).substring(0, width)

  val newComponentClassDefinitions: Map<String, ComponentClassDefinition> by lazy {
    val decls = parse<ComponentDecls>(newStyleComponents)
    decls.decls.map { comp ->
      val name = comp.expression.rootType.name
      // println("${pad(name, 19)} ${pad(comp.expression.specializations, 29)} ${comp.supertypes}")

      ComponentClassDefinition(
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
