(function($) {

var _test = $('#test');

module('ui.common');

test('Minimizer', function() {
    var header = $('<h3>Foo<span class="ui-icon ui-icon-triangle-1-s"/></h3>')
        .appendTo(_test);

    var div = $('<div>test</div>').appendTo(_test);

    header.minimizer({duration: 15});
    
    header
        .bind('hide', function() {
            ok(true, 'DIV should hide after first header click');
            header.trigger('click');
        })
        .bind('show', function() {
            ok(true, 'DIV should show after second header click');
            header.minimizer('destroy');
            _test.empty();
            start();
        });

    stop();

    header.trigger('click');
});

test('Pretty box', function() {
    var select = $('<select/>').appendTo(_test);

    $('<option value="foo">Foo</option>')
        .data('item', {name: 'foo', displayName: 'Foo', type: 'text'})
        .appendTo(select);

    $('<option value="bar">Bar</option>')
        .data('item', {name: 'bar', displayName: 'Bar', type: 'text'})
        .appendTo(select);

    $('<option value="faz">Faz</option>')
        .data('item', {name: 'faz', displayName: 'Faz', type: 'text'})
        .appendTo(select);

    $('<option value="baz">Baz</option>')
        .data('item', {name: 'baz', displayName: 'Baz', type: 'text'})
        .appendTo(select);

    select.prettybox();

    var div = select.siblings('div.ui-prettybox');
    var selected_items = select.siblings('ul.selected-items');

    ok(div.length == 1, 'New holder DIV for pretty box elements is created');

    var ac_input = div.find('input.ui-autocomplete-input');
    ok(ac_input.length == 1, 'Autocomplete input box created');

    ok(selected_items.length == 1, 'List of selected items added')

    ac_input.focus();

    var ac_menu = $('.ui-menu.ui-autocomplete:visible');
    ok(ac_menu.length == 1, 'Autocomplete menu shows on focus');

    stop();

    ac_input.focus().val('Faz').keydown();

    setTimeout(function() {
        ac_input
            .simulate('keydown', { keyCode: $.ui.keyCode.DOWN })
            .simulate('keydown', {keyCode: $.ui.keyCode.ENTER});
        //equals(select.val(), 'faz', 'New value should be selected');
        select.prettybox('destroy');
        _test.empty();
        start();
    }, 100);
});

test('Simpler filter widget: base checks', function() {
    var item = {
            name: 'foo',
            displayName: 'Faz Baz',
            type: 'text'
        },
        el = biomart.renderer.filter('div', item)
            .appendTo(_test)
            .simplerfilter();
    ok(el.hasClass('simplerfilter'), 'Class name OK');
    el.simplerfilter('destroy');
    ok(!el.hasClass('simplerfilter'), 'Class name "simplerfilter" removed on destroy');
    _test.empty();
});

test('Simpler filter widget: add and remove events', function() {
    var item = {
            name: 'foo',
            displayName: 'Faz Baz',
            type: 'text'
        },
        el = biomart.renderer.filter('div', item)
            .appendTo(_test)
            .simplerfilter(),
        input = el.find('input.field'),
        _value = 'bar';

    el.bind('addfilter', function(ev, item, value) {
        ok(true, 'Filter change event triggered');
        equals(value, _value, 'Value should be returned');
        el.find('.ui-icon-circle-close').simulate('click');
    });

    el.bind('removefilter', function(ev, item) {
        ok(true, 'Filter remove event triggered');
        el.simplerfilter('destroy');
        _test.empty();
        start();
    });

    stop();

    input.val(_value).trigger('change');
});


test('Simpler filter widget: default text', function() {
    var item = {
            name: 'foo',
            displayName: 'Faz Baz',
            type: 'singleSelect',
            values: [
                {name: 'a', displayName: 'A'},
                {name: 'b', displayName: 'B'},
                {name: 'c', displayName: 'C'}
            ]
        },
        el = biomart.renderer.filter('div', item)
            .appendTo(_test)
            .simplerfilter();

    ok(el.find('option[value=""]').length, 'Default "Choose" text exists');
});

test('Simpler filter widget: no default text', function() {
    var item = {
            name: 'foo',
            displayName: 'Faz Baz',
            type: 'singleSelect',
            values: [
                {name: 'a', displayName: 'A'},
                {name: 'b', displayName: 'B'},
                {name: 'c', displayName: 'C'}
            ]
        },
        el = biomart.renderer.filter('div', item)
            .appendTo(_test)
            .simplerfilter({chooseText: false});

    ok(el.find('option[value=""]').length === 0, 'Default "Choose" text does not exist');
});

test('Simpler attribute widget: base checks', function() {
    var item = {
            name: 'foo',
            displayName: 'Foo Bar'
        },
        el = biomart.renderer.attribute('div', item)
            .appendTo(_test)
            .simplerattribute();
    ok(el.hasClass('simplerattribute'), 'Class name OK');
    el.simplerattribute('destroy');
    ok(!el.hasClass('simplerattribute'), 'Class name "simplerattribute" removed on destroy');
    _test.empty();
});

test('Simpler attribute widget: add and remove events', function() {
    var item = {
            name: 'foo',
            displayName: 'Foo Bar'
        },
        el = biomart.renderer.attribute('div', item)
            .appendTo(_test)
            .simplerattribute({radio: false}),
        checkbox = el.find('input[type=checkbox]');

    el.bind('addattribute', function() {
        ok(true, 'Add attribute event triggered');
        checkbox.attr('checked', false).click();
    });
    
    el.bind('removeattribute', function() {
        ok(true, 'Remove attribute event triggered');
        el.simplerattribute('destroy');
        _test.empty();
        start();
    });

    stop();

    checkbox.attr('checked', true).click();
});

})(jQuery);
