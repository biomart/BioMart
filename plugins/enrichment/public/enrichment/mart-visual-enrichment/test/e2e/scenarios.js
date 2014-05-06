'use strict';

var data = require("./enrichment-page").data;
var EnrichmentPage = require("./enrichment-page").EnrichmentPage;

function fixIt() {
  GLOBAL.it = function() {
    return jasmine.getEnv().it.apply(jasmine.getEnv(), arguments);
  }
}

function immediateErrorOutput() {
  var theOriginalFail = jasmine.Spec.prototype.fail;
  jasmine.Spec.prototype.fail = function (e) {
    theOriginalFail.apply(this, arguments);
    console.error(e ? jasmine.util.formatException(e) : "Exception");
  };
}

fixIt();
immediateErrorOutput();

describe('Enrichment App', function() {

  var baseUrl = "enrichment/#/gui/Enrichment/";
  var page;
  browser.get(baseUrl);

  beforeEach(function () {
    browser.waitForAngular();
    page = new EnrichmentPage();
  });

  // describe ("interface", function () {
  //   [
  //     "heading", "bedTab", "geneListTab", "upstream",
  //     "downstream", "backgroundTab", "cutoff", "annotationTab",
  //     "geneListInput", "bedInput", "bedInput", "geneListInput",
  //     "backgroundInput", "speciesSelect", "submitButton", "filtersTab"
  //   ].forEach(function (el) {
  //     it ("has element "+ el, function () {
  //       expect(page[el]().isPresent()).toBe(true);
  //     });
  //   });
  // });

  // it ("has the proper URL", function () {
  //   expect(page.url()).toMatch(/config=.+&species=.+/);
  // });

  // it ("Gene List", function () {
  //   expect(page.url()).toMatch(/&sets=0/);
  //   page.geneListUploadFile(setSymPath);

  //   page.selectGeneTypeOption(3);
  //   expect(page.url()).toMatch(/&sets=3/);

  //   expect(page.getGeneList()).toEqual(setSym);

  //   page.selectGeneTypeOption(0);
  //   expect(page.url()).toMatch(/&sets=0/);

  //   expect(page.getGeneList()).toEqual(setSym);

  //   page.setQueryParam("sets", 3).then(function () {
  //     var selOpt = element(by.css(".mve-sets"))
  //         .findElement(by.selectedOption("selected"));

  //     expect(selOpt.getAttribute("value")).toEqual("3");
  //     expect(page.getGeneList()).toEqual(setSym);
  //   });
  // });

  // it ("BED", function () {
  //   page.bedUploadFile(bedPath);
  //   expect(page.url()).not.toMatch(/&bed=/);
  //   expect(page.getBed()).toEqual(bed);
  // });

  describe("Human Query", function () {
    describe("with only gene list", function () {
      var opt;

      beforeEach(function () {
        opt = {
          cutoff: 0.05
        };
        browser.get(baseUrl);
        browser.waitForAngular();
      });

      it ("is successful", function () {
        page.clear();
        page.testFillHuman(opt).then(function () {
          return page.annotationInputs()

          .then(function (els) {
            var length = els.length;
            return page.submit().then(function () {
              return length;
            });
          });

        })

        .then(function (tabNum) {
          browser.findElements(by.css(".progress-bar"))
          .then(function () {
            page.expectVisualizationTabHeadings(tabNum);
          });
        });
      });
    });


    describe("with only bed", function () {
      var opt;

      beforeEach(function () {
        opt = {
          cutoff: 0.05,
          bed: true
        };
        browser.get(baseUrl);
        browser.waitForAngular();
      });

      it ("is successful", function () {
        page.clear();
        page.testFillHuman(opt).then(function () {
          return page.annotationInputs()

          .then(function (els) {
            var length = els.length;
            return page.submit().then(function () {
              return length;
            });
          });

        })

        .then(function (tabNum) {
          browser.findElements(by.css(".progress-bar"))
          .then(function () {
            page.expectVisualizationTabHeadings(tabNum);
          });
        });
      });
    });
  });

});
