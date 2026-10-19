# Almanac

The browser app lives in `src/js/dev/martianzoo/tfm/web/almanac`.

- Opening the app without a URL fragment picks a random card from AiCentral, ArcticAlgae, and
  the eleven README gallery cards. The selected card is recorded in the URL, so refresh and shared
  links retain the selected view.
- Columns show class details (including cards), complete relation results, XPath results, or query examples.
  Clicking a class link or `(see all)` retains the source column and every column to its left,
  removes unpinned columns to its right, and appends the selected details or results after any
  pinned survivors. The pin button beside each close button preserves that panel and its DOM,
  including unsent XPath edits, while navigating. Clicking close still removes a pinned panel.
  Navigation scrolls to the top of the new column.
- Columns use CSS intrinsic widths, capped at 55.5 characters or the available viewport width;
  after the type-size change this is about 85% of the former maximum pixel width.
  They have a 43.2-character minimum where the viewport permits it. Each column has its own
  bordered surface; sections within it are separated by rules.
  Long source text wraps; prose and the submitted XPath follow the width of the source/results.
  The column strip remains horizontally scrollable. Headings and dismiss controls align above a
  fixed 250-pixel image area; missing artwork shows a quiet placeholder. Empty English-text panels
  do not reserve space.
- Light and dark palettes use neutral slate surfaces, a blue accent, sans-serif prose/headlines
  enlarged by one point,
  and monospace source/XPath text. `Dark theme` switches palettes without rebuilding columns and persists in local storage.
  The saved palette is applied before stylesheet paint. Focus indicators and responsive controls
  work in both themes; navigation respects the browser's reduced-motion preference. The tab
  favicon uses a complete 16-pixel white-dog sprite frame.
- Detail panels show direct subclasses and all descendants in separate sections; neither includes
  the class itself. Each section samples at most ten classes. Truncated panels offer `(see all)`.
  Class result lists put descendants of Iconic first, then other classes in muted text, with names
  alphabetical within each group. XPath results and class lookup suggestions use the same priority.
- Lists annotate classes with the direct superclass with the most subclasses, breaking ties
  alphabetically. The complete inheritance declaration remains in Pets source.
- Cards show generated top and bottom text; concrete milestones and awards show one generated
  Text panel. A class's own docstring appears after generated text when present. Details then list
  direct supertypes and dependencies, followed by
  invariants, properties, actions, effects, usages, and the complete parseable Pets class source.
  Custom metrics and instructions also show their original Kotlin implementation in smaller type
  below the Pets source. Run `python3 scripts/build_almanac_custom_sources.py` from the combined
  Solarnet checkout to regenerate the bundled source lookup after an implementation changes.
  Invariants, actions, and effects include ancestor declarations labeled `(from ClassName)`.
  Properties show each class's effective values once; omitted optional requirements show `(NONE)`.
  Inherited invariants appear only when they refer to `This`; a static limit such as
  `HAS MAX 3 Milestone` remains on its declaring class, while an Area's
  `HAS MAX 1 Occupant<This>` appears on its subclasses.
  The source has no extra spacing between declaration kinds; nested classes already acquire
  their enclosing class as a direct supertype when parsed.
- Usage sections query the elaborated Canon XPath tree for Gains, Removes, Triggers, Requirements,
  Counted in, Conditions, and Other references. Requirements include declared class invariants;
  Counted in identifies the target of a `Count` metric outside invariant declarations, so a card's
  printed tag count is not mistaken for a metric use. Self-trigger nodes count the owner class as
  a trigger usage, including OxygenStep. Positive `HAS` contents tied to `This` count as gains; zero counts
  and upper bounds do not. Action costs count as removals and production changes as gains/removals
  of Production. Other references exclude class pairs already covered by the preceding categories.
- Class links always use text; only a class's main image area requests artwork. `PROD` links to
  Production while keeping its authored spelling; a class never links to itself within its detail
  or results panel. Top/bottom prose is generated in the browser via `English.renderCard`.
- Existing columns retain their DOM, scroll positions, and sampled examples while navigating.
  Dismiss removes its column. Selecting a class from the top search field starts a new trail after
  any pinned panels.
- URL fragments preserve the trail, including result columns: `Plant,Plant:Usages:_Gains`.
  Plain class names remain valid. `examples` opens the example list;
  `query:AUTHORED:<URI-encoded XPath>` (or `ELABORATED`) restores a query.
  A `!` prefix marks a pinned panel. Browser history and refresh restore panel kinds and pins.
- Images load by Pets class name from `https://www.martianzoo.dev/images/`.
  Cards first try `classes-pubtext/Name.png`; all classes can use `classes/Name.png`.
  Map classes retain the fallback with the `Map` suffix removed. Large images shrink to fit the
  250-pixel area; small images enlarge by at most 2x and remain centered without cropping.
  There is no downloaded card-image index or bundled local artwork inventory.
- The toolbar's empty class lookup shows `ClassNameToSearchFor` as placeholder text; loading a
  fragment or opening panels leaves the field empty, and selecting a suggestion clears it.
  `see example queries` appends a grouped catalog of
  English descriptions. Clicking an example replaces that column with its query results and clears
  unpinned columns to its right without focusing the populated editor. The XPath editor occupies
  the same 250-pixel area used by class images, with results below it. The result column contains a pill-style
  authored/lowered switch; switching or resubmitting replaces the results below the editor in
  that same column and updates the URL.
- The favicon is the idle white dog cropped from Jason of GDN's CC0
  [Dog Spritesheets](https://opengameart.org/content/dog-spritesheets).
- Class lookup accepts an internal-capital abbreviation such as `MaFG` for
  `MagneticFieldGenerators`.
- `queryExamples.kt` holds the curated examples. Keep distinct search ideas rather than tag/resource
  substitutions. Match each English description to what its XPath actually proves; structural
  searches do not establish playability, sum separate gains, or follow triggered consequences.
- The editor explicitly selects authored or elaborated trees. Each form reuses a lazy `CanonQuery`.
  Results request metadata only, never serialized XML. Cards/classes link to details; nested nodes
  show their owning class and Pets spelling, attributes show their owner and value, and scalars
  remain visible even when false or zero. Empty matches and malformed XPath have visible outcomes.
  Resubmitting a query retains its column DOM and columns to its left.

Run `./gradlew -p pets :almanac:test` for English card text, reference classification, and every example in
its selected XPath 1.0 tree. Representative checks cover nested production, action costs, subtype
joins, tag contents, and scoring. Exercise editor selection/editing/submission, column navigation,
history/refresh, errors, scalar results, and image loading in a live browser when changing the app.
For layout changes, check different column widths, long text, light/dark persistence, and a narrow viewport.

## Publication

The standalone production build is copied to `pets/almanac/` in the Martian Zoo site repository
(`/Users/dev/martianzoo/site`). Build with `./gradlew -p pets :almanac:jsBrowserDistribution`, then copy
the contents of Almanac's `dist/js/productionExecutable` directory, preserving relative asset
paths. The site publishes `main` through GitHub Pages at `https://www.martianzoo.dev/pets/almanac/`.
After pushing the site commit, wait for its Pages build to finish and verify the live app.
