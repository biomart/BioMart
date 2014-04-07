(function() {
module('validator');

var _item = {
    name: 'foo',
    displayName: 'Foo Bar'
};

test('Text filter validation', function() {
    expect(1);
    var item = $.extend({type: 'text'}, _item);
    var el =biomart.renderer.filter('div', item).appendTo($('#test'));
    el.find('.field').val('test');
    var value = biomart.validator.filter(el);
    equals(value, 'test', 'Text value should validate to user input');
    el.remove();
});

test('Boolean filter validation', function() {
    expect(1);
    var item = $.extend({
        type: 'boolean'
    }, _item);
    var el = biomart.renderer.filter('div', item).appendTo($('#test'));
    el.find('input[value=excluded]')[0].checked = true;
    var value = biomart.validator.filter(el);
    equal(value, 'excluded', 'Value should be excluded');
    el.remove();
});

test('Upload filter validation', function() {
    expect(1);
    var item = $.extend({
        type: 'upload'
    }, _item);
    var el = biomart.renderer.filter('div', item).appendTo($('#test'));
    el.find('textarea.field').val('Hello World');
    var value = biomart.validator.filter(el);
    equal(value, 'Hello World', 'Value should be "Hello World"');
    el.remove();
});

test('Single select filter validation', function() {
    expect(2);
    var item = $.extend({
        type: 'singleSelect',
        values: [
            {name: '', displayName: 'Invalid'},
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    }, _item);
    var el =biomart.renderer.filter('div', item).appendTo($('#test'));
    var value = biomart.validator.filter(el);
    equals(value, null, 'null value for invalid selection');
    el.find('select')[0].selectedIndex = 1;
    value = biomart.validator.filter(el);
    strictEqual(value, 'a', 'Validated value should be selected option');
    el.remove();
});

test('Single select upload filter validation', function() {
    expect(1);
    var item = $.extend({
        type: 'singleSelectUpload',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    }, _item);
    var el =biomart.renderer.filter('div', item).appendTo($('#test'));
    var value = biomart.validator.filter(el);
    el.find('textarea.field').val('hello world');
    value = biomart.validator.filter(el);
    deepEqual(value, ['a', 'hello world', 'A'], 'Validated value should be array of [name, value, displayName]');
    el.remove();
});

test('Single select boolean filter validation', function() {
    expect(1);
    var item = $.extend({
        type: 'singleSelectBoolean',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    }, _item);
    var el = biomart.renderer.filter('div', item).appendTo($('#test'));
    var value = biomart.validator.filter(el);
    deepEqual(value, ['a', 'only', 'Foo Bar A'], 'Value should always be "only" and displayName is combination of parent and select filter');
    el.remove();
});

test('Multi select filter validation', function() {
    expect(1);
    var item = $.extend({
        type: 'multiSelect',
        values: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    }, _item);
    var el = biomart.renderer.filter('div', item).appendTo($('#test'));
    el.find('option').each(function() {
        this.selected = true;
    });
    var value = biomart.validator.filter(el);
    equal(value, 'a,b,c', 'Comma-separated value list');
    el.remove();
});

test('Multi boolean select filter validation', function() {
    expect(1);
    var item = $.extend({
        type: 'multiSelectBoolean',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    }, _item);
    var el = biomart.renderer.filter('div', item).appendTo($('#test'));
    el.find('option').each(function() {
        this.selected = true;
    });
    var value = biomart.validator.filter(el);
    deepEqual(value, ['a,b,c', 'only', 'Foo Bar: A, B, C'], 'Array value returned');
    el.remove();
});

test('Multi upload select filter validation', function() {
    expect(1);
    var item = $.extend({
        type: 'multiSelectUpload',
        filters: [
            {name: 'a', displayName: 'A'},
            {name: 'b', displayName: 'B'},
            {name: 'c', displayName: 'C'}
        ]
    }, _item);
    var el = biomart.renderer.filter('div', item).appendTo($('#test'));
    el.find('option').each(function() {
        this.selected = true;
    });
    el.find('textarea.field').val('Hello World');
    var value = biomart.validator.filter(el);
    deepEqual(value, ['a,b,c', 'Hello World', 'Foo Bar: A, B, C'], 'Array value returned');
    el.remove();
});

})();
