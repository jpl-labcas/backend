package gov.nasa.jpl.edrn.labcas.filters;

import gov.nasa.jpl.edrn.labcas.utils.RsaUtils;

public class AuthorizationFilterTest {
	
	private static String privateKeyFilePath = "/Users/cinquini/ESGF_CONFIG/httpd/certs/hostkey.pem";
	
	private static String productId = "3c2f91f8-f20b-40ba-b0ad-269f42a8d299";
	
	public static void main(String[] args) {
		
		RsaUtils rsaUtils = new RsaUtils(privateKeyFilePath);
		
		String signature = rsaUtils.sign(productId);
		System.out.println("Signature="+signature);
		
		boolean tf = rsaUtils.verify(productId, signature);
		System.out.println(tf);
		
	}

}
