(function($) {

var _test = $('#test');

module('auth', {
    setup: function() {
    this.element = $([
            '<li id="biomart-login"> ',
              '<span id="biomart-user">',
                'Not logged in',
              '</span> (',
              '<a class="action sign-in" href="#">Login</a>',
              '<a class="action sign-out" href="/rest/user/logout" style="display: none">Logout</a>',
              ')',
              '<div id="biomart-auth-info" class="gradient-grey-reverse" style="display: none">',
                '<span class="ui-icon ui-icon-close" title="Close"></span>',
                '<p>Unique identifier: <span class="identifier">/span></p>',
                '<p>Email: <span class="email"></span></p>',
              '</div>',
            '</li>'
        ].join('')).appendTo(_test),
    this.dialog = $([
        '<div id="biomart-login-dialog" style="display: none" title="Sign In">',
          '<p class="info login-info">Sign in using your OpenID account.</p>',
             '<form id="biomart-login-form" action="/martservice/user/auth" method="GET"></form>',
             '<p class="help login-help">',
                '<a target="_blank" href="http://openid.net/">Learn more about OpenID</a>',
                '<span class="ui-icon ui-icon-help"></span> ',
             '</p>',
             '<div id="biomart-openid-post-form" style="visibility: hidden"></div>',
             '<div class="loading" style="display: none"></div>',
        '</div>',
        ].join('')).appendTo(_test);
    },

    teardown: function() {
        this.dialog.dialog('destroy').remove();
        this.element.remove();
    }
});

test('Initialization', function() {
    expect(2);
    var self = this,
        testObj = {
        fn: function() {
            ok(true, 'Initialized signal triggered');
            self.element.find('.sign-in').trigger('click');
            biomart.auth.displayLoginForm();
            ok(self.dialog.hasClass('ui-dialog-content'), 'Log in dialog created');
            $.unsubscribeAll('biomart.init');
        }
    };
    $.subscribe('biomart.init', testObj, 'fn');
    biomart.auth.init();
});

})(jQuery);
