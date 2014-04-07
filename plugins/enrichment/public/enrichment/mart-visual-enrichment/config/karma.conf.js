module.exports = function(config){
    config.set({
    basePath : "../",

    files : [
        "app/lib/angular/angular.js",
        "app/lib/angular/angular-*.js",
        "app/lib/jquery.js",
        "test/lib/angular/angular-mocks.js",
        "test/lib/chai.js",
        "test/lib/sinon.js",
        "test/unit/fixtures.js",
        "app/js/services.js",
        "app/js/services/find-bio-element.js",
        "app/js/services/query-validator.js",
        "app/js/controllers.js",
        "app/js/controllers/species.js",
        "app/js/controllers/enrichment.js",
        "app/js/directives.js",
        "app/js/directives/mv-species.js",
        "app/js/directives/filters.js",
        "app/js/directives/mv-filter.js",
        "app/js/directives/mv-attribute.js",
        "app/partials/**/*.html",
        "test/unit/services/find-bio-element-spec.js",
        "test/unit/services/query-validator-spec.js",
        "test/unit/controllers/species*.js",
        "test/unit/controllers/enrichment*.js",
        "test/unit/directives/mv-species-spec.js",
        "test/unit/directives/filters-spec.js",
        "test/unit/directives/mv-filter-spec.js",
        "test/unit/directives/mv-attribute-spec.js"
        // "app/js/**/*.js",
        // "test/unit/services/**/*.js",
        // "test/unit/controllers/**/*.js",
        // "test/unit/directives/**/*.js",
        // "app/partials/*.html"
    ],

    exclude : [
        "app/lib/angular/*.min.js",
        "app/lib/angular/angular-scenario.js"
    ],

    autoWatch : true,

    frameworks: ["mocha"],

    browsers : ["Chrome"],

    plugins : [
        "karma-junit-reporter",
        "karma-chrome-launcher",
        "karma-firefox-launcher",
        "karma-safari-launcher",
        "karma-mocha-reporter",
        "karma-mocha",
        "karma-html2js-preprocessor"
    ],

    reporters: ["mocha"],

    junitReporter : {
        outputFile: "test_out/unit.xml",
        suite: "unit"
    },

    preprocessors: {
        "app/partials/*.html": ["html2js"]
    }

})}
