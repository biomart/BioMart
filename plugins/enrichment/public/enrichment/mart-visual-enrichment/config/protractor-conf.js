exports.config = {
    allScriptsTimeout: 120000,

    specs: [
        "../test/e2e/*.js"
    ],

    capabilities: {
        "browserName": "chrome"
    },

    baseUrl: "http://localhost:9000/",

    framework: "jasmine",

    jasmineNodeOpts: {
        defaultTimeoutInterval: 15000,
        forceExit: true
    }
};
