describe("mvAttribute", function () {
    var expect = chai.expect, $compile, $rootScope, $scope, attr, qb,
        $templateCache, elm;

    beforeEach(module("martVisualEnrichment.directives"));
    beforeEach(module(function ($provide) {
        qb = {
            q: {},
            setAttribute: function (name) {
                this.q[name] = name;
            },
            rmAttribute: function (name) {
                delete this.q[name]
            }
        };

        $provide.value("queryBuilder", qb);
    }));

    beforeEach(inject(function (_$compile_, _$rootScope_, _$templateCache_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $templateCache = _$templateCache_;
        $scope = $rootScope.$new();

        $templateCache.put(
            'mart-visual-enrichment/app/partials/attribute.html',
            window.__html__['app/partials/attribute.html']);
        attr = {
            "name": "Gene Ontology (GO)",
            "displayName": "Gene Ontology (GO)",
            "description": "",
            "isHidden": false,
            "linkURL": "",
            "selected": false,
            "value": "",
            "attributes": [],
            "dataType": "STRING",
            "function": "annotation",
            "parent": "Gene List"
        }
        $scope.attr = attr;
        elm = $compile('<div mv-attribute="attr"></div>')($scope);
        $rootScope.$apply();
    }));


    it ("should have the proper name", function () {
        expect(elm.find("span").text()).to.eql(attr.displayName);
    });


    it ("should be unchecked when selected property is false", function () {
        var scope = elm.scope();
        expect(scope.value).to.be.null;
    });


    it ("should be checked when selected property is true", function () {
        attr.selected = true;
        elm = $compile('<div mv-attribute="attr"></div>')($scope);
        $rootScope.$apply();
        var scope = elm.scope();
        expect(scope.value).to.eql(attr.name);
    });


    it ("#setAttribute(name) registers/unregisters itself to the queryBuilder", function () {
        var scope = elm.scope();
        scope.setAttribute(attr.name)
        expect(qb.q).to.have.property(attr.name);
        scope.setAttribute()
        expect(qb.q).to.not.have.property(attr.name);
    })



});