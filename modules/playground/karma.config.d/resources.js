// Exercise the same packaged recordings that the Playground loads in a browser.
config.files.push({
  pattern: "kotlin/composeResources/**/*",
  included: false,
  served: true,
  watched: false,
});
config.proxies["/composeResources/"] = "/base/kotlin/composeResources/";
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });

// Reuse the original live-game fixtures shared with JVM search tests.
config.files.push({
  pattern: "kotlin/replays/*.gz",
  included: false,
  served: true,
  watched: false,
});
