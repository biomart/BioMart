package org.biomart.dino.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.biomart.dino.tests.querybuilder.JavaQueryBuilderTest;

@RunWith(Suite.class)
@SuiteClasses({
    // EnrichmentDinoTest.class,
    DinoHandlerTest.class, BindingTest.class,
    JavaQueryBuilderTest.class, UtilsTest.class, HypgCommandTest.class })
public class DinoTestSuite {

}
