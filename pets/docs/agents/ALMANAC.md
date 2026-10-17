# Almanac

The browser app lives in `src/js/dev/martianzoo/tfm/web/almanac`.

- Opening the app without a URL fragment picks a random card from AiCentral, ArcticAlgae, and
  the eleven README gallery cards. The selected card is recorded in the URL, so refresh and shared
  links retain the selected view.
- Columns show class details (including cards), complete relation results, XPath results, or query examples.
  Clicking a class link or `(see all)` retains the source column and every column to its left,
  removes columns to its right, and opens the selected details or results next to it.
  Navigation scrolls to the top of the new column.
- Columns use CSS intrinsic widths, capped at 72 characters or the available viewport width.
  Long source text wraps; prose and the submitted XPath follow the width of the source/results.
  The column strip remains horizontally scrollable. Headings and dismiss controls align above artwork;
  missing artwork and empty English-text panels do not reserve space.
- Light and dark palettes share the same restrained monospace layout, thin rules, and muted green
  accent. `Dark theme` switches palettes without rebuilding columns and persists in local storage.
  The saved palette is applied before stylesheet paint. Focus indicators and responsive controls
  work in both themes; navigation respects the browser's reduced-motion preference.
- Detail panels sample at most ten subclasses or usages. Truncated panels offer `(see all)`;
  the result column lists the entire group alphabetically, with clickable class references.
- Lists and the detail supertype panel show the direct superclass with the most subclasses,
  breaking ties alphabetically. The complete inheritance declaration remains in Pets source.
- Usage sections classify the target of each gain/removal separately from its arguments, counts,
  trigger, and conditions. Positive `HAS` contents tied to `This` count as gains; zero counts,
  upper bounds, and unrelated invariants appear under Invariants. Existing action/PROD lowering
  makes action costs removals and production changes gains/removals of Production.
- `Use icons` starts enabled, like Playground. Inline class links use 30px artwork with full-size
  fallback, retain clickable text if artwork is absent, and show the class name as a tooltip.
  Concrete cards, standard projects, Hand, and Production stay textual. Toggling changes only
  presentation; it preserves the columns and their sampled examples. English prose and headings
  stay textual. Top/bottom prose is generated in the browser via `English.renderCard`.
- Existing columns retain their DOM, scroll positions, and sampled examples while navigating.
  Dismiss removes its column. Selecting a class from the top search field starts a new trail.
- URL fragments preserve the trail, including result columns: `Plant,Plant:Usages:_Gains`.
  Plain class names remain valid. `examples` opens the example list;
  `query:AUTHORED:<URI-encoded XPath>` (or `ELABORATED`) restores a query.
  Browser history and refresh restore all column kinds.
- Images load by Pets class name from `https://www.martianzoo.dev/images/`.
  Cards first try `classes-pubtext/Name.png`; all classes can use `classes/Name.png`.
  Map classes retain the fallback with the `Map` suffix removed. Missing images are hidden.
  There is no downloaded card-image index or bundled local artwork inventory.
- The short class-name field is separate from the multiline XPath editor. `(see examples)` appends
  a grouped catalog of English descriptions. Clicking an example clears columns to its right and
  fills the editor and tree selector, but does not run the query. Run query appends results.
- `queryExamples.kt` holds the curated examples. Keep distinct search ideas rather than tag/resource
  substitutions. Match each English description to what its XPath actually proves; structural
  searches do not establish playability, sum separate gains, or follow triggered consequences.
- The editor explicitly selects authored or elaborated trees. Each form reuses a lazy `CanonQuery`.
  Results request metadata only, never serialized XML. Cards/classes link to details; nested nodes
  show their owning class and Pets spelling, attributes show their owner and value, and scalars
  remain visible even when false or zero. Empty matches and malformed XPath have visible outcomes.
  New query results preserve existing columns and their DOM.

Run `./gradlew :almanac:test` for English card text, reference classification, and every example in
its selected XPath 1.0 tree. Representative checks cover nested production, action costs, subtype
joins, tag contents, and scoring. Exercise editor selection/editing/submission, column navigation,
history/refresh, errors, scalar results, and image loading in a live browser when changing the app.
For layout changes, check different column widths, long text, light/dark persistence, and a narrow viewport.

## Publication

The standalone production build is copied to `pets/almanac/` in the Martian Zoo site repository
(`/Users/dev/martianzoo/site`). Build with `./gradlew :almanac:jsBrowserDistribution`, then copy
the contents of Almanac's `dist/js/productionExecutable` directory, preserving relative asset
paths. The site publishes `main` through GitHub Pages at `https://www.martianzoo.dev/pets/almanac/`.
After pushing the site commit, wait for its Pages build to finish and verify the live app.
