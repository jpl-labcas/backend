package gov.nasa.jpl.edrn.labcas.utils;

import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

/**
 * Utility class to digitally sign and verify content.
 * 
 * @author Luca Cinquini
 *
 */
public class RsaUtils {

	private KeyPair keys = null;

	private final Log LOG = LogFactory.getLog(this.getClass());

	public RsaUtils(String privateKeyFilePath) {

		// read private/public keys into memory
		try {
			Security.addProvider(new BouncyCastleProvider());
			PEMReader pemReader = new PEMReader(new FileReader(privateKeyFilePath));
			if (LOG.isInfoEnabled()) LOG.info("Using private key: "+privateKeyFilePath);
			keys = (KeyPair) pemReader.readObject();
			pemReader.close();

		} catch (IOException e) {
			LOG.error(e.getMessage());
		}

	}

	/**
	 * Method to digitally sign a piece of content.
	 * 
	 * @param content
	 * @return
	 */
	public String sign(String content) {

		try {

			Signature sg = Signature.getInstance("SHA1withRSA");

			// use private key
			sg.initSign(this.keys.getPrivate());
			// add content
			sg.update(content.getBytes());
			// generate signature
			String signature = DatatypeConverter.printBase64Binary(sg.sign());

			return signature;

		} catch (Exception e) {
			LOG.error(e.getMessage());
			return null;
		}

	}

	/**
	 * Method to verify the signature of a piece of content.
	 * @param content
	 * @param signature
	 * @return
	 */
	public boolean verify(String content, String signature) {

		try {

			// load public key
			Signature sg = Signature.getInstance("SHA1withRSA");
			// use public key
			sg.initVerify(keys.getPublic());
			// add content
			sg.update(content.getBytes());
			// validate signature
			if (sg.verify(DatatypeConverter.parseBase64Binary(signature))) {

				if (LOG.isDebugEnabled()) { LOG.debug("Signature is valid"); }
				return true;

			} else {
				if (LOG.isDebugEnabled()) { LOG.debug("Signature is NOT valid"); }
			}

		} catch (SignatureException e1) {
			LOG.error(e1.getMessage());
		} catch (NoSuchAlgorithmException e2) {
			LOG.error(e2.getMessage());
		} catch (InvalidKeyException e3) {
			LOG.error(e3.getMessage());
		}

		return false;

	}

}
