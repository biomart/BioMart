module('url');

test('jsonify', function() {
    expect(4);
    var json = biomart.url.jsonify('http://biomart.org/path/?foo=bar&faz=baz#!/fragment/');
    equals(json.host, 'http://biomart.org/', 'Host');
    equals(json.path, 'path/', 'Path');
    equals(json.query, 'foo=bar&faz=baz', 'Query string');
    equals(json.fragment, 'fragment/', 'Fragment');
});

test('stringify', function() {
    var json = {
        host: 'http://test.com/',
        path: 'this/is/the/path/',
        query: 'a=1&b=2&c=3',
        fragment: 'hello/world'
    };
    equals(biomart.url.stringify(json), 'http://test.com/this/is/the/path/?a=1&b=2&c=3#!/hello/world');
});

test('Simple URL params', function() {
    var obj = biomart.url.simpleQueryParams('foo=bar&faz=baz');
    deepEqual(obj, { foo: 'bar', faz: 'baz' }, 'deserializing');
});
