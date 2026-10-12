@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

// Talks to the development server's layout endpoint (modules/playground/webpack.config.d). The
// static distribution has no such endpoint; there, loading reports null and nothing is saved.

/** Calls [received] with the layout file's text, or with null when no endpoint answers. */
internal fun loadLayoutText(received: (String?) -> Unit): Unit =
    js(
        """
        fetch("layout.json", { cache: "no-store" })
          .then((response) => (response.ok ? response.text() : null))
          .then((text) => received(text), () => received(null))
        """
    )

internal fun saveLayoutText(text: String): Unit =
    js(
        """
        fetch("layout.json", {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: text,
        })
        """
    )

/** Calls [changed] whenever someone else edits the layout file. */
internal fun watchLayoutFile(changed: () -> Unit): Unit =
    js(
        """
        new EventSource("layout-events").onmessage = () => changed()
        """
    )

/** Routes history arrows without taking them from browser inputs or shortcut combinations. */
internal fun watchHistoryKeys(pressed: (String) -> Boolean): Unit =
    js(
        """
        window.addEventListener("keydown", (event) => {
          if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
          if (event.target instanceof Element &&
              event.target.closest("input, textarea, select, [contenteditable]")) return;
          if (pressed(event.key)) event.preventDefault();
        })
        """
    )
