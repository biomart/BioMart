describe("findBioElement service", function () {
    chai.should();

    var find, conts, reqs;

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(inject(function ($injector) {
        find = $injector.get("findBioElement");
        conts = fixtures.containers();
        reqs = ["background", "sets", "bonferroni", "cutoff", "annotation"];
    }));

    describe("#addFunctions(funcs)", function () {
        it ("stores the requirements argument in a map", function () {
            var finder = find(conts), m;
            finder.addFunctions(reqs);
            m = finder.getFunctionMap();
            Object.keys(fixtures.fns()).forEach(function (e) {
                m.should.have.property(e).and.property("attributes");
                m.should.have.property(e).and.property("filters");
            });
        });
    });

    describe("#find()", function () {
        it ("gets elements with proper function property", function () {
            var finder = find(conts);
            finder.addFunctions(reqs);
            var els = finder.find();
            var r = fixtures.fns();
            els.should.have.property("annotation");
            els.annotation.attributes.should.have.length(3);
            els.annotation.filters.should.have.length(0);
            r.annotation.should.eql(els.annotation.attributes)

            els.should.have.property("background");
            els.background.attributes.should.have.length(0);
            els.background.filters.should.have.length(1);
            r.background.should.eql(els.background.filters);

            els.should.have.property("sets");
            els.sets.attributes.should.have.length(0);
            els.sets.filters.should.have.length(2);
            r.sets.should.eql(els.sets.filters)

            els.should.have.property("cutoff");
            els.cutoff.attributes.should.have.length(0);
            els.cutoff.filters.should.have.length(1);
            r.cutoff.should.eql(els.cutoff.filters)
        });
    });
})