package org.biomart.dino.dinos;

import java.io.OutputStream;

import org.biomart.dino.Binding;
import org.biomart.queryEngine.Query;

public interface Dino {

	public void run(OutputStream out) throws Exception;
	public Dino setQuery(Query query);
	public Query getQuery();
	public Dino setMetaData(Binding metaData);
	public Dino setMimes(String[] mimes);
}
