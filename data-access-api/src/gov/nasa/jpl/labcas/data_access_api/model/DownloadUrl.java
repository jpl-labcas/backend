package gov.nasa.jpl.labcas.data_access_api.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="urls")
public class DownloadUrl {
	
	private String url;
	
	public DownloadUrl() {}
	
	public DownloadUrl(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
}