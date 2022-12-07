package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.componentDefinitions
import dev.martianzoo.tfm.petaform.api.ComponentDecls
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.parser.PetaformParser
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.tfm.types.ComponentClassLoader
import org.junit.jupiter.api.Test

// Not testing much, just a bit of the canon data
class ComponentDefinitionTest {
  @Test
  fun whoKnows() {
    PetaformParser.parseComponentClasses(Canon.componentDefnsString)
  }

  @Test
  fun foo() {
    val data = Canon.componentDefinitions
    val tr = data["TerraformRating"]!!
    assertThat(tr.name).isEqualTo("TerraformRating")
    assertThat(tr.abstract).isFalse()
    assertThat(tr.supertypesPetaform).containsExactly("Owned<Player>")
    assertThat(tr.dependenciesPetaform).isEmpty()
    assertThat(tr.effectsPetaform).containsExactly(
        "This:: HasRaisedTR.", "ProductionPhase: 1", "End: VictoryPoint"
    )
  }

  @Test
  fun slurp() {
    val defns = Canon.allDefinitions
    assertThat(defns.size).isGreaterThan(550)

    val loader = ComponentClassLoader()
    loader.loadAll(defns.values)
    val table = loader.snapshot()

    table.all().forEach { rc ->
      val cc = defns[rc.name]!!
      if (cc.supertypesPetaform.isNotEmpty()) {
        // checkRoundTrip(cc.supertypesPetaform, rc.superclasses)
      }
      checkRoundTrip(listOfNotNull(cc.immediatePetaform), listOfNotNull(rc.immediate))
      checkRoundTrip(cc.actionsPetaform, rc.actions)
      checkRoundTrip(cc.effectsPetaform, rc.effects)
      // deps??
    }
  }

  fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetaformNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat("${it.second}").isEqualTo(it.first)
    }
  }

  @Test
  fun craxy() {
    val decls = parse<ComponentDecls>(Canon.componentDefnsString)
    decls.decls.forEach {
      print(if (it.abstract) "abstract" else "concrete")
      println(" ${it.expression} : ${it.supertypes}")
    }
  }

  @Test
  fun craxier() {
    componentDefinitions.values.forEach {
      println(it)
    }
  }
}
