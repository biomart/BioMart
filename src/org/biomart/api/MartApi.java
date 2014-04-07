package org.biomart.api;

import java.sql.SQLException;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.configurator.test.MartConfigurator;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.queryEngine.QueryController;
import org.jdom.Document;

/* This class should not be used. Please see org.biomart.api.Portal and
 * org.biomart.api.Query classes.
 */


@Deprecated
public class MartApi {

	/**
	 * registry containing all the info
	 */
	private MartRegistry martRegistry = null;
		

    public MartApi(Document root, String keyFile) {
        MartConfigurator.initForWeb();
        this.martRegistry = new MartRegistry(root, keyFile);
    }
	
	
	public org.biomart.api.lite.MartRegistry getRegistry(String userName, String password) throws FunctionalException {
		return new org.biomart.api.lite.MartRegistry(this.martRegistry, userName, password);
	}
	
	public QueryController prepareQuery(String xmlString, String username)
            throws ClassNotFoundException, SQLException, TechnicalException, FunctionalException {
        return new QueryController(xmlString, this.martRegistry, username, false);
	}
}
