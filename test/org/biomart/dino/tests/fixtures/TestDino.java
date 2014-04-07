package org.biomart.dino.tests.fixtures;

import java.io.OutputStream;

import org.biomart.dino.Binding;
import org.biomart.dino.annotations.Func;
import org.biomart.dino.dinos.Dino;
import org.biomart.queryEngine.Query;

public class TestDino implements Dino {

	@Func(id = "first")
	String f1;
	@Func(id = "second", optional = true)
	String f2;
	String foo;
	double bar;
		   
	@Override
	public Dino setQuery(Query query) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public Dino setMimes(String[] mimes) {
		// TODO Auto-generated method stub
		return this;
		
	}

	@Override
	public void run(OutputStream out) {
		// TODO Auto-generated method stub
		
	}

	public String getF1() {
		return f1;
	}

	public String getF2() {
		return f2;
	}

	public String getFoo() {
		return foo;
	}

	public double getBar() {
		return bar;
	}

	@Override
	public Dino setMetaData(Binding metaData) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public Query getQuery() {
        // TODO Auto-generated method stub
        return null;
    }



}
