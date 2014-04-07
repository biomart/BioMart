module('core');

test('UUID', function() {
    expect(2);

    ok(biomart.uuid(), 'Exists');
    ok(biomart.uuid() != biomart.uuid(), 'Generate unique');
});

test('Number formatting', function() {
    expect(6);

    var f = biomart.number.format;
    equals(f(0.192, {decimals: 2}), '0.19', 'Rounding down to two decimals');
    equals(f(0.197, {decimals: 2}), '0.20', 'Rounding up to two decimals');
    equals(f(0.005, {decimals: 2}), '0.01', 'Rounding from three to two decimals');
    equals(f(0.001, {decimals: 2}), '0.00', 'Rounding from three to two decimals');
    equals(f(1000000, {separateThousands: true}), '1,000,000', 'Separating thousands with commas');
    equals(f(9876.54321, {decimals: 4, separateThousands: true}), '9,876.5432', 'Separate thousands and round');
});

test('Queue', function() {
    expect(1);

    var queue = new biomart.Queue($('#test'), 'test'),
        arr = [];
    for (var i=0; i<10; i++) {
        queue.queue((function(i) {
            return function() {
                setTimeout(function() {
                    arr.push(i);
                    if (i==9) {
                        same(arr, [0,1,2,3,4,5,6,7,8,9], 'Queued async array push');
                        start();
                    } else {
                        queue.dequeue();
                    }
                }, ~~(Math.random() * 100) + 15);
            };
        })(i));
    }
    stop();
    queue.dequeue();
});

test('Partial application', function() {
    expect(2);

    var x = ~~(Math.random() * 10),
        y = ~~(Math.random() * 10),
        z = ~~(Math.random() * 10);

    var g = f.partial(x),
        h = g.partial(y);

    equals(f(x, y, z), g(y, z), 'Partial application on f()');
    equals(g(y, x), h(x), 'Partial application on g()');

    function f(a, b, c) {
        return a + b + c;
    }
});

test('Currying', function() {
    expect(1);

    var x = ~~(Math.random() * 10),
        y = ~~(Math.random() * 10),
        z = ~~(Math.random() * 10);

    equals(f.curry()(x)(y)(z), f(x, y, z), 'Currying on f()');

    function f(a, b, c) {
        return a + b + c;
    }
});

test('Array remove', function() {
    expect(1);

    var arr = [1,2,3,4];
    arr.remove(1);
    deepEqual(arr, [1,3,4], 'Removing element from array');
});

test('Namespacing', function() {
    expect(3);

    $.namespace('foo.bar');
    deepEqual(foo, {bar:{}}, 'Empty foo.bar namespace');
    foo.bar = {
        faz: 1
    };
    $.namespace('foo.bar', {baz:2});
    deepEqual(foo.bar, {faz:1, baz:2}, 'Adding to existing namespace');
    $.namespace('foo.hello', function(self) {
        self.world = 'Hello World!';
        self.arr = [1,2,3];
    });
    deepEqual(foo, {
        bar: {
            faz: 1,
            baz:2
        },
        hello: {
            world: 'Hello World!',
            arr: [1,2,3]
        }
    }, 'Namespacing with function');
});

asyncTest('Pub/Sub', function() {
    expect(5);

    var i = 0;

    var myObj = {
        f: function(a) { 
            ok(a == 2, 'Argument passed through publish');
        },
        g: function(b) {
            ok(b == 'arg', 'Argument bounded by subscribe');
        },
        h: function(c, d) {
            ok(c == 'one' && d == 'two', 'Bounded and passed arguments');
        },
        F: function() {
            ok(true, 'Namespace test: foo');
        },
        G: function() {
            if (++i == 2) {
                ok(true, 'Namepace test: foo.bar');
                start();
            }
        }
    };

    $.subscribe('test', myObj, 'f');
    $.subscribe('test2', myObj, 'g', 'arg');
    $.subscribe('test3', myObj, 'h', 'one');
    $.subscribe('foo', myObj, 'F');
    $.subscribe('foo.bar', myObj, 'G');

    $.publish('test', 2);
    $.publish('test2');
    $.publish('test3', 'two');
    $.publish('foo');
    $.publish('foo.bar');
});

test('Utility functions', function() {
    expect(3);

    BIOMART_CONFIG.labels.message = 'Hello World!';
    equals(_('message'), BIOMART_CONFIG.labels.message, 'Label function');

    equals(biomart.stripHtml('<span>test</span>'), 'test', 'Strip HTML tags (simple)');
    equals(biomart.stripHtml('<p>This <a href="#" class="link">is</a> <em><strong>a</strong></em> longer test.</p>'), 
        'This is a longer test.', 'Strip HTML tags (longer)');
});
