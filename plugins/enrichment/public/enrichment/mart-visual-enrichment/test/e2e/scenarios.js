'use strict';

var fs = require("fs");
var path = require("path");
var bed = fs.readFileSync(path.resolve("../data/bed.txt", __filename), {encoding: "utf8"});
var setSym = fs.readFileSync(path.resolve("../data/input_hgnc_ss.txt", __filename), {encoding: "utf8"});
var bkSym = fs.readFileSync(path.resolve("../data/input_hgnc.txt", __filename), {encoding: "utf8"});


function EnrichmentPage() {
  this.heading = element(by.xpath('//h1[contains(.,"Enrichment")]'));
  this.bedTab = element(by.xpath('//h3[contains(.,"BED")]'));
  this.geneListTab = element(by.xpath('//h3[contains(.,"Gene List")]'));
  this.backgroundTab = element(by.xpath('//h3[contains(.,"Background")]'));
  this.annotationTab = element(by.xpath('//h3[contains(.,"Annotation")]'));
  this.upstream = element(by.css('.mve-upstream input[type="text"]'));
  this.downstream = element(by.css('.mve-downstream input[type="text"]'));
  this.cutoff = element(by.css('.mve-filters .mve-cutoff input[type="text"]'));
  this.geneListInput = element(by.css(".mve-sets-form textarea"));
  this.bedInput = element(by.css(".mve-bed-form textarea"));
  this.backgroundInput = element(by.css(".mve-background textarea"));
  this.speciesSelect = element(by.css(".mve-species select"))
}


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
      "backgroundInput", "speciesSelect"
    ].forEach(function (el) {
      it ("has element "+ el, function () {
        expect(page[el].isPresent()).toBe(true);
      })
    });

  });

  it ("has the proper URL", function () {
    expect(browser.getCurrentUrl()).toMatch(baseUrl + "?config=.+&species=.+&sets=0&background=0");
  });

  describe ("human query", function () {
    it ("is successful", function () {
      page.geneListTab.click();
      page.geneListInput.sendKeys(setSym);
    });
  });

  // it('should automatically redirect to /view1 when location hash/fragment is empty', function() {
  //   expect(browser.getLocationAbsUrl()).toMatch("/view1");
  // });


  // describe('view1', function() {

  //   beforeEach(function() {
  //     browser.get('index.html#/view1');
  //   });


  //   it('should render view1 when user navigates to /view1', function() {
  //     expect(element.all(by.css('[ng-view] p')).first().getText()).
  //       toMatch(/partial for view 1/);
  //   });

  // });


  // describe('view2', function() {

  //   beforeEach(function() {
  //     browser.get('index.html#/view2');
  //   });


  //   it('should render view2 when user navigates to /view2', function() {
  //     expect(element.all(by.css('[ng-view] p')).first().getText()).
  //       toMatch(/partial for view 2/);
  //   });

  // });
});
