config.set({
  files: config.files.concat([
    {
      pattern: "dev/**/*",
      included: false,
      served: true,
      watched: false,
      nocache: true,
    },
  ]),
  proxies: Object.assign(config.proxies, {
    "/dev/": "/base/dev/",
  }),
});
