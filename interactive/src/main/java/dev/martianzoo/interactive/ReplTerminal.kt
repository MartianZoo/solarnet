package dev.martianzoo.interactive

internal interface ReplTerminal {
  fun loop(prompt: () -> String, handler: (String) -> List<String>, welcome: String)
  fun historyLines(max: Int? = null): List<String>
}
