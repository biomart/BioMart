(function($) {
$.namespace('biomart.auth', function(self) {
    var div,
        info,
        dialog,
        link,
        form,
        loading,
        initialized = false,
        actions = {
            sign_in: null,
            sign_out: null
        };

    self.postForm = null;

    self.callback = function(json) {
        self.setUser(json);
        dialog.dialog('close');
        $.publish('biomart.restart');
    };

    self.setUser = function(attributes) {
        info.find('span.identifier').text(attributes.identifier);
        info.find('span.email').text(attributes.email);
        div.addClass('logged-in');
        $('#biomart-user').html(_('logged in as') + ': <a href="#" class="profile" title="View profile">' + (attributes.email || attributes.identifier) + '</a>');
        actions.sign_in.hide();
        actions.sign_out.show();
    };

    self.setAnonymous = function() {
        $('#biomart-user').text(_('not logged in'));
        actions.sign_in.show();
        actions.sign_out.hide();
    };

    self.displayLoginForm = function() {
        if (!initialized) self.setupOpenId();
        dialog.dialog('open');
    };

    self.setupOpenId = function() {
        var offset,
            width = 600,
            height = 400,
            divWidth;

        divWidth = div.width();

        initialized = true;

        dialog = $('#biomart-login-dialog').dialog({
            draggable: false,
            resizable: false,
            modal: true,
            autoOpen: false,
            width: width,
            height: height,
            dialogClass: 'login',
            open: function() {
                $('#openid_username').focus();
            },
            close: function() {
                // reset everything
                dialog.find('.error').addClass('invisible');
                self.postForm.empty();
                loading.hide();
            },
            buttons: {
                "Cancel": function() {
                    $(this).dialog('close');
                }
            }
        });

        loading = dialog.find('div.loading');

        form = $('#biomart-login-form')
            .openid({
                img_path: '/css/openid/',
                txt: {
                    label: 'Enter your {username} for <b>{provider}</b>',
                    title: 'Select a provider.',
                    sign: 'Go'
                }
            })
            .submit(function() {
                loading.show();
                self.postForm.load(BIOMART_CONFIG.service.url + 'user/auth?' + $(this).serialize(),
                    function(responseText, textStatus, xhr) {
                        if (textStatus == 'error') {
                            form.find('input[name=url]').remove();
                            dialog.find('.error').removeClass('invisible').text(responseText);
                            loading.hide();
                        }
                    });
                return false;
            });

        self.postForm = $('#biomart-openid-post-form');
    };

    self.init = function() {
        div = $('#biomart-login');
        actions.sign_in = div.find('a.sign-in');
        actions.sign_out = div.find('a.sign-out');
        info = $('#biomart-auth-info');

        actions.sign_in.bind('click.auth', function() {
            biomart.auth.displayLoginForm();
            return false;
        });

        div.bind('click.auth', function(ev) {
            if ($(ev.target).hasClass('profile')) {
                info.slideToggle();
                return false;
            } 
        });
            
        info.bind('click.auth', function(ev) {
            if ($(ev.target).hasClass('ui-icon-close')) {
                info.slideUp();
            }
        });

        $.publish('biomart.init');
    };
});

$.subscribe('biomart.login', biomart.auth, 'init');
})(jQuery);
