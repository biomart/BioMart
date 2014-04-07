#! /usr/bin/env node

"use strict";

var ejs = require("ejs"),
    fs = require("fs"),
    path = require("path"),
    opts = {
        filename: "name",
        ngPartials: [
            {
                "id": "partials/vis.html",
                path: "partials/vis.html"
            },
            {
                "id": "partials/table-of-results.html",
                path: "partials/table-of-results.html"
            }
        ]
    };

var args = process.argv;
if (args.length < 3) {
    console.error("Environment name must be provided");
    exit(1);
}

var env = process.argv[2];
var basedir = path.resolve(path.join(__dirname, ".."));
var tplDir = path.join(basedir, "partials", env);
var partialsDir = path.join(basedir, "app", "partials");
var head = fs.readFileSync(path.join(tplDir, "head.html"));
var tail = fs.readFileSync(path.join(tplDir, "tail.html"));
var enrichData = fs.readFileSync(path.join(tplDir, "enrichment-data.json"));
var includes = fs.readdirSync(partialsDir);
includes = includes.filter(function (i) {
    if (i[0] === ".") return false;
    return true;
});

var ngPartials = includes.map(function (i) {
    return {
        "id": i,
        content: fs.readFileSync(path.join(partialsDir, i), {encoding: "utf-8"})
    };
});

opts.head = head;
opts.tail = tail;
opts.ngPartials = ngPartials;
opts.enrichmentData = enrichData;


fs.readFile(path.join(basedir, "app", "_index-tmpl.html"), {encoding: "utf-8"}, function (err, data) {
    if (err) {
        console.error(err);
        process.exit(1);
    }
    var html = ejs.render(data, opts);
    fs.writeFile(path.join(basedir, "app", "index.html"), html, function (err) {
        if (err) {
            console.error(err);
            process.exit(1);
        } else {
            process.exit(0);
        }
    });
});



