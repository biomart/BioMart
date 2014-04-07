module('renderer: filter');

test('Text filter', function() {
    expect(3);
    var item = {
        name: 'foo',
        displayName: 'Faz Baz',
        type: 'text'
    };
    var el = biomart.renderer.filter('div', item);
    ok(el.hasClass('text'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Faz Baz', 'Correct label name');
    ok(el.find('.field.text').length > 0, 'Text field exists');
});

test('Upload filter', function() {
    expect(4);
    var item = {
        name: 'foo',
        displayName: 'Faz Baz',
        type: 'upload'
    };
    var el = biomart.renderer.filter('div', item);
    ok(el.hasClass('upload'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Faz Baz', 'Correct label name');
    ok(el.find('textarea.field').length > 0, 'Value field exists');
    ok(el.find('input[type=file]').length > 0, 'Upload field exists');
});

test('Boolean filter', function() {
    expect(4);
    var item = {
        name: 'foo',
        displayName: 'Foo Bar',
        type: 'boolean'
    };
    var el = biomart.renderer.filter('div', item);
    ok(el.hasClass('boolean'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Foo Bar', 'Correct label name');
    ok(el.find('input[value=excluded]').length > 0, 'Excluded field exists');
    ok(el.find('input[value=only]').length > 0, 'Only field exists');
});

test('Single select filter', function() {
    expect(5);
    var item = {
        name: 'foo',
        displayName: 'Foo Bar',
        type: 'singleSelect',
        values: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    };
    var el = biomart.renderer.filter('div', item),
        select = el.find('select.field');
    ok(el.hasClass('singleSelect'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Foo Bar', 'Correct label name');
    ok(select.length > 0, 'Select field exists');
    ok(el.find('option[value=c]').length > 0, 'Option c exists');
    ok(!select[0].multiple, 'Select box has multiple=false');
});

test('Single select boolean filter', function() {
    expect(4);
    var item = {
        name: 'foo',
        displayName: 'Faz Baz',
        type: 'singleSelectBoolean',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    };
    var el = biomart.renderer.filter('div', item),
        select = el.find('select.filter-list');
    ok(el.hasClass('singleSelectBoolean'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Faz Baz', 'Correct label name');
    ok(select.length > 0, 'Filter list exists');
    ok(!select[0].multiple, 'Select box has multiple=false');
});

test('Single select upload filter', function() {
    expect(6);
    var item = {
        name: 'foo',
        displayName: 'Foo Bar',
        type: 'singleSelectUpload',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    };
    var el = biomart.renderer.filter('div', item),
        select = el.find('select.filter-list');
    ok(el.hasClass('singleSelectUpload'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Foo Bar', 'Correct label name');
    ok(select.length > 0, 'Filter list exists');
    ok(el.find('textarea.field').length > 0, 'Value field exists');
    ok(el.find('input[type=file]').length > 0, 'Upload field exists');
    ok(!select[0].multiple, 'Select box has multiple=false');
});

test('Multi select filter', function() {
    expect(4);
    var item = {
        name: 'foo',
        displayName: 'Faz Baz',
        type: 'multiSelect',
        values: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    };
    var el = biomart.renderer.filter('div', item),
        select = el.find('select.field');
    ok(el.hasClass('multiSelect'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Faz Baz', 'Correct label name');
    ok(select.length > 0, 'Multi-select list exists');
    ok(select[0].multiple, 'Select box has multiple=true');
});

test('Multi select boolean filter', function() {
    expect(4);
    var item = {
        name: 'foo',
        displayName: 'Faz Baz',
        type: 'multiSelectBoolean',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    };
    var el = biomart.renderer.filter('div', item),
        select = el.find('select.filter-list');
    ok(el.hasClass('multiSelectBoolean'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Faz Baz', 'Correct label name');
    ok(select.length > 0, 'Filter list exists');
    ok(select[0].multiple, 'Select box has multiple=true');
});

test('Multi select upload filter', function() {
    expect(6);
    var item = {
        name: 'foo',
        displayName: 'Foo Bar',
        type: 'multiSelectUpload',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    };
    var el = biomart.renderer.filter('div', item),
        select = el.find('select.filter-list');
    ok(el.hasClass('multiSelectUpload'), 'Correct class name');
    equals(el.find('.item-name').text(), 'Foo Bar', 'Correct label name');
    ok(el.find('select.filter-list').length > 0, 'Filter list exists');
    ok(el.find('textarea.field').length > 0, 'Value field exists');
    ok(el.find('input[type=file]').length > 0, 'Upload field exists');
    ok(select[0].multiple, 'Select box has multiple=true');
});


module('renderer: attribute');

test('Attribute', function() {
    expect(2);
    var item = {
        name: 'foo',
        displayName: 'Foo Bar'
    };
    var el = biomart.renderer.attribute('div', item);
    ok(el.find('input.checkbox').length > 0, 'Checkbox exists');
    ok(el.find('label.item-name').length > 0, 'Label exists');
});


module('renderer: container');

test('Container rendering modes sanity check', function() {
    expect(4);
    // Make sure these are never changed
    equals(biomart.renderer.NONE, 0, 'None');
    equals(biomart.renderer.FILTERS, 1, 'Filters');
    equals(biomart.renderer.ATTRIBUTES, 2, 'Attributes');
    equals(biomart.renderer.BOTH, 3, 'Both');
});

test('One container rendering', function() {
    expect(2);
    var item = {
        name: 'foo',
        displayName: 'Foo Bar',
        isLeaf: true,
        attributes: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ],
        filters: [
            {name: 'x', displayName: 'X', type: 'text'},
            {name: 'y', displayName: 'Y', type: 'text'}
        ]
    };

    var el = biomart.renderer.container({
        item: item,
        mode: biomart.renderer.BOTH,
        id: 'test'
    });
    ok(el.find('ul.items.attributes').length > 0, 'Attribute list exists');
    ok(el.find('ul.items.filters').length > 0, 'Filter list exists');
});
