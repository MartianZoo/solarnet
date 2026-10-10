config.set({
  files: config.files.concat([
    {
      pattern: "language/**/*",
      included: false,
      served: true,
      watched: false,
      nocache: true,
    },
  ]),
  proxies: Object.assign(config.proxies, {
    "/language/": "/base/language/",
  }),
});
