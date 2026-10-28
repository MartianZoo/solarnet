@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

/** Saves a full game file through the browser's normal download flow. */
@JsFun(
    """(filename, contents) => {
      const blob = new Blob([contents], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    }"""
)
internal external fun downloadReplayFile(filename: String, contents: String)
