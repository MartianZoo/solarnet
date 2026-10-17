package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.types.ClassTable

@Composable
internal fun EffectsTab(
    table: ClassTable,
    bootstrapEffects: Set<Effect>,
    includeImaginary: Boolean,
    useIcons: Boolean,
) {
  val specimen = LocalSpecimen.current
  val palette = LocalPalette.current
  val effects = remember(table, specimen) { activeEffects(table, specimen) }
  val byOwner =
      remember(effects, bootstrapEffects, includeImaginary) {
        effects
            .filterNot { it.effect in bootstrapEffects }
            .filter { includeImaginary || effectIsIconic(it.effect, table) }
            .groupBy { it.context.owner?.toString() }
      }
  val owners = (specimen.playerNames + byOwner.keys).distinct().filter { it in byOwner }
  Column(
      Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .horizontalScroll(rememberScrollState())
          .padding(palette.spacing),
  ) {
    if (byOwner.isEmpty()) Text("No effects match the filters.", style = palette.body)
    for (owner in owners) {
      SectionLabel(owner ?: "NO OWNER")
      val groups = byOwner.getValue(owner).groupBy { it.effect.trigger }
      for ((trigger, entries) in groups.entries.sortedBy { it.key.toString() }) {
        val instructions = InstructionGroup.of(entries.map { it.effect.instruction * it.count })
        val effect = Effect(trigger, instructions)
        PetsText(effect.toString().replace("!", ""), icons = useIcons, source = effect)
        Spacer(Modifier.height(4.dp))
      }
      Spacer(Modifier.height(palette.spacing))
    }
  }
}
