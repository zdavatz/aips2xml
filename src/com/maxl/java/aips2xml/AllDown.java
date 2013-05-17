package com.maxl.java.aips2xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

public class AllDown {
	
	public void downAipsXls(String file_medical_infos_xsd, String file_medical_infos_xml) {
		// http://download.swissmedicinfo.ch/
		// ja, ja
		try {
			// Suppress all warnings!
			java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
			// Start timer 
			long startTime = System.currentTimeMillis();
			System.out.print("- Downloading AIPS file ... ");		
			
			WebClient webClient = new WebClient();
			HtmlPage currentPage = webClient.getPage("http://download.swissmedicinfo.ch/");
			// System.out.println(">>> " + currentPage.getWebResponse().getContentAsString());
			HtmlSubmitInput acceptBtn = currentPage.getElementByName("ctl00$MainContent$btnOK");
			currentPage = acceptBtn.click();
			acceptBtn = currentPage.getElementByName("ctl00$MainContent$BtnYes");	

			InputStream is = acceptBtn.click().getWebResponse().getContentAsStream();
			File destination = new File("./xml/tmp/aips.zip");
			FileUtils.copyInputStreamToFile(is, destination);
			webClient.closeAllWindows();
			
			unzipToTemp(destination);
			
	        // Copy file ./tmp/unzipped_preparations/Preparations.xml to ./xml/bag_preparations_xml.xml
			File folder = new File("./xml/tmp/unzipped_tmp");
			File[] listOfFiles = folder.listFiles();
			for (int i=0; i<listOfFiles.length; ++i) {
				if (listOfFiles[i].isFile()) {
					String file = listOfFiles[i].getName();
					if (file.endsWith(".xml")) {
				        File src = new File("./xml/tmp/unzipped_tmp/" + file);
				        File dst = new File(file_medical_infos_xml);
				        FileUtils.copyFile(src, dst);
						// Stop timer 
						long stopTime = System.currentTimeMillis();				        
						System.out.println(dst.length()/1024 + " kB in " + (stopTime-startTime)/1000.0f + " sec");
					} else if (file.endsWith(".xsd")) {
				        File src = new File("./xml/tmp/unzipped_tmp/" + file);
				        File dst = new File(file_medical_infos_xsd);
				        FileUtils.copyFile(src, dst);
					}
				}
			}
		
	        // Delete folder ./tmp
	        FileUtils.deleteDirectory(new File("./xml/tmp"));	     			
		} catch(IOException e) {
			//
		}		
	}
	
