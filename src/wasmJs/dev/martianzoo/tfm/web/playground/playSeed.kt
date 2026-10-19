package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.ast.ClassName

/** The displayed seed keeps all 32 bits, including the sign bit. */
internal fun formatPlaySeed(seed: Int): String = "0x${seed.toUInt().toString(16).padStart(8, '0')}"

/** Accepts the displayed hexadecimal spelling and signed decimal seeds. */
internal fun parsePlaySeed(text: String): Int? {
  val value = text.trim()
  return if (value.startsWith("0x", ignoreCase = true)) {
    value.drop(2).toUIntOrNull(16)?.toInt()
  } else value.toIntOrNull()
}

/** Ranks each card independently so adding a card preserves the others' relative order. */
internal fun orderedDeck(cards: List<ClassName>, seed: Int): List<ClassName> =
    cards
        .map { card -> card to cardRank(seed, card.toString()) }
        .sortedWith(compareBy<Pair<ClassName, ULong>> { it.second }.thenBy { it.first.toString() })
        .map { it.first }

private fun cardRank(seed: Int, name: String): ULong {
  var hash = 0xcbf29ce484222325uL
  for (shift in 0..24 step 8) {
    hash = (hash xor ((seed.toUInt() shr shift) and 0xffu).toULong()) * 0x100000001b3uL
  }
  for (byte in name.encodeToByteArray()) {
    hash = (hash xor byte.toUByte().toULong()) * 0x100000001b3uL
  }
  hash = (hash xor (hash shr 33)) * 0xff51afd7ed558ccduL
  hash = (hash xor (hash shr 33)) * 0xc4ceb9fe1a85ec53uL
  return hash xor (hash shr 33)
}
