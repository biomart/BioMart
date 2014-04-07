package org.biomart.configurator.wizard.addsource;

import org.biomart.configurator.model.object.DataLinkInfo;
import org.jdom.Document;

public class AddSourceWizardObject {
	private String profileName;
	private DataLinkInfo dlinkInfo;
	private Document profile;
	

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}
	
	public String getProfileName() {
		return profileName;
	}
	
	public void setDlinkInfo(DataLinkInfo dlinkInfo) {
		this.dlinkInfo = dlinkInfo;
	}
	
	public DataLinkInfo getDlinkInfo() {
		return dlinkInfo;
	}

	public void setProfile(Document profile) {
		this.profile = profile;
	}

	public Document getProfile() {
		return profile;
	}
}