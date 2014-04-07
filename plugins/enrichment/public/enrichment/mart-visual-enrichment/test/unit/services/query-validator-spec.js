describe("queryValidator service", function () {
    var expect = chai.expect, elms, qv, sets, bed_regions, cutoff, filters;

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(inject(function ($injector) {
        qv = $injector.get("queryValidator");
        filters = {};
        sets = {
            name: "set filter",
            function: "sets"
        }
        bed_regions = {
            name: "bed filter",
            function: "bed_regions"
        };
        cutoff = {
            name: "cutoff filter",
            function: "cutoff"
        };
        elms = {
            attributes: {foo: {function: "annotation"}},
            filters: filters
        }
    }));

    it ("should ignore elements with null value", function () {
        filters.foo = null, filters.sets = sets, filters.cutoff = cutoff;
        expect(qv.validate(elms)).to.be.true;
    })

    describe("valid case", function () {
        it ("when sets and cutoff are present", function () {
            filters.sets = sets, filters.cutoff = cutoff;
            expect(qv.validate(elms)).to.be.true;
            expect(qv.errMessage()).to.be.empty;
        });

        it ("when bed_regions and cutoff are present", function () {
            filters.bed_regions = bed_regions, filters.cutoff = cutoff;
            expect(qv.validate(elms)).to.be.true;
            expect(qv.errMessage()).to.be.empty;
        });
    });


    describe("invalid case", function () {
        it ("when there are no filters", function () {
            expect(qv.validate(elms)).to.be.false;
            expect(qv.errMessage()).to.not.be.empty;
        })

        it ("when sets, bed_regions and cutoff are present", function () {
            filters.sets = sets, filters.cutoff = cutoff, filters.bed_regions = bed_regions;
            expect(qv.validate(elms)).to.be.false;
            expect(qv.errMessage()).to.not.be.empty;
        });

        it ("when cutoff is missing", function () {
            filters.bed_regions = bed_regions;
            expect(qv.validate(elms)).to.be.false;
            expect(qv.errMessage()).to.not.be.empty;
        });
    });

});