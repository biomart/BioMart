package org.biomart.dino.tests.fixtures;

import org.biomart.dino.annotations.Func;

public class MetaData {

	@Func(id = "first")
	String first;
	@Func(id = "second")
	String second;
	@Func(id = "other")
	String other;
}
