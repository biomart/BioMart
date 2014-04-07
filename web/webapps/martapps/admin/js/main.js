(function($) {

$.namespace('biomart.admin', function(self) {
    var OAUTH_REQUEST_TOKEN_URL = '/oauth/token',
        OAUTH_AUTHORIZE_URL = '/oauth/authorize',
        OAUTH_ACCESS_TOKEN_URL = '/oauth/access',
        OAUTH_SIGNATURE_METHOD = 'PLAINTEXT',
        _elements = {}, 
        _state = {},
        _init = {},
        _utils = {};

    self.init = function() {
        _init.elements();
        _init.oauth();
    };

    self.handleVerifier = function(pinCode) {
        _elements.oauthForm
            .data('oauth_verifier', pinCode)
            .find('strong.pin').text(pinCode);
    };

    _init.oauth = function() {
       $.ajax({
            url: '/oauth_admin/accessor',
            complete: function(xhr) {
                if (xhr.getResponseHeader) {
                    OAuth.correctTimestamp(~~(new Date(xhr.getResponseHeader('Date')).getTime() / 1000));
                }
            },
            success: _utils.loadAccessor
        });
    };

    _init.elements = function() {
        _elements.content = $('#biomart-content');

        _elements.oauth = $('#oauth-consumers')
            .delegate('.ui-icon-close', 'click', function() { _utils.removeConsumer($(this).parent()) });

        _elements.oauthConsumerForm = _elements.oauth.find('form.oauth-new-form')
            .dialog({
                draggable: false,
                resizable: false,
                width: 500,
                height: 350,
                modal: true,
                autoOpen: false,
                open: function() { _elements.oauthNameInput.focus() },
                close: function() {
                    _elements.oauthForm.empty();
                    _elements.oauthNameInput.val('');
                    _elements.oauthDescriptionInput.val('');
                    _elements.oauthCallbackInput.val('');
                    _elements.oauthKeyInput.val('');
                    _elements.oauthSecretInput.val('');
                }
            })
            .bind('submit',  function() {
                _utils.addOAuthConsumer();
                _elements.oauthConsumerForm.dialog('close');
                return false;
            })
            .delegate('.cancel', 'click', function() { _elements.oauthConsumerForm.dialog('close') });
        _elements.oauthNameInput = $('#oauth-new-name');
        _elements.oauthDescriptionInput = $('#oauth-new-description');
        _elements.oauthCallbackInput = $('#oauth-new-callback');
        _elements.oauthKeyInput = $('#oauth-new-key');
        _elements.oauthSecretInput = $('#oauth-new-secret');
        _elements.oauth.find('button.add') .bind('click.oauth', function() {
            _elements.oauthKeyInput.val(hex_md5(''+(new Date().getTime())));
            _elements.oauthSecretInput.val(hex_sha1(''+(new Date().getTime() * Math.random() * 1000000)));
            _elements.oauthConsumerForm.dialog('open');
        });

        _elements.oauth
            .delegate('button.authorize', 'click', function() {
                var item = $(this).closest('li'),
                    key = item.find('dd.key').text(),
                    secret = item.find('dd.secret').text();
                _state.key = key;
                _state.secret = secret;
                _elements.oauthForm.dialog('open');
            });

        _elements.oauthForm = _elements.oauth.children('form.oauth-authorization')
            .dialog({
                autoOpen: false,
                resizable: false,
                draggable: false,
                dialogClass: 'oauth',
                modal: true,
                width: 600,
                height: 160,
                open: _utils.startAuthorization
            })
            .bind('submit', function() {
                $(this)
                    .empty()
                    .append(['<p>Authorization successful! Pin code = <strong class="pin"/>.</p>'].join(''))
                    .append([
                        '<button class="box continue">Add access token</button>',
                        '<span class="cancel">cancel</span>'
                    ].join(''));
            })
            .delegate('.close,.cancel', 'click', function() {
                _elements.oauthForm.dialog('close');
                return false;
            })
            .delegate('.continue', 'click', _utils.continueAuthorization);

        _elements.oauthDeleteForm = _elements.oauth.children('form.oauth-consumer-delete')
            .dialog({
                autoOpen: false,
                resizable: false,
                draggable: false,
                dialogClass: 'oauth',
                modal: true,
                width: 500,
                height: 120
            })
            .delegate('.delete', 'click', function() {
                var consumerKey = _elements.oauthDeleteForm.data('consumerKey');

                $.ajax({
                    url: ['/oauth_admin/consumer/delete/', consumerKey].join(''),
                    type: 'POST',
                    complete: function() { _elements.oauthDeleteForm.dialog('close') },
                    success: function(json) {
                        _init.oauth();
                    }, 
                    error: function(xhr) {
                    }
                });

                return false;
            })
            .delegate('.cancel', 'click', function() { _elements.oauthDeleteForm.dialog('close') });
    };

    _utils.loadAccessor = function(json) {
        _state.accessors = json;
        _state.accessorMap = {};

        for (var i=0, item; item=json[i]; i++) {
            if (!_state.accessorMap[item.oauthAccessor.consumer.consumerKey]) {
                _state.accessorMap[item.oauthAccessor.consumer.consumerKey] = [];
            }
            _state.accessorMap[item.oauthAccessor.consumer.consumerKey].push(item);
        }

        var list = _elements.oauth.children('ul').html('<li style="border:none"><span class="loading"></span></li>');

        $.ajax({
            url: '/oauth_admin/consumer',
            success: function(json) {
                list.empty();

                if (!json.length) {
                    list.append('<li class="empty">None</li>');
                    return;
                }

                for (var i=0, consumer, element, accessors; consumer=json[i]; i++) {
                    accessors = _state.accessorMap[consumer.key];
                    element = $([
                        '<li class="clearfix">',
                            '<h3>', consumer.name, '</h3>',
                            '<span class="ui-icon ui-icon-close" title="Remove"/>',
                            '<dl class="properties clearfix">',
                                '<dt>Description: </dt>',
                                '<dd class="description">', consumer.description || '--', '</span>',
                                '<dt>Key: </dt>',
                                '<dd class="key">', consumer.key, '</span>',
                                '<dt>Secret: </dt>',
                                '<dd class="secret">', consumer.secret, '</span>',
                            '</dl>',
                        '</li>'
                    ].join('')).appendTo(list);

                    if (accessors) {
                        for (var j=0, accessor; accessor=accessors[j]; j++) {
                            $([
                                '<dl class="clearfix accessor">',
                                    '<dt>OpenID: </dt>',
                                    '<dd class="openid">', accessor.openId, '</dd>',
                                    '<dt>Access token: </dt>',
                                    '<dd class="access-token">', accessor.oauthAccessor.accessToken, '</dd>',
                                    '<dt>Access token secret: </dt>',
                                    '<dd class="access-token-secret">', accessor.oauthAccessor.tokenSecret, '</dd>',
                                '</dl>',
                            ].join('')).appendTo(element);;
                        }
                    }

                    $([
                        '<button class="box authorize">Add access token for user</button>',
                    ].join('')).appendTo(element);;
                }
            }
        });
    };

    _utils.addOAuthConsumer = function() {
        var key = _elements.oauthKeyInput.val(),
            secret = _elements.oauthSecretInput.val(),
            name = _elements.oauthNameInput.val(),
            description = _elements.oauthDescriptionInput.val(),
            callback = _elements.oauthCallbackInput.val();

        $.ajax({
            url: '/oauth_admin/consumer/add/' + key,
            type: 'POST',
            data: {
                name: name,
                secret: secret,
                description: description,
                callback: callback
            },
            success: function(json) {
                _init.oauth();
            },
            error: function() {
            }
        });
    };

    _utils.startAuthorization = function() {
        var $this = $(this),
            url = OAUTH_REQUEST_TOKEN_URL,
            accessor = {
                consumerKey: _state.key,
                consumerSecret: _state.secret
            },
            message = {
                action: url,
                method: 'GET',
                parameters: {
                    oauth_signature_method: OAUTH_SIGNATURE_METHOD,
                    oauth_consumer_key: _state.key,
                    oauth_callback: '/oauth/continue.jsp'
                }
            };

        $this.html('<p>Negotiating with server...</p>');

        OAuth.completeRequest(message, accessor);

        $.ajax({
            url: url,
            type: 'GET',
            data: message.parameters,
            success: function(data) {
                var params = $.extend({
                        oauth_signature_method: OAUTH_SIGNATURE_METHOD,
                        oauth_consumer_key: _state.key,
                        oauth_callback: '/oauth/continue.jsp'
                    }, $.deparam(data)),

                    url = OAUTH_AUTHORIZE_URL,

                    message = {
                        action: url,
                        method: 'GET',
                        parameters: params
                    };

                OAuth.completeRequest(message, accessor);

                url += '?' + $.param(message.parameters);

                _elements.oauthForm
                    .data('oauth_token', params.oauth_token)
                    .data('oauth_token_secret', params.oauth_token_secret)
                    .data('accessor', accessor)
                    .attr('action', url).empty();

                for (var k in message.parameters) {
                    $(['<input type="hidden" name="', k, '" value="', message.parameters[k], '"/>'].join(''))
                        .appendTo(_elements.oauthForm);
                }

                _elements.oauthForm.append([
                    '<p>User authorization needed.</p>',
                    '<p>Return to this window once authorization has completed</p>'
                    ].join(''));
                _elements.oauthForm.append([
                    '<button class="box go">Continue to authorization</button>',
                    '<span class="cancel">cancel</span>'
                ].join(''));
            },
            error: function(xhr) {
                $this.html([
                    '<p class="error">Error encountered during server negotiation</p>',
                    '<p>', xhr.responseText, '</p>',
                    '<p><button class="box close">OK</button></p>'
                ].join(''));
            }
        });
    };

    _utils.continueAuthorization = function() {
        var $this = $(this),
            url = OAUTH_ACCESS_TOKEN_URL,
            form = _elements.oauthForm,
            oauth_token = form.data('oauth_token'),
            oauth_token_secret = form.data('oauth_token_secret'),
            oauth_verifier = form.data('oauth_verifier'),
            accessor = {
                consumerKey: _state.key,
                consumerSecret: _state.secret,
                token: oauth_token,
                tokenSecret: oauth_token_secret
            },
            message = {
                action: url,
                method: 'GET',
                parameters: {
                    oauth_verifier: oauth_verifier,
                    oauth_signature_method: OAUTH_SIGNATURE_METHOD,
                    oauth_consumer_key: _state.key
                }
            };

        form.html('<p>Negotiating with server...</p>');

        OAuth.completeRequest(message, accessor);

        $.ajax({
            url: url,
            type: 'GET',
            data: message.parameters,
            success: function(data) {
                var params = $.deparam(data);
                form.html([
                    '<p>Successfully retrieved access token. The client will need all of the following: </p>',
                    '<dl class="clearfix">',
                        '<dt>Consumer key: </dt>',
                        '<dd>', _state.key, '</dd>',
                        '<dt>Consumer secret: </dt>',
                        '<dd>', _state.secret, '</dd>',
                        '<dt>Access token: </dt>',
                        '<dd>', params.oauth_token, '</dd>',
                        '<dt>Access token secret: </dt>',
                        '<dd>', params.oauth_token_secret, '</dd>',
                    '</dl>',
                    '<button class="box close">Close</button>'
                ].join(''));
                _init.oauth();
            },
            error: function(xhr) {
                form.html([
                    '<p class="error">Error encountered during server negotiation</p>',
                    '<p>', xhr.responseText, '</p>',
                    '<p><button class="box close">OK</button></p>'
                ].join(''));
            }
        });

        return false;
    };

    _utils.removeConsumer = function(element) {
        var list = element.children('dl.properties'),
            name = list.children('dd.name').text(),
            consumerKey = list.children('dd.key').text();

        _elements.oauthDeleteForm
            .data('consumerKey', consumerKey)
            .find('em.name').text(name)
            .end()
            .dialog('open');
    };
});

})(jQuery);
