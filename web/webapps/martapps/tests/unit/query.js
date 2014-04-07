module('query');

test('Compile XML with config', function() {
    var expectedXml = '<!DOCTYPE Query><Query client="test" processor="TSV" limit="-1" header="1"><Dataset name="test1,test2" config="testconfig"><Filter name="filter1" value="1"/><Attribute name="foo"/><Attribute name="bar"/></Dataset></Query>';

    var xml1 = biomart.query.compileSingleMartXML({
        config: 'testconfig',
        datasets: ['test1', 'test2'],
        attributes: {
            foo: {
                name: 'foo'
            },
            bar: {
                name: 'bar'
            }
        },
        filters: {
            filter1: {
                name: 'filter1',
                value: '1'
            }
        }
    }, 'TSV', -1, 1, 'test');

    var xml2 = biomart.query.compile('XML', {
        config: 'testconfig',
        datasets: 'test1,test2',
        attributes: [{name: 'foo'}, {name: 'bar'}],
        filters: [{name: 'filter1', value: '1'}]
    }, 'TSV', -1, 1, 'test');

    equals(xml1, expectedXml, 'Dataset array, JSON attributes and filters');
    equals(xml2, expectedXml, 'String dataset list, array attributes and filters');
});

test('Compile XML without config', function() {
    var expectedXml = '<!DOCTYPE Query><Query client="test" processor="TSV" limit="-1" header="1"><Dataset name="test1,test2" ><Filter name="filter1" value="1"/><Attribute name="foo"/><Attribute name="bar"/></Dataset></Query>';

    var xml = biomart.query.compile('XML', {
        datasets: ['test1', 'test2'],
        attributes: {
            foo: {
                name: 'foo'
            },
            bar: {
                name: 'bar'
            }
        },
        filters: {
            filter1: {
                name: 'filter1',
                value: '1'
            }
        }
    }, 'TSV', -1, 1, 'test');

    equals(xml, expectedXml, 'Dataset array, JSON attributes and filters');
});

test('Compile XML with attribute list', function() {
    var expectedXml = '<!DOCTYPE Query><Query client="test" processor="TSV" limit="-1" header="1"><Dataset name="test1,test2" ><Attribute name="faz"/><Attribute name="baz"/><Attribute name="bar"/></Dataset></Query>';

    var xml = biomart.query.compile('XML', {
        datasets: ['test1', 'test2'],
        attributes: {
            foo: {
                name: 'foo',
                attributes: [
                    {name: 'faz', displayName: 'Faz'},
                    {name: 'baz', displayName: 'Baz'}
                ]
            },
            bar: {
                name: 'bar'
            }
        }
    }, 'TSV', -1, 1, 'test');

    equals(xml, expectedXml, 'Dataset array, JSON attributes and filters');
});

/* Comment out for now
test('Compiling SPARQL query', function() {
    var expectedSparql = "SELECT ?a0 ?a1 \nFROM datasets:test1\nFROM datasets:test2\nWHERE {\n  ?mart objects:foo \"1\" .\n  ?mart objects:foo ?a0 .\n  ?mart objects:bar ?a1\n}\n";

    var sparql = biomart.query.compileSingleMartSPARQL({
        datasets: 'test1,test2',
        attributes: [{name: 'foo'}, {name: 'bar'}],
        filters: [{name: 'foo', value: '1'}]
    }, 'RDF', -1);

    // Remove the trailing PREFIX lines from the result, since they contain
    // the URL of the mart and I do not want to fiddle around with that.
    // There are two prefixes: 'datasets:' and 'objects:'
    sparql = sparql.replace(/^PREFIX \w+: <[^#]+#>\n/, '');
    sparql = sparql.replace(/^PREFIX \w+: <[^#]+#>\n/, '');

    equals(sparql, expectedSparql, 'String dataset list, array attributes and filters');
});
*/

test('Normalize query object', function() {
    var obj = {
        datasets: ['foo', 'bar', 'faz', 'baz'],
        filters: {
            e: {name: 'e', displayName: 'E', type: 'text'},
            g: {name: 'g', displayName: 'G', type: 'text'},
            f: {name: 'f', displayName: 'F', type: 'text'},
            h: {name: 'h', displayName: 'H', type: 'text'}
        },
        attributes: {
            a: {name: 'a', displayName: 'A'},
            c: {name: 'c', displayName: 'C'},
            b: {name: 'b', displayName: 'B'},
            d: {name: 'd', displayName: 'D'}
        }
    };

    var expected = {
        datasets: 'foo,bar,faz,baz',
        params: [],
        filters: [
            {name: 'e', displayName: 'E', type: 'text'},
            {name: 'g', displayName: 'G', type: 'text'},
            {name: 'f', displayName: 'F', type: 'text'},
            {name: 'h', displayName: 'H', type: 'text'}
        ],
        attributes: [
            {name: 'a', displayName: 'A'},
            {name: 'c', displayName: 'C'},
            {name: 'b', displayName: 'B'},
            {name: 'd', displayName: 'D'}
        ]
    };

    deepEqual(biomart.query.normalizeQueryMartObject(obj), expected, 'Normalize mart object for querying');
});

