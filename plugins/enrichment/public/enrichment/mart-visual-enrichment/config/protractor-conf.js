exports.config = {
    allScriptsTimeout: 11000,

    specs: [
        "../test/e2e/*.js"
    ],

    capabilities: {
        "browserName": "chrome"
    },

    baseUrl: "http://localhost:8000/test/",

    framework: "jasmine",

    jasmineNodeOpts: {
        defaultTimeoutInterval: 30000
    }
};
