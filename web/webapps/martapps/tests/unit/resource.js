module('resource');

test('Load root GUI container', function() {
    expect(1);

    stop();
    biomart.resource.load('portal', function(data) {
        equals(data.name, 'root', 'Name should be "root"');
        start();
    });
});

test('Load marts', function() {
    expect(1);

    stop();
    biomart.resource.load('marts', function(data) {
        ok(true, 'Marts loaded');
        start();
    });
});
