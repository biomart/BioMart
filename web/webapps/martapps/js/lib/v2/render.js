// Based on ICanHaz (http://github.com/andyet/ICanHaz.js/blob/master/ICanHaz.js)
// This version supports Mustache and Handlebars
// By M@ McCray
window.render = (function($, engine){
  var cache   = {},
      methods = {};
  
  $(function(){
    $('script[type="text/html"]').each(function () {
        var name    = $(this).attr('id'),
            content = $(this).html();
        if(cache.hasOwnProperty(name)) {
          throw "You've already got a template by the name: \"" + name + "\"";
        }
        cache[name] = engine.compile(content);
        // build the corresponding public retrieval function
        methods[name] = function(data, raw) {
          data = data || {},
          html = engine.render(cache[name], data);
          return (raw) ? html : $(html);
        };
        // remove the element from the dom
        $(this).remove();
    });
  });
  
  return methods;

})(jQuery, (function(){
  if(window.Mustache) {
    return {
      compile: function(s){ return s; },
      render: function(content, data) { return Mustache.to_html(content, data); }
    };
  } else if(window.Handlebars) {
    return {
      compile: function(s) { return Handlebars.compile(s); },
      render: function(template, data) { return template(data); }
    };
  } else {
    throw "You must include Mustache or Handlebars on the page before this script!";
  };
})());
