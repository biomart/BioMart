package org.biomart.dino.tests.querybuilder;

import org.biomart.dino.querybuilder.JavaQueryBuilder;

import com.google.inject.AbstractModule;

public class QueryBuilderTestModule extends AbstractModule {

	@Override
	protected void configure() {
		requestStaticInjection(JavaQueryBuilder.class);
		
	}

}
