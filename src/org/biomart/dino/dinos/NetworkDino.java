package org.biomart.dino.dinos;

import org.biomart.common.resources.Log;
import org.biomart.processors.annotations.FieldInfo;
import org.biomart.processors.fields.IntegerField;

public class NetworkDino {
	
	@FieldInfo(clientDefined=true, required=true)
    private IntegerField nqueries = new IntegerField();
	
	public NetworkDino() {
		Log.debug(this.getClass().getName() + " has been invoked!");
	}

}
