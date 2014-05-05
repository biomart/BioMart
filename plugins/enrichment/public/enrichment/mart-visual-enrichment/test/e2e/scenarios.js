'use strict';

var fs = require("fs");
var path = require("path");
var bedPath = path.resolve(__dirname, "../data/bed.txt");
var setSymPath =path.resolve(__dirname, "../data/input_hgnc_ss.txt");
var bkSymPath = path.resolve(__dirname, "../data/input_hgnc.txt");
var bed = fs.readFileSync(bedPath, {encoding: "utf8"});
var setSym = fs.readFileSync(setSymPath, {encoding: "utf8"});
var bkSym = fs.readFileSync(bkSymPath, {encoding: "utf8"});


function EnrichmentPage() {

  "use strict";

  this.heading = element(by.xpath('//h1[contains(.,"Enrichment")]'));
  this.bedTab = element(by.xpath('//h3[contains(.,"BED")]'));
  this.geneListTab = element(by.xpath('//h3[contains(.,"Gene List")]'));
  this.backgroundTab = element(by.xpath('//h3[contains(.,"Background")]'));
  this.annotationTab = element(by.xpath('//h3[contains(.,"Annotation")]'));
  this.filtersTab = element(by.xpath('//h3[contains(.,"Filters")]'));
  this.upstream = element(by.css('.mve-upstream input[type="text"]'));
  this.downstream = element(by.css('.mve-downstream input[type="text"]'));
  this.cutoff = element(by.css('.mve-filters .mve-cutoff input[type="text"]'));
  this.geneListInput = element(by.css(".mve-sets-form textarea"));
  this.geneListUpload = $('.mve-sets-form input[type="file"]');
  this.geneListType = element(by.css(".mve-sets-form select"));
  this.bedInput = element(by.css(".mve-bed-form textarea"));
  this.bedUpload = $('.mve-bed-form input[type="file"]');
  this.backgroundInput = element(by.css(".mve-background textarea"));
  this.speciesSelect = element(by.css(".mve-species select"));
  this.annotationInputs = element.all(by.css('.mve-annotations input[type="checkbox"]'));
  this.submitButton = element(by.xpath('//button[contains(.,"Submit")]'));

  this.url = function () {
    return browser.getCurrentUrl();
  };

  this.submit = function () {
    return this.submitButton.click();
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
    return this.geneListInput.getAttribute("value");
  };

  this.selectGeneTypeOption = function (idx) {
    return this.geneListType.findElement(by.css('option[value="'+idx+'"]'))
              .click();
  };

  this.setCutoff = function (value) {
    var page = this;
    return this.filtersTab.click().then(function () {
      page.cutoff.sendKeys(value.toString());
      // blur the input to point that we finished
      return $("body").click();
    });
  };

  this.setGeneList = function (value) {
    var page = this;
    return this.geneListTab.click().then(function () {
      return page.geneListInput.sendKeys(value);
    });
    // return this.geneListTab.click().then(function () {
    //   browser.executeScript(function () {
    //     document.querySelector(".mve-sets-form textarea").value = arguments[0];
    //   }, value);
    // });
  };

  this.getBed = function () {
    return this.bedInput.getAttribute("value");
  };

  this.setBed = function (value) {
    var page = this;
    return this.bedTab.click().then(function() {
      return page.bedInput.sendKeys(value);
    });
    // return this.bedTab.click().then(function () {
    //   browser.executeScript(function () {
    //     document.querySelector(".mve-bed-form textarea").value = arguments[0];
    //   }, value);
    // });
  };

  this.bedUploadFile = function (path) {
    return this.bedUpload.sendKeys(path);
  };

  this.geneListUploadFile = function (path) {
    return this.geneListUpload.sendKeys(path);
  };

  this.checkAnnotations = function () {
    var page = this, clickPromises = [];
    this.annotationTab.click().then(function () {
      page.annotationInputs.each(function (el) {
        clickPromises.push(el.click());
      });
    });
    return protractor.promise.all(clickPromises);
  };

  this.testSubmitHumanOnlyGeneList = function () {
    var page = this;
    return protractor.promise.all([
      page.geneListUploadFile(setSymPath),
      page.checkAnnotations(),
      page.setCutoff(0.05)])
    .then(function () {
      page.url().then(function (url) {
        return decodeURIComponent(url);
      })
      .then(function (url) {
        page.annotationInputs.count().then(function (annNumber) {
          expect(url).toMatch(/cutoff=0.05/i);
          expect(url).toMatch(/sets=0/i);
          expect(url).toMatch(/cutoff=0.05/i);
          expect(url).toMatch(
            new RegExp("(annotation=.*){"+ annNumber +"}", "i")
          );
          page.submit();
        });
      });
    });
  };


}

protractor.promise.all = function(arr) {
  var n = arr.length;
  if (!n) {
    return protractor.promise.fulfilled([]);
  }

  var toFulfill = n;
  var result = protractor.promise.defer();
  var values = [];

  var onFulfill = function(index, value) {
    values[index] = value;
    toFulfill--;
    if (toFulfill == 0) {
      result.fulfill(values);
    }
  };

  function partial (fn) {
    var args = [].slice.call(arguments, 1);
    return function () {
      fn.apply(null, args.concat(arguments));
    };
  }

  for (var i = 0; i < n; ++i) {
    protractor.promise.asap(
        arr[i], partial(onFulfill, i), result.reject);
  }

  return result.promise;
};







describe('Enrichment App', function() {

  var baseUrl = "enrichment/#/gui/Enrichement/";
  var page;
  browser.get(baseUrl);

  beforeEach(function () {
    page = new EnrichmentPage();
  });

  describe ("interface", function () {
    [
      "heading", "bedTab", "geneListTab", "upstream",
      "downstream", "backgroundTab", "cutoff", "annotationTab",
      "geneListInput", "bedInput", "bedInput", "geneListInput",
      "backgroundInput", "speciesSelect", "submitButton", "filtersTab"
    ].forEach(function (el) {
      it ("has element "+ el, function () {
        expect(page[el].isPresent()).toBe(true);
      });
    });
  });

  it ("has the proper URL", function () {
    expect(page.url()).toMatch(/config=.+&species=.+/);
  });

  it ("Gene List", function () {
    expect(page.url()).toMatch(/&sets=0/);
    page.geneListUploadFile(setSymPath);

    page.selectGeneTypeOption(3);
    expect(page.url()).toMatch(/&sets=3/);

    expect(page.getGeneList()).toEqual(setSym);

    page.selectGeneTypeOption(0);
    expect(page.url()).toMatch(/&sets=0/);

    expect(page.getGeneList()).toEqual(setSym);

    page.setQueryParam("sets", 3).then(function () {
      var selOpt = element(by.css(".mve-sets"))
          .findElement(by.selectedOption("selected"));

      expect(selOpt.getAttribute("value")).toEqual("3");
      expect(page.getGeneList()).toEqual(setSym);
    });
  });

  it ("BED", function () {
    page.bedUploadFile(bedPath);
    expect(page.url()).not.toMatch(/&bed=/);
    expect(page.getBed()).toEqual(bed);
  });

  describe("Human Query", function () {
    describe("with only gene list", function () {
      beforeEach(function () {
        browser.get(baseUrl);
      });

      it ("generates the correct URL", function () {
        page.testSubmitHumanOnlyGeneList();
      });

      it ("is successful", function () {
        page.testSubmitHumanOnlyGeneList();
      });
    });
  });

});
