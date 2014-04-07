var consumer = {};

consumer.example =
{ consumerKey   : "0e60157a903b15d9c6b96f7aaab0680b"
, consumerSecret: "fa3264313cd6efe5a3a6e4fff6ced55bba4aa567"
, serviceProvider:
  { signatureMethod     : "PLAINTEXT"
  , requestTokenURL     : "https://localhost:9043/oauth/token"
  , userAuthorizationURL: "https://localhost:9043/oauth/authorize"
  , accessTokenURL      : "https://localhost:9043/oauth/access"
  , echoURL             : "https://localhost:9043/oauth/echo"
  }
};

consumer.initializeForm =
function initializeForm(form, etc, usage) {
    var selector = etc.elements[0];
    var selection = selector.options[selector.selectedIndex].value;
    var selected = consumer[selection];
    if (selected != null) {
        consumer.setInputs(etc, { URL           : selected.serviceProvider[usage + "URL"]
                                , consumerSecret: selected.consumerSecret
                                , tokenSecret   : selected[usage + "Secret"]
                                });
        consumer.setInputs(form, { oauth_signature_method: selected.serviceProvider.signatureMethod
                                 , oauth_consumer_key    : selected.consumerKey
                                 , oauth_token           : selected[usage]
                                 });
    }
    return true;
};

consumer.setInputs =
function setInputs(form, props) {
    for (p in props) {
        if (form[p] != null && props[p] != null) {
            form[p].value = props[p];
        }
    }
}

consumer.signForm =
function signForm(form, etc) {
    form.action = etc.URL.value;
    var accessor = { consumerSecret: etc.consumerSecret.value
                   , tokenSecret   : etc.tokenSecret.value};
    var message = { action: form.action
                  , method: form.method
                  , parameters: []
                  };
    for (var e = 0; e < form.elements.length; ++e) {
        var input = form.elements[e];
        if (input.name != null && input.name != "" && input.value != null
            && (!(input.type == "checkbox" || input.type == "radio") || input.checked))
        {
            message.parameters.push([input.name, input.value]);
        }
    }
    OAuth.setTimestampAndNonce(message);
    OAuth.SignatureMethod.sign(message, accessor);
    //alert(outline("message", message));
    var parameterMap = OAuth.getParameterMap(message.parameters);
    for (var p in parameterMap) {
        if (p.substring(0, 6) == "oauth_"
         && form[p] != null && form[p].name != null && form[p].name != "")
        {
            form[p].value = parameterMap[p];
        }
    }
    return true;
};
