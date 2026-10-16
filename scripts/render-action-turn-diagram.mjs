import { readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { join } from "node:path";

const [temporary, output] = process.argv.slice(2);
const require = createRequire(join(temporary, "package.json"));
const { instance } = require("@viz-js/viz");
const { Resvg } = require("@resvg/resvg-js");
const viz = await instance();
const standaloneLinks = svg => svg.replaceAll('href="#panel-', 'href="index.html#panel-');
const escape = s => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
// Each panel's geometry comes from Graphviz; the poster only arranges those rectangles in rows.
const panelNumbers = [...readFileSync(join(output, "action-turns.dot"), "utf8")
  .matchAll(/graph \[id="panel-(\d+)"/g)].map(match => Number(match[1]));
const panels = panelNumbers.map(number => {
  const dot = readFileSync(join(output, `panel-${number}.dot`), "utf8");
  const svg = viz.renderString(dot, { format: "svg", engine: "dot" });
  const viewBox = svg.match(/viewBox="([^"]+)"/)[1];
  const [, , width, height] = viewBox.split(" ").map(Number);
  const body = svg.slice(svg.indexOf(">", svg.indexOf("<svg")) + 1, svg.lastIndexOf("</svg>"));
  const scoped = preservePanel => body.replace(/id="([^"]+)"/g, (_, encoded) => {
    const id = encoded.replaceAll("&#45;", "-");
    return `id="${preservePanel && id.startsWith("panel-") ? id : `p${number}-${id}`}"`;
  });
  writeFileSync(join(output, `panel-${number}.svg`), standaloneLinks(svg));
  return { number, width, height, viewBox, scoped };
});
const overview = viz.renderString(readFileSync(join(output, "overview.dot"), "utf8"), { format: "svg", engine: "dot" });
writeFileSync(join(output, "overview.svg"), standaloneLinks(overview));
writeFileSync(join(output, "overview.png"), new Resvg(overview, { fitTo: { mode: "width", value: 1800 } }).render().asPng());
const inlineOverview = overview.slice(overview.indexOf("<svg"))
  .replace(/id="([^"]+)"/g, (_, id) => `id="overview-${id}"`);
const gap = 24;
const columnWidth = Math.max(...panels.map(p => p.width));
let y = 150;
const placements = [];
for (let i = 0; i < panels.length; i += 2) {
  const row = panels.slice(i, i + 2);
  row.forEach((panel, column) => placements.push(
    `<svg x="${gap + column * (columnWidth + gap)}" y="${y}" width="${panel.width}" height="${panel.height}" viewBox="${panel.viewBox}">${panel.scoped(true)}</svg>`));
  y += Math.max(...row.map(p => p.height)) + gap;
}
const width = columnWidth * 2 + gap * 3;
const svg = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${width}" height="${y}" viewBox="0 0 ${width} ${y}">
<rect width="100%" height="100%" fill="#f7f8fc"/>
<g font-family="Helvetica,Arial,sans-serif" fill="#213047">
<text x="24" y="36" font-size="28" font-weight="bold">FOLLOW THE BATON. WATCH THE ACTIONSCOPE.</text>
<text x="24" y="66" font-size="16">Event → every matching live rule → work or choice. Follow numbered destinations; row order is not execution order.</text>
<text x="24" y="91" font-size="14">+ = gain · − = removal · 2 X = at least two · MAX 0 X = none · This / Me@ belong to the rule's owner.</text>
<text x="24" y="116" font-size="14">Green: automatic (::) · Blue: OR choice · Amber: FINALLY, after ordinary and LATER work. Conditions are read from Pets.</text>
</g>${placements.join("\n")}</svg>`;
writeFileSync(join(output, "action-turns.svg"), svg);
writeFileSync(join(output, "action-turns.png"), new Resvg(svg, { fitTo: { mode: "width", value: 2600 } }).render().asPng());
const rules = escape(readFileSync(join(output, "rules.md"), "utf8"));
const inline = panels.map(p => `<section id="panel-${p.number}"><svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="${p.viewBox}">${p.scoped(false)}</svg><p><a href="#top">Back to guide</a> · <a href="panel-${p.number}.dot">Panel DOT</a></p></section>`).join("\n");
writeFileSync(join(output, "index.html"), `<!doctype html>
<meta charset="utf-8"><title>How action-phase turns work</title>
<style>
body{margin:0;background:#f7f8fc;color:#213047;font:16px/1.5 system-ui,sans-serif}
header,details{max-width:1050px;margin:24px auto;padding:0 24px}
h1{margin-bottom:8px}p{margin:8px 0}a{color:#2456a6}
.legend{display:flex;gap:12px;flex-wrap:wrap}.legend span{padding:5px 12px;border-radius:5px}
main{max-width:1200px;margin:auto;padding:20px}svg{width:100%;height:auto}section{margin:24px 0 48px;scroll-margin-top:20px}section p{text-align:right;font-size:13px}nav{display:flex;gap:8px;flex-wrap:wrap}nav a{padding:4px 10px;background:#e9edf6;border-radius:5px}
pre{white-space:pre-wrap;font-size:13px}summary{cursor:pointer;font-weight:600}
</style>
<header id="top"><h1>Follow the Baton. Watch the ActionScope.</h1>
<p>Read each panel left to right: <b>event → matching live rule → work or choice</b>.
Click a numbered destination to continue. All matching rules react; their vertical order is not execution order.</p>
<p>The Baton stays with the player across actions. Accepting an action opens its ActionScope;
finishing it can offer another action or release the Baton. The conditions below determine which.</p>
<div class="legend"><span style="background:#dcf3e9">:: Automatic</span><span style="background:#e4edff">OR: choose an alternative</span><span style="background:#fff0d6">FINALLY: after ordinary and LATER work</span></div>
<p><code>+X</code> means X gained; <code>-X</code> means X removed. <code>2 X</code> in a condition means at least 2; <code>MAX 0 X</code> means none.
<code>This</code> and <code>Me@</code> belong to the rule's owner. Here ActionPhase exists; branches requiring its absence do not run.</p>
<p><a href="action-turns.svg">SVG</a> · <a href="action-turns.png">PNG</a> · <a href="action-turns.dot">DOT source</a> · <a href="rules.md">Extracted rules and source hashes</a></p>
<nav>${panels.map(p => `<a href="#panel-${p.number}">Panel ${p.number}</a>`).join(" ")}</nav>
</header><main><section id="overview"><h2>Where play can go</h2>
<p>These arrows come from the rules below. They show possible paths: each path still depends on its rule's guard or player choice. Click a box to inspect that event.</p>
${inlineOverview}<p><a href="overview.dot">Overview DOT</a> · <a href="overview.svg">Overview SVG</a></p></section>${inline}</main>
<details><summary>Source evidence and diagram scope</summary><pre>${rules}</pre></details>
`);
console.log(`Rendered SVG, PNG and index.html in ${output}`);
