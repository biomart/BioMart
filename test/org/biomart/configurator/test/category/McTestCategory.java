package org.biomart.configurator.test.category;

import org.biomart.configurator.test.SettingsForTest;

public abstract class McTestCategory {
	protected String testName;
		
	public static McTestCategory getCategory(String name) {
		System.out.println("running test "+name);
		String category = SettingsForTest.getTestCategory(name);
		String base = "org.biomart.configurator.test.category";
		try {
			Class<?> cl = Class.forName(base+"."+category);
			McTestCategory testCategory =  (McTestCategory)cl.newInstance();	
			testCategory.testName = name;
			return testCategory;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public abstract boolean test();
}