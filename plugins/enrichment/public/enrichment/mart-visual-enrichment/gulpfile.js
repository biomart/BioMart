var gulp = require("gulp")
var shell = require("shelljs/global")
var uglify = require("gulp-uglify");
var concat = require("gulp-concat");
var jshint = require("gulp-jshint");

var srcPaths = ["app/**/*.{js,css,html}", "app/*.{js,css,html}"]

var buildFiles = [
    
    "app/lib/localforage.js",
    "app/lib/angular-localForage.js",

    "app/js/app.js",

    "app/js/services.js",
    "app/js/services/bmservice.js",
    "app/js/services/mv-config.js",
    "app/js/services/find-bio-element.js",
    "app/js/services/query-store.js",
    "app/js/services/query-builder.js",
    "app/js/services/query-validator.js",
    "app/js/services/sanitize.js",
    "app/js/services/store-pusher.js",

    "app/js/controllers.js",
    "app/js/controllers/species.js",
    "app/js/controllers/enrichment.js",
    "app/js/controllers/query.js",

    "app/js/directives.js",
    "app/js/directives/mv-species.js",
    "app/js/directives/filters.js",
    "app/js/directives/mv-filter.js",
    "app/js/directives/mv-attribute.js",

    "app/lib/d3-tip/index.js",

    "app/js/services/terms-async.js",
    "app/js/services/terms.js",
    "app/js/services/progress-state.js",

    "app/js/controllers/progress.js",
    "app/js/controllers/visualization.js",
    "app/js/controllers/results-table.js",
    "app/lib/file-saver.js",
    "app/lib/canvg-1.3/rgbcolor.js",
    "app/lib/canvg-1.3/StackBlur.js",
    "app/lib/canvg-1.3/canvg.js",
    "app/lib/canvas-toBlob.js",
    "app/js/controllers/graph.js",

    "app/js/directives/mv-graph.js",
    "app/js/directives/mv-results-table.js"

];

gulp.task("build", function() {
  gulp.src(buildFiles)
    .pipe(uglify())
    .pipe(concat("mart-visual-enrichment.min.js"))
    .pipe(gulp.dest("dist/"));
});

gulp.task("compile", function () {
    var currDir = pwd();
    cd("../../../../..")
    if (exec("ant").code !== 0) {
        echo("There has been an error with ant compilation");
    }
    cd(currDir)
})

gulp.task("ant", function () {
    gulp.watch(srcPaths, ["compile"]).
        on("change", function (evt) {
            console.log("File "+evt.path+" was "+evt.type+", running tasks...");
        })
})

gulp.task('lint-job', function() {
    gulp.src('app/js/**/*.js')
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
})

gulp.task("lint", function () {
    gulp.watch(srcPaths, ["lint-job"])
})

gulp.task("default", ["lint", "ant"])