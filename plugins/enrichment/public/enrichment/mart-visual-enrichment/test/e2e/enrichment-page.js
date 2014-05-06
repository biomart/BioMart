
var fs = require("fs");
var path = require("path");
require("./support");

var bedPath = path.resolve(__dirname, "../data/bed.txt");
var setSymPath =path.resolve(__dirname, "../data/input_hgnc_ss.txt");
var bkSymPath = path.resolve(__dirname, "../data/input_hgnc.txt");
var bed = fs.readFileSync(bedPath, {encoding: "utf8"});
var setSym = fs.readFileSync(setSymPath, {encoding: "utf8"});
var bkSym = fs.readFileSync(bkSymPath, {encoding: "utf8"});

module.exports = {
  data: {
    bedPath: bedPath,
    bed: bed,
    setSymPath: setSymPath,
    setSym: setSym,
    bkSymPath: bkSymPath,
    bkSym: bkSym
  },

  EnrichmentPage: EnrichmentPage
};

function EnrichmentPage() {

  "use strict";

  this.heading          = function () {
    return element(by.xpath('//h1[contains(.,"Enrichment")]'));
  };

  this.bedTab           = function () {
    return element(by.xpath('//h3[contains(.,"BED")]'));
  };

  this.geneListTab      = function () {
    return element(by.xpath('//h3[contains(.,"Gene List")]'));
  };

  this.backgroundTab    = function () {
    return element(by.xpath('//h3[contains(.,"Background")]'));
  };

  this.annotationTab    = function () {
    return element(by.xpath('//h3[contains(.,"Annotation")]'));
  };

  this.filtersTab       = function () {
    return element(by.xpath('//h3[contains(.,"Filters")]'));
  };

  this.upstream         = function () {
    return element(by.css('.mve-upstream input[type="text"]'));
  };

  this.downstream       = function () {
    return element(by.css('.mve-downstream input[type="text"]'));
  };

  this.cutoff           = function () {
    return element(by.css('.mve-filters .mve-cutoff input[type="text"]'));
  };

  this.geneListInput    = function () {
    return element(by.css(".mve-sets-form textarea"));
  };

  this.geneListUpload   = function () {
    return $('.mve-sets-form input[type="file"]');
  };

  this.geneListType     = function () {
    return element(by.css(".mve-sets-form select"));
  };

  this.bedInput         = function () {
    return element(by.css(".mve-bed-form textarea"));
  };

  this.bedUpload        = function () {
    return $('.mve-bed-form input[type="file"]');
  };

  this.backgroundInput  = function () {
    return element(by.css(".mve-background textarea"));
  };

  this.speciesSelect    = function () {
    return element(by.css(".mve-species select"));
  };

  this.annotationInputs = function () {
    return this.annotationTab().click()
      .then(function () { return browser.sleep(1000); })
      .then(function () {
        return element.all(by.css('.mve-annotations input[type="checkbox"]'));
      });
  };

  this.submitButton     = function () {
    return element(by.xpath('//button[contains(.,"Submit")]'));
  };

  this.clearButton     = function () {
    return element(by.xpath('//button[contains(.,"Clear")]'));
  };

  this.url = function () {
    return browser.getCurrentUrl();
  };

  this.submit = function () {
    return this.submitButton().click();
  };

  this.clear = function () {
    return this.clearButton().click();
  };

  this.setQueryParam = function (key, val) {
    return browser.executeScript(function () {
      var key = arguments[0], val = arguments[1],
          hash = location.hash,
          keyReg = new RegExp("&"+key+"=", "i"),
          paramReg;

      if (keyReg.test(hash)) {
        paramReg = new RegExp("&"+key+"=.*(&)?", "i");
        hash = hash.replace(paramReg, "&"+key+"="+val+"$1");
      } else {
        hash = hash.concat("&"+key+"="+val);
      }

      location.hash = hash;

    }, key, val);
  };

  this.getGeneList = function () {
    return this.geneListInput().getAttribute("value");
  };

  this.selectGeneTypeOption = function (idx) {
    return this.geneListType().findElement(by.css('option[value="'+idx+'"]'))
              .click();
  };

  this.setCutoff = function (value) {
    var page = this;
    return this.filtersTab().click()
      .then(function () { return browser.sleep(1000); })
      .then(function () { return page.cutoff().sendKeys(value.toString()); })
      .then(function () { return $("body").click(); });
  };

  this.setGeneList = function (value) {
    var page = this;
    return this.geneListTab().click().then(function () {
      return page.geneListInput().sendKeys(value);
    });
    // return this.geneListTab.click().then(function () {
    //   browser.executeScript(function () {
    //     document.querySelector(".mve-sets-form textarea").value = arguments[0];
    //   }, value);
    // });
  };

  this.getBed = function () {
    return this.bedInput().getAttribute("value");
  };

  this.setBed = function (value) {
    var page = this;
    return this.bedTab().click().then(function() {
      return page.bedInput().sendKeys(value);
    });
    // return this.bedTab.click().then(function () {
    //   browser.executeScript(function () {
    //     document.querySelector(".mve-bed-form textarea").value = arguments[0];
    //   }, value);
    // });
  };

  this.bedUploadFile = function (path) {
    var page = this;
    return this.bedTab().click().then(function() {
      return page.bedUpload().sendKeys(path);
    });
  };

  this.geneListUploadFile = function (path) {
    return this.geneListTab().click().then(function () {
      return this.geneListUpload().sendKeys(path);
    }.bind(this));
  };

  this.checkAnnotations = function () {
    var clickPromises = [];
    return this.annotationInputs().then(function (anns) {
      anns.forEach(function (el) {
        clickPromises.push(el.click());
      });
    }).then(function () {
      return protractor.promise.all(clickPromises);
    });
  };

  this.numberOfAnnotationCheckbox = function () {
    var page = this;
    this._numOfAnns = this._numOfAnns || this.annotationTab().click().then(function () {
      return page.annotationInputs().then(function (els) { return els.length });
    });
    return this._numOfAnns;
  };

  this.testFillHuman = function (opt) {
    var page = this, cutoff = opt.cutoff || 0.05;
    return protractor.promise.all([
      page.setCutoff(cutoff),
      opt.bed ? page.bedUploadFile(bedPath) : page.geneListUploadFile(setSymPath),
      page.checkAnnotations()])
    .then(function () {
      return page.url().then(function (url) {
        return decodeURIComponent(url);
      })})
    .then(function (url) {
      return page.numberOfAnnotationCheckbox().then(function (annNumber) {
        expect(url).toMatch("cutoff="+cutoff);
        opt.bed || expect(url).toMatch(/sets=0/i);
        expect(url).toMatch("(annotation=.*){"+ annNumber +"}");
      });
    });
  };

  this.expectVisualizationTabHeadings = function (tabNum) {
    return browser.findElements(by.css(".mve-tab-heading")).then(function (els) {
      console.log("ASSERTION");expect(els.length).toBe(tabNum);
    })
    // return element.all(by.css(".mve-tab-heading")).count()
    //   .then(function (count) { console.log("ASSERTION");expect(count).toBe(tabNum); });
  };
}