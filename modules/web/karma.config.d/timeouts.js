config.set({
  browserDisconnectTimeout: 180000,
  browserNoActivityTimeout: 300000,
  client: Object.assign(config.client || {}, {
    mocha: Object.assign((config.client && config.client.mocha) || {}, {
      timeout: 300000,
    }),
  }),
});
