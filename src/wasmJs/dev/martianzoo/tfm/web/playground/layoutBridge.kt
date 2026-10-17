@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

/** Opens the named Pets class in the Almanac without replacing the Playground. */
internal fun openAlmanacClass(name: String): Unit =
    js(
        "window.open('https://martianzoo.dev/pets/almanac/#' + encodeURIComponent(name), '_blank', 'noopener')"
    )

// Writes a review snapshot to the development server; the app never reads layout JSON.

internal fun saveLayoutText(text: String): Unit =
    js(
        """
        PLAYGROUND_LAYOUT_ENABLED && fetch("layout.json", {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: text,
        }).catch(() => {})
        """
    )

/** Routes history arrows and undo without taking them from text inputs. */
internal fun watchHistoryKeys(pressed: (String) -> Boolean): Unit =
    js(
        """
        window.addEventListener("keydown", (event) => {
          if (event.defaultPrevented || event.altKey) return;
          if (event.target instanceof Element &&
              event.target.closest("input, textarea, select, [contenteditable]")) return;
          const undo = (event.metaKey || event.ctrlKey) && !event.shiftKey && event.key.toLowerCase() === "z";
          if (!undo && (event.ctrlKey || event.metaKey)) return;
          if (pressed(undo ? "Undo" : event.key)) event.preventDefault();
        })
        """
    )
