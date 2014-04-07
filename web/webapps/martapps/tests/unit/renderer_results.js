(function($) {

var _test = $('#test');

module('renderer: results', {
    setup: function() {
        this.element = $('<div/>').appendTo(_test);
        this.header = ['Foo', 'Bar', 'Faz', 'Baz'];
        this.data = [
            ['a', 'b', 'c', 'd'],
            ['e', 'i', 'm', 'q'],
            ['f', 'j', 'n', 'r'],
            ['g', 'k', 'o', 's'],
            ['h', 'l', 'p', 't']
        ];
        this.chartData = [
            ['a', 2, 7, 4],
            ['b', 2, 3, 5],
            ['c', 2, 2, 4],
            ['d', 8, 3, 5],
            ['e', 2, 3, 1]
        ];
        this.heatData = [
            [.5, 'up', 7, 'a'],
            [.3, 'up', 3, 'b'],
            [.4, 'up', 2, 'c'],
            [-.5, 'down', 3, 'd'],
            [2, 'up', 3, 'e']
        ];
    },
    teardown: function() {
        this.renderer.destroy();
        ok(true, 'destroy');
        delete this.renderer;
        delete this.data;
        delete this.chartData;
        delete this.heatData;
        delete this.header;
    }
});

test('Plain', function() {
    var renderer = this.renderer = biomart.renderer.get('plain'),
        writee = renderer.getElement().appendTo(this.element),
        html;

    ok(renderer != null, 'Renderer returned OK');

    renderer.printHeader(this.header, writee);
    renderer.parse(this.data, writee);
    equals(writee[0].tagName, 'PRE', 'Element should be <pre>');

    html = writee[0].innerHTML;
    ok(new RegExp(this.data[2].join('\t')).test(html), 'Should have one of the result rows');
});

test('Table', function() {
    var renderer = this.renderer = biomart.renderer.get('table');
    renderer.option('highlight', 0);

    var writee = renderer.getElement().appendTo(this.element);

    ok(renderer != null, 'Renderer returned OK');

    renderer.printHeader(this.header, writee);
    renderer.parse(this.data, writee);
    equals(writee[0].tagName, 'TABLE', 'Element should be <table>');

    ok(writee.find('thead').length, '<thead> exists');
    ok(writee.find('tbody').length, '<tbody> exists');
    ok(writee.find('td').first().hasClass('highlight'), 'First column is highlighted');
    equals(writee.find('p').first().text(), 'Foo', 'First column should have text "Foo"');
    equals(writee.find('p').last().text(), 't', 'Last column should have text "t"');
});

test('List', function() {
    var renderer = this.renderer = biomart.renderer.get('list');
    renderer.option('breakAt', 2);
    var writee = renderer.getElement().appendTo(this.element);

    ok(renderer != null, 'Renderer returned OK');

    renderer.printHeader(this.header, writee);
    renderer.parse(this.data, writee);
    equals(writee[0].tagName, 'UL', 'Element should be <ul>');

    renderer.draw(writee);

    var first = writee.children().first(),
        items = first.find('li');
    equals(first.attr('pk'), 'a', 'pk on first row should be "a"');
    ok(items.first().hasClass('primary'), 'First list item is the primary value');
    ok(items.last().hasClass('last'), 'Last class name set');
    ok(items.filter('.col-1').hasClass('meta'), 'Second column considered meta info');
    ok(items.filter('.col-2').hasClass('break'), 'Break at third column (according to option)');
});

test('List - empty results', function() {
    var renderer = this.renderer = biomart.renderer.get('list');
    var writee = renderer.getElement().appendTo(this.element);

    renderer.printHeader(this.header, writee);

    renderer.draw(writee);

    ok(writee.find('.empty').length > 0, 'Empty results printed');
});

test('Chart', function() {
    var renderer = this.renderer = biomart.renderer.get('chart');
    var writee = renderer.getElement().appendTo(this.element).width(400);

    ok(renderer != null, 'Renderer returned OK');

    renderer.printHeader(this.header, writee);
    renderer.parse(this.chartData, writee);

    ok(true, 'Parsing data OK');

    renderer.draw(writee);

    ok(true, 'Drawing OK');
});

test('Histogram', function() {
    var renderer = this.renderer = biomart.renderer.get('histogram');
    var writee = renderer.getElement().appendTo(this.element).width(400);

    ok(renderer != null, 'Renderer returned OK');

    renderer.printHeader(this.header, writee);
    renderer.parse(this.chartData, writee);

    ok(true, 'Parsing data OK');

    renderer.draw(writee);

    ok(true, 'Drawing OK');
});

test('Histogram - empty results', function() {
    var renderer = this.renderer = biomart.renderer.get('histogram');
    var writee = renderer.getElement().appendTo(this.element);

    renderer.printHeader(this.header, writee);

    renderer.draw(writee);

    ok(writee.find('.empty').length > 0, 'Empty results printed');
});

test('Heatmap', function() {
    var renderer = this.renderer = biomart.renderer.get('heatmap');
    var writee = renderer.getElement().appendTo(this.element);

    ok(renderer != null, 'Renderer returned OK');

    renderer.printHeader(this.header, writee);
    renderer.parse(this.heatData, writee);

    ok(true, 'Parsing data OK');

    renderer.draw(writee);

    ok(true, 'Drawing OK');
});

test('Heatmap - empty results', function() {
    var renderer = this.renderer = biomart.renderer.get('heatmap');
    var writee = renderer.getElement().appendTo(this.element);

    renderer.printHeader(this.header, writee);

    renderer.draw(writee);

    ok(writee.find('.empty').length > 0, 'Empty results printed');
});

})(jQuery);
