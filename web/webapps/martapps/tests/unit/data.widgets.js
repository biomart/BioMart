(function($) {

var _test = $('#test');

module('data.widgets: datasource');

test('local data', function() {
    expect(2);
    var el = $('<div/>').appendTo(_test);

    el.datasource({
        type: 'local', 
        data: [1, 2, 3, 4, 5]
    });

    el
        .bind('datasource.success', function(ev, data) {
            deepEqual(data,[1, 2, 3, 4, 5], 'Successful data return');
        })
        .bind('datasource.complete', function() {
            ok(true, 'Complete signal triggered');
            el.datasource('destroy').remove();
        })
        .datasource('exec');
});


test('ajax success', function() {
    expect(2);
    var el = $('<div/>').appendTo(_test);

    el.datasource({
        /* defaults to ajax, POST */
        url: 'data/say.jsp', 
        dataType: 'json',
        data: {
            what: 'Hello World'
        }
    });

    el
        .bind('datasource.success', function(ev, data) {
            equals(data.what, 'Hello World', 'Successful data return');
        })
        .bind('datasource.complete', function() {
            ok(true, 'Complete signal triggered');
            el.datasource('destroy').remove();
            start();
        })
        .datasource('exec');

    stop();
});

test('ajax error', function() {
    expect(1);
    var el = $('<div/>').appendTo(_test);

    el.datasource({
        /* defaults to ajax, POST */
        url: 'data/say.jsp', 
        dataType: 'json',
        data: {
            what: 'error'
        }
    });

    el
        .bind('datasource.error', function(ev, xhr, reason, data) {
            equals(xhr.status, 400, 'Error status code returned');
            el.datasource('destroy').remove();
            start();
        })
        .datasource('exec');

    stop();
});

test('streaming', function() {
    expect(2);
    var el = $('<div/>').appendTo(_test);

    el.datasource({
        type: 'streaming',
        url: 'data/jsonp.jsp', 
        dataType: 'json',
        data: {
            what: 'Hello World'
        }
    });

    el
        .bind('datasource.success', function(ev, data) {
            equals(data.what, 'Hello World', 'Streaming data returned');
        })
        .bind('datasource.complete', function() {
            ok(true, 'Complete signal triggered');
            el.datasource('destroy').remove();
            start();
        })
        .datasource('exec');

    stop();
});


module('data.widgets: datacontroller', {
    setup: function() {
        this.element = $('<div/>').appendTo(_test);
        this.data = [
            ['Foo', 'Bar', 'Faz', 'Baz'],
            ['a', 'b', 'c', 'd'],
            ['e', 'i', 'm', 'q'],
            ['f', 'j', 'n', 'r'],
            ['g', 'k', 'o', 's'],
            ['u', 'v', 'w', 'x'],
            ['y', 'z', '0', '1'],
            ['h', 'l', 'p', 't']
        ];
    },
    teardown: function() {
        delete this.data;
        this.element
            .datacontroller('destroy')
            .remove();
        ok(true, 'destroy');
    }
});

test('table - basic', function() {
    this.element.datacontroller();
    ok(true, 'create');

    this.element
        .datacontroller('write_lines', this.data)
        .datacontroller('done');

    equals(this.element.children()[0].tagName, 'TABLE', 'Creates <table> by default');
});

test('table - limited rows with headers', function() {
    this.element
        .datacontroller({
            headers: true,
            limit: 3
        })
        .datacontroller('write_lines', this.data)
        .datacontroller('done');

    var thead = this.element.find('thead');
    ok(thead.length > 0, 'thead found');

    var tbody = this.element.find('tbody');
    ok(tbody.length > 0, 'tbody found');

    ok(tbody.find('tr').length == 3, 'Exactly three rows displayed');
});

test('caption', function() {
    this.element.datacontroller();
    ok(true, 'create');

    var data_with_header = [
        '# this is a comment',
        '# this is another comment',
        '# third comment',
        'A\tB\tC\tD\tE',
        '1\t2\t3\t4\t5',
        '6\t7\t8\t9\t10',
        '11\t12\t13\t14\t15'
    ];

    this.element
        .datacontroller('write_lines', data_with_header)
        .datacontroller('done');

    equals(this.element.find('tr').length, 4, 'Exactly four rows printed ');
    ok(this.element.find('caption').length > 0, 'Caption found');
});

})(jQuery);
