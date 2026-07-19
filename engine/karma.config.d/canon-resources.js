config.set({
  files: config.files.concat([
    {
      pattern: "canon/**/*",
      included: false,
      served: true,
      watched: false,
      nocache: true,
    },
  ]),
  proxies: Object.assign(config.proxies, {
    "/canon/": "/base/canon/",
  }),
});
