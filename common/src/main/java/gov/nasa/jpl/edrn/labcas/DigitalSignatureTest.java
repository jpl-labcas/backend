package gov.nasa.jpl.edrn.labcas;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignedObject;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class DigitalSignatureTest {

	public static void main(String[] args) {

		try {
			
			Security.addProvider(new BouncyCastleProvider());
			System.out.println("BouncyCastle provider added.");
			KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
			
			PrivateKey privateKey = generatePrivateKey(keyFactory, "/Users/cinquini/ESGF_CONFIG/httpd/certs/hostkey.pem");
			
			//PrivateKey privateKey = RSA.getPrivateKey("/Users/cinquini/ESGF_CONFIG/httpd/certs/hostkey.pem");
			//System.out.println(privateKey.toString());
			
			//PublicKey publicKey = keyPair.getPublic();
			
			//PublicKey publicKey = getPublicKey("/Users/cinquini/ESGF_CONFIG/httpd/certs/hostcert.pem");
			PublicKey publicKey = generatePublicKey(keyFactory, "/Users/cinquini/ESGF_CONFIG/httpd/certs/hostcert.pem");
			

			// We can sign Serializable objects only
			String unsignedObject = new String("A Test Object");
			Signature signature = Signature.getInstance(privateKey.getAlgorithm());
			SignedObject signedObject = new SignedObject(unsignedObject, privateKey, signature);

			// Verify the signed object
			Signature sig = Signature.getInstance(publicKey.getAlgorithm());
			boolean verified = signedObject.verify(publicKey, sig);

			System.out.println("Is signed Object verified ? " + verified);

			// Retrieve the object
			unsignedObject = (String) signedObject.getObject();

			System.out.println("Unsigned Object : " + unsignedObject);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error:"+e.getMessage());
		}

	}


	
	private static PrivateKey generatePrivateKey(KeyFactory factory, String filename) throws InvalidKeySpecException, FileNotFoundException, IOException {
		
		PemFile pemFile = new PemFile(filename);
		byte[] content = pemFile.getPemObject().getContent();
		PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
		return factory.generatePrivate(privKeySpec);
		
	}
	
	private static PublicKey generatePublicKey(KeyFactory factory, String filename) throws CertificateException, InvalidKeySpecException, FileNotFoundException, IOException {
		
		PemFile pemFile = new PemFile(filename);
		byte[] content = pemFile.getPemObject().getContent();
		X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
		return factory.generatePublic(pubKeySpec);
		
		//CertificateFactory fact = CertificateFactory.getInstance("X.509");
	    //FileInputStream is = new FileInputStream (filename);
	    //Certificate cer = fact.generateCertificate(is);
	    //PublicKey key = cer.getPublicKey();
	    //return key;
	    
	}
	


}
