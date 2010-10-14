package nl.uiterlinden.android.productcodechecker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class KiesCheck {

	public KiesCheck() {
	}
	
	public String[] check() throws Exception {
		String reason = null;
		try {
			URL url = new URL("http://fus.samsungmobile.com/MS_TEST/msfus.php");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			int mode = 0; // normal
			String pda = "I9000XXJM1";	String csc = "I9000OXAJF3";		String phone = "I9000XXJM1";
//			String pda = "I9000XXJF3";	String csc = "I9000OXAJF3";		String phone = "I9000XXJF3";
			String contents = pda;
			String fwVersion = pda + "/" + csc + "/" + phone + "/" + contents;
			String buyerCode = "XX";
			String productCode = "XEU";
			
			String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><FUSMsg><FUSHdr><ProtoVer>1.0</ProtoVer><SessionID>0</SessionID><MsgID>1</MsgID></FUSHdr><FUSBody><Put><CmdID>1</CmdID><UPGRADE_MODE><Data>" + mode 
				+ "</Data></UPGRADE_MODE><CLIENT_LANGUAGE><Type>String</Type><Type>ISO 3166-1-alpha-3</Type><Data>0413</Data></CLIENT_LANGUAGE><CLIENT_PRODUCT><Data>Kies</Data></CLIENT_PRODUCT><CLIENT_VERSION><Data>1.5.1.10074.2</Data></CLIENT_VERSION><DEVICE_ANDROID_PDA_VERSION><Data>" + pda 
				+ "</Data></DEVICE_ANDROID_PDA_VERSION><DEVICE_ANDROID_CSC_VERSION><Data>" + csc 
				+ "</Data></DEVICE_ANDROID_CSC_VERSION><DEVICE_ANDROID_PHONE_VERSION><Data>" + phone 
				+ "</Data></DEVICE_ANDROID_PHONE_VERSION><DEVICE_ANDROID_CONTENTS_VERSION><Data>" + contents 
				+ "</Data></DEVICE_ANDROID_CONTENTS_VERSION><DEVICE_PLATFORM><Data>AndroidGSM</Data></DEVICE_PLATFORM><DEVICE_MODEL_NAME><Data>GT-I9000</Data></DEVICE_MODEL_NAME><DEVICE_FW_VERSION><Data>" + fwVersion 
				+ "</Data></DEVICE_FW_VERSION><DEVICE_BUYER_CODE><Data>" + buyerCode 
				+ "</Data></DEVICE_BUYER_CODE><DEVICE_PRODUCT_CODE><Data>" + productCode 
				+ "</Data></DEVICE_PRODUCT_CODE></Put><Get><CmdID>2</CmdID><LATEST_FW_VERSION/></Get></FUSBody></FUSMsg>";

			
			conn.addRequestProperty("Content-Type", "appliction/x-www-form-urlencoded");
			conn.addRequestProperty("User-Agent", "SAMSUNG_KIES");
			conn.addRequestProperty("Host", "fus.samsungmobile.com");
			conn.addRequestProperty("Content-Length", String.valueOf(body.length() + 2));
			conn.addRequestProperty("Expect", "100-continue");
			conn.addRequestProperty("Connection", "Keep-Alive");
			conn.addRequestProperty("Accept", "");
			
			DataOutputStream dataOutputStream = new DataOutputStream(conn.getOutputStream());
			dataOutputStream.writeBytes(body);
			
			dataOutputStream.writeBytes("\n\n");
			dataOutputStream.flush();
//			dataOutputStream.close();
			InputStreamReader reader = new InputStreamReader(conn.getInputStream());
			BufferedReader bReader = new BufferedReader(reader);
			StringBuilder message = new StringBuilder();
			String line = null;
			while ((line = bReader.readLine()) != null) {
				message.append(line);
			}
			bReader.close();
			
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			ResultHandler resultHandler = new ResultHandler();
			parser.parse(new ByteArrayInputStream(message.toString().getBytes()), resultHandler);
			
			return new String[] { resultHandler.getLatestPDAVersion(), resultHandler.getLatestPhoneVersion(), resultHandler.getLatestCSCVersion() };

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			reason = e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			reason = e.getMessage();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			reason = e.getMessage();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			reason = e.getMessage();
			
		}
		throw new Exception(reason);
	}
	
	class ResultHandler extends DefaultHandler {
		
		StringBuilder currentValue;
		String latestPdaVersion;
		String latestCscVersion;
		String latestPhoneVersion;
		String prevData = null;

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			currentValue = new StringBuilder();
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (localName.equals("Data")) {
				prevData = currentValue.toString();
			} else if (localName.equals("LATEST_ANDROIDGSM_PDA_VERSION")) {
				latestPdaVersion = prevData;
			} else if (localName.equals("LATEST_ANDROIDGSM_PHONE_VERSION")) {
				latestPhoneVersion = prevData;
			} else if (localName.equals("LATEST_ANDROIDGSM_CSC_VERSION")) {
				latestCscVersion = prevData;
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			currentValue.append(ch, start, length);
		}
		
		public String getLatestPDAVersion() {
			return latestPdaVersion;
		}
		
		public String getLatestCSCVersion() {
			return latestCscVersion;
		}
		
		public String getLatestPhoneVersion() {
			return latestPhoneVersion;
		}
		
	}
}