	public void downPackungenXls(String file_packages_xls) {
		try {
			// Start timer 
			long startTime = System.currentTimeMillis();			
			System.out.print("- Downloading Packungen file ... ");	
			URL url = new URL("http://www.swissmedic.ch/daten/00080/00251/index.html?lang=de&download=NHzLpZeg7t,lnp6I0NTU042l2Z6ln1acy4Zn4Z2qZpnO2Yuq2Z6gpJCDdH56fWym162epYbg2c_JjKbNoKSn6A--&.xls");
			File destination = new File(file_packages_xls);
			FileUtils.copyURLToFile(url, destination);
			long stopTime = System.currentTimeMillis();	
			System.out.println(destination.length()/1024 + " kB in " + (stopTime-startTime)/1000.0f + " sec");
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void downSwissindexXml(String language, String file_refdata_pharma_xml) {	
		// Start timer 
		long startTime = System.currentTimeMillis();
		System.out.print("- Downloading Swissindex (" + language + ") file ... ");			
		
		try {
			SOAPMessage soapRequest = MessageFactory.newInstance().createMessage();

			// Setting SOAPAction header line
	        MimeHeaders headers = soapRequest.getMimeHeaders();
	        headers.addHeader("SOAPAction", "http://swissindex.e-mediat.net/SwissindexPharma_out_V101/DownloadAll");	
	        
			SOAPPart soapPart = soapRequest.getSOAPPart();
			SOAPEnvelope envelope = soapPart.getEnvelope();
			SOAPBody soapBody = envelope.getBody();
			// Construct SOAP request message
			SOAPElement soapBodyElement1 = soapBody.addChildElement("pharmacode");
			soapBodyElement1.addNamespaceDeclaration("", "http://swissindex.e-mediat.net/SwissindexPharma_out_V101");
			soapBodyElement1.addTextNode("DownloadAll");
			SOAPElement soapBodyElement2 = soapBody.addChildElement("lang");
			soapBodyElement2.addNamespaceDeclaration("", "http://swissindex.e-mediat.net/SwissindexPharma_out_V101");
			if (language.equals("DE"))
				soapBodyElement2.addTextNode("DE");
			else if (language.equals("FR"))
				soapBodyElement2.addTextNode("FR");
			else {
				System.err.println("down_swissindex_xml: wrong language!");
				return;
			}
			soapRequest.saveChanges();
			
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();			
			SOAPConnection connection = soapConnectionFactory.createConnection();			
			String wsURL = "https://swissindex.refdata.ch/Swissindex/Pharma/ws_Pharma_V101.asmx?WSDL";
			SOAPMessage soapResponse = connection.call(soapRequest, wsURL);

			Document doc = soapResponse.getSOAPBody().extractContentAsDocument();
			String strBody = getStringFromDoc(doc);
			String xmlBody = prettyFormat(strBody);
			// Note: parsing the Document tree and using the removeAttribute function is hopeless! 
			xmlBody = xmlBody.replaceAll("xmlns.*?\".*?\" ", "");			
			long len = writeToFile(xmlBody, file_refdata_pharma_xml);
			long stopTime = System.currentTimeMillis();	
			System.out.println(len/1024 + " kB in " + (stopTime-startTime)/1000.0f + " sec");
			
			connection.close();			
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void downPreparationsXml(String file_preparations_xml) {
		// http://bag.e-mediat.net/SL2007.Web.External/File.axd?file=XMLPublications.zip
		try {
			// Start timer 
			long startTime = System.currentTimeMillis();
			System.out.print("- Downloading Preparations file ... ");	
			
			URL url = new URL("http://bag.e-mediat.net/SL2007.Web.External/File.axd?file=XMLPublications.zip");
			File destination = new File("./xml/tmp/preparations.zip");
			FileUtils.copyURLToFile(url, destination);
			
			unzipToTemp(destination);
	        
	        // Copy file ./tmp/unzipped_preparations/Preparations.xml to ./xml/bag_preparations_xml.xml
	        File src = new File("./xml/tmp/unzipped_tmp/Preparations.xml");
	        File dst = new File(file_preparations_xml);
	        FileUtils.copyFile(src, dst);
	        long stopTime = System.currentTimeMillis();	
	        System.out.println(dst.length()/1024 + " kB in " + (stopTime-startTime)/1000.0f + " sec");

	        // Delete folder ./tmp
	        FileUtils.deleteDirectory(new File("./xml/tmp"));	        
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
	
	private void unzipToTemp(File dst) {
		try {
			ZipInputStream zin = new ZipInputStream(new FileInputStream(dst));
			String workingDir = "./xml/tmp" + File.separator + "unzipped_tmp";
			byte buffer[] = new byte[4096];
		    int bytesRead;	
		    
			ZipEntry entry = null;		    
	        while ((entry = zin.getNextEntry()) != null) {
	            String dirName = workingDir;
	
	            int endIndex = entry.getName().lastIndexOf(File.separatorChar);
	            if (endIndex != -1) {
	                dirName += entry.getName().substring(0, endIndex);
	            }
	
	            File newDir = new File(dirName);
	            // If the directory that this entry should be inflated under does not exist, create it
	            if (!newDir.exists() && !newDir.mkdir()) { 
	            	throw new ZipException("Could not create directory " + dirName + "\n"); 
	            }
	
	            // Copy data from ZipEntry to file
	            FileOutputStream fos = new FileOutputStream(workingDir + File.separator + entry.getName());
	            while ((bytesRead = zin.read(buffer)) != -1) {
	                fos.write(buffer, 0, bytesRead);
	            }
	            fos.close();
	        }
	        zin.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private long writeToFile(String stringToWrite, String filename) {
        try {
			File wfile = new File(filename);
			if (!wfile.exists())
				wfile.createNewFile();
			BufferedWriter bw = new BufferedWriter
					(new OutputStreamWriter(new FileOutputStream(wfile.getAbsoluteFile()),"UTF-8"));   			
			bw.write(stringToWrite);
			bw.close();
			return wfile.length();
 		} catch (IOException e) {
			e.printStackTrace();
 		}		
        return 0;
	}	
	
	private String getStringFromDoc(Document doc)    {
	    DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
	    LSSerializer lsSerializer = domImplementation.createLSSerializer();
	    return lsSerializer.writeToString(doc);   
	}
	
	private String prettyFormat(String input) {
	    try {
	        Source xmlInput = new StreamSource(new StringReader(input));
	        StringWriter stringWriter = new StringWriter();
	        StreamResult xmlOutput = new StreamResult(stringWriter);
	        Transformer transformer = TransformerFactory.newInstance().newTransformer(); 
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	        transformer.transform(xmlInput, xmlOutput);
	        return xmlOutput.getWriter().toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e); // simple exception handling, please review it
	    }
	}
}
