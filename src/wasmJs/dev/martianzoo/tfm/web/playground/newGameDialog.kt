package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon

/** A solo GameConfig editor; GO starts a live game from the resolved configuration. */
@Composable
internal fun NewGameDialog(
    draft: NewGameDraft,
    savedSetups: List<SavedGameSetup>,
    onClose: () -> Unit,
    startupError: String?,
    needsLogAccess: Boolean,
    logConnecting: Boolean,
    onGo: (GameConfig, GamePremise, Int?) -> Unit,
) {
  val palette = LocalPalette.current
  val entries = draft.entries.toList()
  val playerName = draft.playerName
  var resolved by
      remember(entries, playerName) {
        mutableStateOf<Result<Pair<GameConfig, GamePremise>>?>(null)
      }
  val premise = resolved?.getOrNull()?.second
  val seed = draft.seedText.trim()
  val parsedSeed = parsePlaySeed(seed)
  val problem =
      resolved?.exceptionOrNull()?.message
          ?: if (seed.isNotEmpty() && parsedSeed == null)
              "Seed must be a 32-bit decimal or 0x hexadecimal number."
          else null
  Dialog(
      onDismissRequest = onClose,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    MaterialTheme(
        colors =
            darkColors(
                primary = palette.accent,
                background = palette.background,
                surface = palette.surface,
                onSurface = palette.text,
                onBackground = palette.text,
                onPrimary = palette.background,
            )
    ) {
      Surface(
          color = palette.surface,
          modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.88f).widthIn(max = 1050.dp),
      ) {
        Column(Modifier.padding(22.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "New solo game",
                style =
                    palette.body.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = palette.body.fontSize * 1.5f,
                    ),
            )
            Text(
                "CLOSE",
                Modifier.clickable(onClick = onClose),
                style = palette.label.copy(color = palette.accent),
            )
          }
          Spacer(Modifier.height(14.dp))
          Row(
              Modifier.fillMaxWidth().weight(1f),
              horizontalArrangement = Arrangement.spacedBy(20.dp),
          ) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
              if (savedSetups.isNotEmpty()) {
                SavedSetupMenu(savedSetups, draft::recall)
                Spacer(Modifier.height(12.dp))
              }
              PlayerEditor(draft)
              OutlinedTextField(
                  value = draft.seedText,
                  onValueChange = { draft.seedText = it },
                  label = { Text("Shuffle seed (decimal or 0x hex; blank for random)") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                  textStyle = palette.body,
              )
              Text(
                  "Decks never reshuffle: running out ends the game. Search for Life and Asteroid " +
                      "Deflection System are excluded because fresh-card reveals are not supported yet.",
                  style = palette.label,
              )
              Spacer(Modifier.height(18.dp))
              SelectionEditors(draft, premise)
            }
            Column(
                Modifier.width(370.dp)
                    .fillMaxHeight()
                    .background(palette.raised)
                    .border(palette.borderWidth, palette.line)
                    .padding(16.dp)
            ) {
              Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text("GAMECONFIG", style = palette.label.copy(color = palette.accent))
                Spacer(Modifier.height(10.dp))
                Text("CLICK A LINE TO REMOVE IT", style = palette.label)
                entries.forEach { entry ->
                  Text(
                      entry,
                      Modifier.fillMaxWidth()
                          .clickable { draft.remove(entry) }
                          .padding(vertical = 5.dp),
                      style = palette.mono,
                  )
                }
                Spacer(Modifier.height(12.dp))
                Text("PLAYER", style = palette.label)
                Text(playerName, style = palette.mono)
                Spacer(Modifier.height(16.dp))
                Text("GAMEPREMISE · ADDITIONAL PARTS", style = palette.label)
                if (premise == null) {
                  Text(
                      "Calculated when you press GO",
                      style = palette.mono.copy(color = palette.dimText),
                  )
                }
                premise
                    ?.classSelections
                    ?.sortedWith(compareBy({ !it.included }, { it.className.toString() }))
                    ?.map { if (it.included) "${it.className}" else "-${it.className}" }
                    ?.filterNot(entries::contains)
                    ?.forEach { selection ->
                      Text(
                          selection,
                          Modifier.fillMaxWidth().padding(vertical = 2.dp),
                          style = palette.mono.copy(color = palette.dimText),
                      )
                    }
                premise?.componentAdjustments?.forEach { (name, amount) ->
                  val adjustment = "$amount $name"
                  if (adjustment !in entries) {
                    Text(adjustment, style = palette.mono.copy(color = palette.dimText))
                  }
                }
                premise?.bootstrapClassName?.let { name ->
                  Text("bootstrap: $name", style = palette.mono.copy(color = palette.dimText))
                }
                premise?.premiseClassName?.let { name ->
                  Text("premise: $name", style = palette.mono.copy(color = palette.dimText))
                }
              }
              Spacer(Modifier.height(12.dp))
              Text(
                  problem
                      ?: startupError
                      ?: if (logConnecting)
                          "Chrome has not returned a log folder. GO can still start the game."
                      else if (resolved == null) "Ready to check configuration."
                      else "Configuration accepted.",
                  style =
                      palette.body.copy(
                          color =
                              if (problem == null && startupError == null) palette.accent
                              else palette.mars
                      ),
              )
              Spacer(Modifier.height(12.dp))
              if (needsLogAccess) {
                Text(
                    "GO starts the game and asks Chrome for the live log folder. Choose " +
                        "/Users/dev/solarnet/main/_local if the chooser appears. " +
                        "Until then, the log is not being saved to disk.",
                    style = palette.label,
                )
                Spacer(Modifier.height(12.dp))
              }
              Button(
                  onClick = {
                    val checked = runCatching { draft.resolve() }
                    resolved = checked
                    checked.getOrNull()?.let { (config, premise) ->
                      onGo(config, premise, parsedSeed)
                    }
                  },
                  enabled = seed.isEmpty() || parsedSeed != null,
                  modifier = Modifier.fillMaxWidth(),
              ) {
                Text("GO")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SavedSetupMenu(saved: List<SavedGameSetup>, onChoose: (SavedGameSetup) -> Unit) {
  val palette = LocalPalette.current
  var open by remember { mutableStateOf(false) }
  Box(Modifier.fillMaxWidth()) {
    Text(
        "Replay a saved setup  ▾",
        Modifier.fillMaxWidth()
            .border(palette.borderWidth, palette.line)
            .clickable { open = true }
            .padding(10.dp),
        style = palette.body,
    )
    DropdownMenu(
        open,
        onDismissRequest = { open = false },
        modifier = Modifier.heightIn(max = 330.dp).background(palette.raised),
    ) {
      saved.forEach { setup ->
        DropdownMenuItem(
            onClick = {
              open = false
              onChoose(setup)
            }
        ) {
          Text(setup.label, style = palette.mono)
        }
      }
    }
  }
}

@Composable
private fun PlayerEditor(draft: NewGameDraft) {
  val palette = LocalPalette.current
  Text("PLAYER", style = palette.label.copy(color = palette.accent))
  OutlinedTextField(
      value = draft.playerName,
      onValueChange = { draft.playerName = it },
      label = { Text("Name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      textStyle = palette.body,
  )
  Text(
      "The name is passed as the player class. A color word such as Green also colors the player.",
      style = palette.label,
  )
}

@Composable
private fun SelectionEditors(draft: NewGameDraft, premise: GamePremise?) {
  val palette = LocalPalette.current
  val entries = draft.entries.toList()
  val choiceKey = entries to draft.playerName
  val maps = remember { Canon.marsMapDefinitions.map { it.className.toString() }.sorted() }
  val modules = remember { Canon.modules.keys.map { it.toString() }.sorted() }
  val soloObjectives = listOf("StandardSoloObjective", "Tr63SoloObjective")
  val packs = remember { Canon.bundles.map { "${it.bundleName}:cards" }.distinct().sorted() }
  val milestones = remember { concreteNamesBelow("Milestone") }
  val awards = remember { concreteNamesBelow("Award") }
  val cards = remember { Canon.cards.map { it.className.toString() }.sorted() }
  val colonyTiles = remember { Canon.colonyTileClassNames.map { it.toString() }.sorted() }
  Text("CONFIGURATION", style = palette.label.copy(color = palette.accent))
  Spacer(Modifier.height(8.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    ChoiceMenu(
        "Map · ${entries.firstOrNull { it in maps } ?: "none"}",
        maps,
        Modifier.weight(1f),
    ) {
      draft.chooseMap(it)
    }
    Button(
        onClick = { draft.chooseMap(maps.filterNot(entries::contains).ifEmpty { maps }.random()) }
    ) {
      Text("RANDOM")
    }
  }
  Spacer(Modifier.height(8.dp))
  ChoiceMenu(
      "Solo objective · ${entries.firstOrNull { it in soloObjectives } ?: "default"}",
      soloObjectives,
  ) {
    draft.chooseSoloObjective(it)
  }
  Spacer(Modifier.height(8.dp))
  ChoiceMenu(
      "Add an option",
      modules.filter {
        it !in entries && "-$it" !in entries && it !in maps && it !in soloObjectives
      },
  ) {
    draft.add(it)
  }
  Spacer(Modifier.height(8.dp))
  ChoiceMenu(
      "Exclude an option",
      modules
          .map { "-$it" }
          .filter {
            it !in entries && it.drop(1) !in entries && it.drop(1) !in soloObjectives
          },
  ) {
    draft.add(it)
  }
  Spacer(Modifier.height(8.dp))
  AutocompleteChoice(
      "Choose milestones",
      milestones,
      choiceKey,
      { it !in entries && "-$it" !in entries },
      enabled = (premise?.playerNames?.size ?: 1) > 1,
      onRandom = {
        draft.entries.removeAll(milestones.toSet())
        draft.entries.addAll(milestones.shuffled().take(5))
      },
  ) {
    draft.add(it)
  }
  AutocompleteChoice(
      "Choose awards",
      awards,
      choiceKey,
      { it !in entries && "-$it" !in entries },
      enabled = (premise?.playerNames?.size ?: 1) > 1,
      onRandom = {
        draft.entries.removeAll(awards.toSet())
        draft.entries.addAll(awards.shuffled().take(5))
      },
  ) {
    draft.add(it)
  }
  AutocompleteChoice(
      "Choose colony tiles (${entries.count { it in colonyTiles }} selected)",
      colonyTiles,
      choiceKey,
      { it !in entries && "-$it" !in entries },
      // Colonies may need a tile selection before the draft can resolve successfully.
      enabled =
          "ColoniesExpansion" in entries ||
              premise?.modules?.contains(cn("ColoniesExpansion")) == true,
      onRandom = {
        draft.entries.removeAll(colonyTiles.toSet())
        draft.entries.addAll(colonyTiles.shuffled().take(4))
      },
  ) {
    draft.add(it)
  }
  Spacer(Modifier.height(8.dp))
  ChoiceMenu(
      "Add card packs",
      packs.filter { it !in entries && "-$it" !in entries },
  ) {
    draft.add(it)
  }
  Spacer(Modifier.height(8.dp))
  AutocompleteChoice(
      "Add individual cards",
      cards,
      choiceKey,
      { it !in entries && "-$it" !in entries },
  ) {
    draft.add(it)
  }
  AutocompleteChoice(
      "Exclude individual cards",
      cards,
      choiceKey,
      { "-$it" !in entries && it !in entries },
  ) {
    draft.add("-$it")
  }
}

private fun concreteNamesBelow(superclass: String): List<String> {
  val parent = Canon.classTable.getClass(cn(superclass))
  return Canon.classTable
      .allSubclasses(parent)
      .filterNot { it.abstract }
      .map { it.className.toString() }
      .sorted()
}

@Composable
private fun ChoiceMenu(
    label: String,
    choices: List<String>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onChoose: (String) -> Unit,
) {
  val palette = LocalPalette.current
  var open by remember { mutableStateOf(false) }
  Box(modifier) {
    Text(
        "$label  ▾",
        Modifier.fillMaxWidth()
            .border(palette.borderWidth, palette.line)
            .clickable { open = true }
            .padding(10.dp),
        style = palette.body,
    )
    DropdownMenu(
        open,
        onDismissRequest = { open = false },
        modifier = Modifier.heightIn(max = 330.dp).background(palette.raised),
    ) {
      if (choices.isEmpty()) {
        DropdownMenuItem(onClick = { open = false }) {
          Text("No more choices", style = palette.body)
        }
      } else
          choices.forEach { choice ->
            DropdownMenuItem(
                onClick = {
                  open = false
                  onChoose(choice)
                }
            ) {
              Text(choice, style = palette.body)
            }
          }
    }
  }
}

@Composable
private fun AutocompleteChoice(
    label: String,
    names: List<String>,
    choiceKey: Any,
    isAvailable: (String) -> Boolean,
    enabled: Boolean = true,
    onRandom: (() -> Unit)? = null,
    onChoose: (String) -> Unit,
) {
  val palette = LocalPalette.current
  var query by remember { mutableStateOf("") }
  val matching =
      remember(query, names, choiceKey, enabled) {
        if (!enabled || query.isBlank()) emptyList()
        else
            names
                .filter { it.contains(query.trim(), ignoreCase = true) && isAvailable(it) }
                .take(30)
      }
  Row(
      Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (enabled) 1f else 0.45f),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.weight(1f)) {
      OutlinedTextField(
          value = query,
          enabled = enabled,
          onValueChange = { query = it },
          label = { Text(label) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = palette.body,
      )
      DropdownMenu(
          expanded = enabled && matching.isNotEmpty(),
          properties = PopupProperties(focusable = false),
          onDismissRequest = { query = "" },
          modifier = Modifier.heightIn(max = 300.dp).background(palette.raised),
      ) {
        matching.forEach { name ->
          DropdownMenuItem(
              onClick = {
                onChoose(name)
                query = ""
              }
          ) {
            Text(name, style = palette.body)
          }
        }
      }
    }
    if (onRandom != null) {
      Button(onClick = onRandom, enabled = enabled) { Text("RANDOM") }
    }
  }
}
