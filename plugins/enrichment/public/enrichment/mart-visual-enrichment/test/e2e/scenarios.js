'use strict';

var fs = require("fs");
var path = require("path");
var bed = fs.readFileSync(path.resolve(__dirname, "../data/bed.txt"), {encoding: "utf8"});
var setSym = fs.readFileSync(path.resolve(__dirname, "../data/input_hgnc_ss.txt"), {encoding: "utf8"});
var bkSym = fs.readFileSync(path.resolve(__dirname, "../data/input_hgnc.txt"), {encoding: "utf8"});


function EnrichmentPage() {

  "use strict";

  this.heading = element(by.xpath('//h1[contains(.,"Enrichment")]'));
  this.bedTab = element(by.xpath('//h3[contains(.,"BED")]'));
  this.geneListTab = element(by.xpath('//h3[contains(.,"Gene List")]'));
  this.backgroundTab = element(by.xpath('//h3[contains(.,"Background")]'));
  this.annotationTab = element(by.xpath('//h3[contains(.,"Annotation")]'));
  this.upstream = element(by.css('.mve-upstream input[type="text"]'));
  this.downstream = element(by.css('.mve-downstream input[type="text"]'));
  this.cutoff = element(by.css('.mve-filters .mve-cutoff input[type="text"]'));
  this.geneListInput = element(by.css(".mve-sets-form textarea"));
  this.geneListType = element(by.css(".mve-sets-form select"));
  this.bedInput = element(by.css(".mve-bed-form textarea"));
  this.backgroundInput = element(by.css(".mve-background textarea"));
  this.speciesSelect = element(by.css(".mve-species select"));

  this.url = function () {
    return browser.getCurrentUrl();
  };

  this.selectGeneTypeOption = function (idx) {
    return this.geneListType.findElement(by.css('option[value="'+idx+'"]'))
              .click();
  }

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

    // return this.url().then(function (u) {
    //   var keyReg = new RegExp("&"+key+"=", "i"),
    //     paramReg;

    //   console.log("URL: "+u);
    //   if (keyReg.test(u)) {
    //     console.log("replacing url query")
    //     paramReg = new RegExp("&"+key+"=.*(&)?", "i");
    //     u = u.replace(paramReg, "&"+key+"="+val+"$1");
    //   } else {
    //     console.log("appending...")
    //     u = u.concat("&"+key+"="+val);
    //   }

    //   return browser.get(u);
    // });
  }


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
    expect(page.url()).toMatch(/config=.+&species=.+/);
  });

  describe ("Gene List", function () {
    it ("works :P", function () {
      page.geneListTab.click();
      page.geneListInput.sendKeys(setSym);
      expect(page.url()).not.toMatch(/&sets=/);

      page.selectGeneTypeOption(3);
      expect(page.url()).toMatch(/&sets=3/);

      expect(page.geneListInput.getAttribute("value")).toEqual(setSym);

      page.selectGeneTypeOption(0);
      expect(page.url()).toMatch(/&sets=0/);
      
      expect(page.geneListInput.getAttribute("value")).toEqual(setSym);

      page.setQueryParam("sets", 3).then(function () {
        var selOpt = element(by.css(".mve-sets"))
            .findElement(by.selectedOption("selected"));

        expect(selOpt.getAttribute("value")).toEqual("3");
        expect(page.geneListInput.getAttribute("value")).toEqual(setSym);
      });

    });
  });

});
