export PATH := node_modules/.bin:$(PATH)

bin				:=	./node_modules/.bin
mvenrichment	:=	build/mvenrichment.min.js
deps			:=	build/libs.min.js
src_dir			:=	app/js
ng_app			:=	$(src_dir)/app.js
ng_services		:=	$(src_dir)/services.js $(wildcard $(src_dir)/services/*.js)
ng_directives	:=	$(src_dir)/directives.js $(wildcard $(src_dir)/directives/*.js)
ng_controllers	:=	$(src_dir)/controllers.js
libraries		:=	$(wildcard app/lib/*.min.js)

.PHONY: all clean dev prod

$(mvenrichment): $(ng_app) $(ng_services) $(ng_controllers) $(ng_directives)
	mkdir -p $(dir $@)
	$(bin)/uglifyjs $^ -cmo $@

$(deps): $(libraries)
	mkdir -p $(dir $@)
	cat $^ > $@

dev:
	scripts/compile-index-template.sh development

prod:
	scripts/compile-index-template.sh production


all: $(deps) prod $(mvenrichment)

clean:
	rm -rf build && rm -rf app/index.html