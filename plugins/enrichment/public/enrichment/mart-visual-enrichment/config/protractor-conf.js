var bigTimeout = 180 * 1000; // ms
var allScriptsTimeout = 10e5;

exports.config = {
    allScriptsTimeout: allScriptsTimeout,

    specs: [
        "../test/e2e/*.js"
    ],

    // capabilities: [
    //     // { "browserName": "chrome" },
    //     { "browserName": "firefox" }
    // ],

    capabilities: { "browserName": "firefox" },

    baseUrl: "http://ec2-54-225-80-160.compute-1.amazonaws.com:9100/",

    framework: "jasmine",

    jasmineNodeOpts: {
        defaultTimeoutInterval: bigTimeout,
        isVerbose: true
    }
};
