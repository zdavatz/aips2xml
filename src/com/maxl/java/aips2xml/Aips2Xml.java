/*
Copyright (c) 2013 Max Lungarella

This file is part of Aips2Xml.

Aips2Xml is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.maxl.java.aips2xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.select.Elements;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.maxl.java.aips2xml.Preparations.Preparation;

public class Aips2Xml {

	// Set by command line options (default values)
	private static String DB_LANGUAGE = "";
	private static boolean SHOW_ERRORS = false;
	private static boolean SHOW_LOGS = true;
	private static String VERSION = "1.0.0";
	private static String MED_TITLE = "";
	private static boolean DOWNLOAD_ALL = true;
	private static boolean ZIP_XML = false;

	// XML and XSD files to be parsed (contains DE and FR -> needs to be extracted)
	private static final String FILE_MEDICAL_INFOS_XML = "./xml/aips_xml.xml";
	private static final String FILE_MEDICAL_INFOS_XSD = "./xml/aips_xsd.xsd";
	// Excel file to be parsed (DE = FR)
	private static final String FILE_PACKAGES_XLS = "./xls/swissmedic_packages_xls.xls";
	// ****** ATC class xls file (DE != FR) ******
	private static final String FILE_ATC_CLASSES_XLS = "./xls/wido_arz_amtl_atc_index_0113_xls.xls";
	private static final String FILE_ATC_MULTI_LINGUAL_TXT = "./xls/atc_codes_multi_lingual.txt";
	// ****** Refdata xml file to be parsed (DE != FR) ******
	private static final String FILE_REFDATA_PHARMA_DE_XML = "./xml/refdata_pharma_de_xml.xml";
	private static final String FILE_REFDATA_PHARMA_FR_XML = "./xml/refdata_pharma_fr_xml.xml";
	// BAG xml file to be parsed (contains DE and FR)
	private static final String FILE_PREPARATIONS_XML = "./xml/bag_preparations_xml.xml";
	
	// ****** Parse reports (DE != FR) ****** 
	private static final String FILE_REPORT_BASE = "./reports/parse_report";

	// Map to list with all the relevant information
	// HashMap is faster, but TreeMap is sort by the key :)
	private static Map<String, ArrayList<String>> package_info = new TreeMap<String, ArrayList<String>>();	
	
	// Map to String of atc classes, key is the ATC-code or any of its substrings
	private static Map<String, String> atc_map = new TreeMap<String, String>();
	
	// Map to String of additional info, key is the SwissmedicNo5
	private static Map<String, String> add_info_map = new TreeMap<String, String>();
	
	// Global variables
	private static String mPackSection_str = "";
	
	/**
	 * Adds an option into the command line parser
	 * @param optionName - the option name
	 * @param description - option descriptiuon
	 * @param hasValue - if set to true, --option=value, otherwise, --option is a boolean
	 * @param isMandatory - if set to true, the option must be provided.
	 */
	@SuppressWarnings("static-access")
	static void addOption(Options opts, String optionName, String description, boolean hasValue, boolean isMandatory) {
		OptionBuilder opt = OptionBuilder.withLongOpt(optionName);
		opt = opt.withDescription(description);
		if (hasValue) 
			opt = opt.hasArg();
		if(isMandatory) 
			opt = opt.isRequired();
		opts.addOption(opt.create());
	}	
	
	static void commandLineParse(Options opts, String[] args) {
		CommandLineParser parser = new GnuParser();
		try {
			CommandLine cmd = parser.parse(opts, args);
			if (cmd.hasOption("help")) {				
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("aips2xml", opts);
				System.exit(0);
			}
			if (cmd.hasOption("version")) {
				System.out.println("Version of aips2xml: " + VERSION);
			}
			if (cmd.hasOption("lang")) {
				if (cmd.getOptionValue("lang").equals("de"))
					DB_LANGUAGE = "de"; 
				else if (cmd.getOptionValue("lang").equals("fr"))
					DB_LANGUAGE = "fr";
			}
			if (cmd.hasOption("verbose"))
				SHOW_ERRORS = true;
			if (cmd.hasOption("quiet")) {
				SHOW_ERRORS = false;
				SHOW_LOGS = false;
			}
			if (cmd.hasOption("zip")) {
				ZIP_XML = true;
			}
			if (cmd.hasOption("alpha")) {
				MED_TITLE = cmd.getOptionValue("alpha");
			}
			if (cmd.hasOption("nodown")) {
				DOWNLOAD_ALL = false;
			}
		} catch(ParseException e) {
			System.err.println("Parsing failed: " + e.getMessage());
		}
	}	
	
	public static void main(String[] args) {
		Options options = new Options();
		addOption(options, "help", "print this message", false, false );
		addOption(options, "version", "print the version information and exit", false, false);
		addOption(options, "quiet", "be extra quiet", false, false);
		addOption(options, "verbose", "be extra verbose", false, false);
		addOption(options, "zip", "generate zip file", false, false);
		addOption(options, "lang", "use given language", true, false);
		addOption(options, "alpha", "only include titles which start with option value", true, false);
		addOption(options, "nodown", "no download, parse only", false, false);

		commandLineParse(options, args);
		
		// Download all files and save them in appropriate directories
		if (DOWNLOAD_ALL) {
			System.out.println("");
			allDown();		
		}
		
		DateFormat df = new SimpleDateFormat("ddMMyy");
		String date_str = df.format(new Date());

		System.out.println("");
		if (!DB_LANGUAGE.isEmpty()) {
			extractPackageInfo();
			
			List<MedicalInformations.MedicalInformation> med_list = readAipsFile();

			if (SHOW_LOGS) {
				System.out.println("");
				System.out.println("- Generating xml and html files ... ");
			}
			long startTime = System.currentTimeMillis();
			int counter = 0;
			String fi_complete_xml = "";		
			for (MedicalInformations.MedicalInformation m : med_list) {
				if( m.getLang().equals(DB_LANGUAGE) && m.getType().equals("fi") ) {
					if (m.getTitle().startsWith(MED_TITLE)) {		
						if (SHOW_LOGS)
							System.out.println(++counter + ": " + m.getTitle());			
						String[] html_str = extractHtmlSection(m);
						// html_str[0] -> registration numbers
						// html_str[1] -> content string
						String xml_str = convertHtmlToXml(m.getTitle(), html_str[1], html_str[0]);
						if (DB_LANGUAGE.equals("de")) {
							if (!html_str[0].isEmpty()) {
								String name = m.getTitle();
								// Replace all "Sonderzeichen"
								name = name.replaceAll("[/%:]", "_");
								writeToFile(html_str[1], "./fis/fi_de_html/", name + "_fi_de.html");
								writeToFile(xml_str, "./fis/fi_de_xml/", name + "_fi_de.xml");
								fi_complete_xml += (xml_str + "\n");
							}
						} else if (DB_LANGUAGE.equals("fr")) {
							if (!html_str[0].isEmpty()) {
								String name = m.getTitle();
								// Replace all "Sonderzeichen"
								name = name.replaceAll("[/%:]", "_");
								writeToFile(html_str[1], "./fis/fi_fr_html/", name + "_fi_fr.html");
								writeToFile(xml_str, "./fis/fi_fr_xml/", name + "_fi_fr.xml");
								fi_complete_xml += (xml_str + "\n");	
							}
						}
					}
				}
			}
			
			// Add header to huge xml
			fi_complete_xml = addHeaderToXml(fi_complete_xml);
			// Dump to file
			if (DB_LANGUAGE.equals("de")) {
				writeToFile(fi_complete_xml, "./fis/", "fi_de.xml");
				if (ZIP_XML)
					zipToFile("./fis/", "fi_de.xml");
			}
			else if (DB_LANGUAGE.equals("fr")) {
				writeToFile(fi_complete_xml, "./fis/", "fi_fr.xml");
				if (ZIP_XML)
					zipToFile("./fis/", "fi_fr.xml");				
			}
			
			if (SHOW_LOGS) {
				long stopTime = System.currentTimeMillis();
				System.out.println("- Generated " + counter + " xml and html files in " + (stopTime-startTime)/1000.0f + " sec");
			}
		}
				
		System.exit(0);
	}
	
	static void allDown() {
		AllDown a = new AllDown();
			
		a.downAipsXls(FILE_MEDICAL_INFOS_XSD, FILE_MEDICAL_INFOS_XML);		
		a.downPackungenXls(FILE_PACKAGES_XLS);
		a.downSwissindexXml("DE", FILE_REFDATA_PHARMA_DE_XML);
		a.downSwissindexXml("FR", FILE_REFDATA_PHARMA_FR_XML);
		a.downPreparationsXml(FILE_PREPARATIONS_XML);
	}
	
	static void extractPackageInfo() {		
		try {			
			long startTime = System.currentTimeMillis();			
			if (SHOW_LOGS)
				System.out.print("- Processing packages xls ... ");
			// Load Swissmedic xls file
			FileInputStream packages_file = new FileInputStream(FILE_PACKAGES_XLS);
			// Get workbook instance for XLS file (HSSF = Horrible SpreadSheet Format)
			HSSFWorkbook packages_workbook = new HSSFWorkbook(packages_file);
			// Get first sheet from workbook
			HSSFSheet packages_sheet = packages_workbook.getSheetAt(0);
			// Iterate through all rows of first sheet
			Iterator<Row> rowIterator = packages_sheet.iterator();
						
			int num_rows = 0;			
			while (rowIterator.hasNext()) {				
				Row row = rowIterator.next();
				if (num_rows>3) {
					String swissmedic_no5 = ""; // SwissmedicNo5 registration number (5 digits)
					String sequence_name = "";
					String package_id = "";		
					String swissmedic_no8 = "";	// SwissmedicNo8 = SwissmedicNo5 + Package id (8 digits)
					String heilmittel_code = "";
					String package_size = "";
					String package_unit = "";
					String swissmedic_cat = "";
					String application_area = "";
					String public_price = "";
					String exfactory_price = "";
					String therapeutic_index = "";
					String withdrawn_str = "";
					String speciality_str = "";
					String plimitation_str = "";
					String add_info_str = "";	// Contains additional information separated by ;
					
					// 0: Zulassungsnnr, 1: Sequenz, 2: Sequenzname, 3: Zulassunginhaberin, 4: T-Nummer, 5: ATC-Code, 6: Heilmittelcode
					// 7: Erstzulassung Prï¿½parat, 8: Zulassungsdatum Sequenz, 9: Gï¿½ltigkeitsdatum, 10: Verpackung, 11: Packungsgrï¿½sse
					// 12: Einheit, 13: Abgabekategorie, 14: Wirkstoff, 15: Zusammensetzung, 16: Anwendungsgebiet Prï¿½parat, 17: Anwendungsgebiet Sequenz
	
					if (row.getCell(0)!=null) 
						swissmedic_no5 = row.getCell(0).getStringCellValue();  	// Swissmedic registration number (5 digits)			
					if (row.getCell(2)!=null) 
						sequence_name = row.getCell(2).getStringCellValue();	// Sequence name
					if (row.getCell(6)!=null) 
						heilmittel_code = row.getCell(6).getStringCellValue();
					if (row.getCell(11)!=null) 
						package_size = row.getCell(11).getStringCellValue();
					if (row.getCell(12)!=null) 
						package_unit = row.getCell(12).getStringCellValue();
					if (row.getCell(13)!=null) 
						swissmedic_cat = row.getCell(13).getStringCellValue();
					if (row.getCell(16)!=null)
						application_area = row.getCell(16).getStringCellValue();
					
					if (row.getCell(10)!=null) {
						package_id = row.getCell(10).getStringCellValue();
						swissmedic_no8 = swissmedic_no5 + package_id;
						// Fill in row
						ArrayList<String> pack = new ArrayList<String>();
						pack.add(swissmedic_no5);	 					// 0
						pack.add(sequence_name);	 					// 1
						pack.add(heilmittel_code);	 					// 2
						pack.add(package_size);		 					// 3
						pack.add(package_unit);		 					// 4
						pack.add(swissmedic_cat);	 					// 5
						if (!application_area.isEmpty())
							pack.add(application_area + " (Swissmedic)\n"); // 6 = swissmedic + bag
						else
							pack.add("");
						pack.add(public_price);		 					// 7
						pack.add(exfactory_price);	 					// 8
						pack.add(therapeutic_index); 					// 9
						pack.add(withdrawn_str);	 					// 10
						pack.add(speciality_str);	 					// 11	
						pack.add(plimitation_str);	 					// 12
						pack.add(add_info_str);		 					// 13

						package_info.put(swissmedic_no8, pack);
					}
				}
				num_rows++;
			}
			long stopTime = System.currentTimeMillis();			
			if (SHOW_LOGS) {
				System.out.println((package_info.size()+1) + " packages in " + (stopTime-startTime)/1000.0f + " sec");				
			}
			startTime = System.currentTimeMillis();
			if (SHOW_LOGS) 
				System.out.print("- Processing atc classes xls ... ");	
			if (DB_LANGUAGE.equals("de")) {
				// Load ATC classes xls file
				FileInputStream atc_classes_file = new FileInputStream(FILE_ATC_CLASSES_XLS);
				// Get workbook instance for XLS file (HSSF = Horrible SpreadSheet Format)
				HSSFWorkbook atc_classes_workbook = new HSSFWorkbook(atc_classes_file);
				// Get first sheet from workbook
				HSSFSheet atc_classes_sheet = atc_classes_workbook.getSheetAt(1);
				// Iterate through all rows of first sheet
				rowIterator = atc_classes_sheet.iterator();
					
				num_rows = 0;
				while (rowIterator.hasNext()) {				
					Row row = rowIterator.next();
					if (num_rows>2) {
						String atc_code = "";
						String atc_class = "";
						if (row.getCell(0)!=null) {
							atc_code = row.getCell(0).getStringCellValue().replaceAll("\\s", "");
						}
						if (row.getCell(2)!=null) {
							atc_class = row.getCell(2).getStringCellValue();							
						}
						// Build a full map atc code to atc class
						if (atc_code.length()>0) {
							atc_map.put(atc_code, atc_class);
						}
					}
					num_rows++;
				}				
			} else if (DB_LANGUAGE.equals("fr")) {
				// Load multilinguagl ATC classes txt file
				String atc_classes_multi = readFromFile(FILE_ATC_MULTI_LINGUAL_TXT);
				// Loop through all lines
				Scanner scanner = new Scanner(atc_classes_multi);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					List<String> atc_class = Arrays.asList(line.split(": "));
					String atc_code = atc_class.get(0);
					String[] atc_classes_str = atc_class.get(1).split(";");
					String atc_class_french = atc_classes_str[1].trim();
					atc_map.put(atc_code, atc_class_french);
				}
				scanner.close();
			}
			stopTime = System.currentTimeMillis();
			if (SHOW_LOGS)
				System.out.println((atc_map.size()+1) + " classes in " + (stopTime-startTime)/1000.0f + " sec");			
			// Load Refdata xml file
			File refdata_xml_file = null;
			if (DB_LANGUAGE.equals("de"))
				refdata_xml_file = new File(FILE_REFDATA_PHARMA_DE_XML);
			else if (DB_LANGUAGE.equals("fr"))
				refdata_xml_file = new File(FILE_REFDATA_PHARMA_FR_XML);				
			else {
				System.err.println("ERROR: DB_LANGUAGE undefined");
				System.exit(1);
			}
			FileInputStream refdata_fis = new FileInputStream(refdata_xml_file);
			
			startTime = System.currentTimeMillis();
			if (SHOW_LOGS)
				System.out.print("- Unmarshalling Refdata Pharma " + DB_LANGUAGE + " ... ");
			
			JAXBContext context = JAXBContext.newInstance(Pharma.class);			
			Unmarshaller um = context.createUnmarshaller();			
			Pharma refdataPharma = (Pharma) um.unmarshal(refdata_fis);
			List<Pharma.ITEM> pharma_list = refdataPharma.getItem();
			
			String smno8;
			for (Pharma.ITEM pharma : pharma_list) {
				String ean_code = pharma.getGtin();
				if (ean_code.length()==13) {
					smno8 = ean_code.substring(4, 12);
					// Extract pharma corresponding to swissmedicno8
					ArrayList<String> pi_row = package_info.get(smno8);					
					// Replace sequence_name
					if (pi_row!=null) {
						if (pharma.getAddscr().length()>0)
							pi_row.set(1, pharma.getDscr() + ", " + pharma.getAddscr());
						else
							pi_row.set(1, pharma.getDscr());
						if (pharma.getStatus().equals("I")) {
							if (DB_LANGUAGE.equals("de"))
								pi_row.set(10, "a.H.");
							else if (DB_LANGUAGE.equals("fr"))
								pi_row.set(10, "p.c.");
						}
					} else {
						if (SHOW_ERRORS)
							System.err.println(">> Does not exist in BAG xls: " + smno8 + " (" + pharma.getDscr() + ", " + pharma.getAddscr() + ")");
					}
						
				} 
				else if (ean_code.length()<13 ) {
					if (SHOW_ERRORS)
						System.err.println(">> EAN code too short: " + ean_code + ": " + pharma.getDscr());
				} else if (ean_code.length()>13 ) {
					if (SHOW_ERRORS)
						System.err.println(">> EAN code too long: " + ean_code + ": " + pharma.getDscr());
				}
			}
			
			stopTime = System.currentTimeMillis();
			if (SHOW_LOGS)
				System.out.println(pharma_list.size() + " medis in " + (stopTime-startTime)/1000.0f + " sec");
			
			// Load BAG xml file					
			File bag_xml_file = new File(FILE_PREPARATIONS_XML);
			FileInputStream fis_bag = new FileInputStream(bag_xml_file);

			startTime = System.currentTimeMillis();
			if (SHOW_LOGS)
				System.out.print("- Processing preparations xml ... ");
			
			context = JAXBContext.newInstance(Preparations.class);			
			um = context.createUnmarshaller();			
			Preparations prepInfos = (Preparations) um.unmarshal(fis_bag);
			List<Preparations.Preparation> prep_list = prepInfos.getPreparations();

			int num_preparations = 0;
			for (Preparations.Preparation prep : prep_list) {
				String swissmedicno5_str = prep.getSwissmedicNo5();
				if (swissmedicno5_str!=null) {
					String orggencode_str = "";		// "O", "G" or empty -> ""
					String flagSB20_str = "";		// "Y" -> 20% or "N" -> 10%							
					if (prep.getOrgGenCode()!=null)
						orggencode_str = prep.getOrgGenCode();
					if (prep.getFlagSB20()!=null) {
						flagSB20_str = prep.getFlagSB20();
						if (flagSB20_str.equals("Y")) {
							if (DB_LANGUAGE.equals("de"))
								flagSB20_str = "SB 20%";
							else if (DB_LANGUAGE.equals("fr"))
								flagSB20_str = "QP 20%";
						} else if (flagSB20_str.equals("N")) {
							if (DB_LANGUAGE.equals("de"))
								flagSB20_str = "SB 10%";
							else if (DB_LANGUAGE.equals("fr"))
								flagSB20_str = "QP 10%";
						}
						else
							flagSB20_str = "";
					}
					add_info_map.put(swissmedicno5_str, orggencode_str + ";" + flagSB20_str);
				}
				
				List<Preparation.Packs> packs_list = prep.getPacks();
				for (Preparation.Packs packs : packs_list ) {
					// Extract codes for therapeutic index / classification
					String bag_application = "";
					String therapeutic_code = "";
					List<Preparations.Preparation.ItCodes> itcode_list = prep.getItCodes();
					for (Preparations.Preparation.ItCodes itc : itcode_list) {
						List<Preparations.Preparation.ItCodes.ItCode> code_list = itc.getItCode();
						int index = 0;
						for (Preparations.Preparation.ItCodes.ItCode code : code_list) {
							if (index==0) {
								if (DB_LANGUAGE.equals("de"))
									therapeutic_code = code.getDescriptionDe();
								else if (DB_LANGUAGE.equals("fr"))
									therapeutic_code = code.getDescriptionFr();								
							}
							else { 
								if (DB_LANGUAGE.equals("de"))								
									bag_application = code.getDescriptionDe();
								else if (DB_LANGUAGE.equals("fr"))
									bag_application = code.getDescriptionFr();								
							}	
							index++;
						}
					}					
					// Generate new package info
					List<Preparation.Packs.Pack> pack_list = packs.getPack();
					for (Preparation.Packs.Pack pack : pack_list) {
						// Get SwissmedicNo8 and used it as a key to extract all the relevant package info
						String swissMedicNo8 = pack.getSwissmedicNo8();
						ArrayList<String> pi_row = package_info.get(swissMedicNo8);
						// Preparation also in BAG xml file (we have a price)
						if (pi_row!=null) {
							// Update Swissmedic catory if necessary ("N->A", Y->"A+")
							if (pack.getFlagNarcosis().equals("Y"))
								pi_row.set(5, pi_row.get(5)+"+");		
							// Extract point limitations
							List<Preparations.Preparation.Packs.Pack.PointLimitations> point_limits = pack.getPointLimitations();
							for (Preparations.Preparation.Packs.Pack.PointLimitations limits : point_limits) {
								List<Preparations.Preparation.Packs.Pack.PointLimitations.PointLimitation> plimits_list
									= limits.getPointLimitation();
								if (plimits_list.size()>0)
									if (plimits_list.get(0)!=null)
										pi_row.set(12, ", LIM" + plimits_list.get(0).getPoints() + "");								
							}
							// Extract exfactory and public prices
							List<Preparations.Preparation.Packs.Pack.Prices> price_list = pack.getPrices();						
							for (Preparations.Preparation.Packs.Pack.Prices price : price_list) {
								List<Preparations.Preparation.Packs.Pack.Prices.PublicPrice> public_price = 
										price.getPublicPrice();
								List<Preparations.Preparation.Packs.Pack.Prices.ExFactoryPrice> exfactory_price =
										price.getExFactoryPrice();
								if (exfactory_price.size()>0) {
									try {
										float f = Float.valueOf(exfactory_price.get(0).getPrice());
										String ep = String.format("%.2f", f);
										pi_row.set(8, "CHF " + ep);
									} catch (NumberFormatException e) {
										if (SHOW_ERRORS)
											System.err.println("Number format exception (exfactory price): " + swissMedicNo8 + " (" + public_price.size() + ")");
									}
									
								}
								if (public_price.size()>0) {			
									try {
										float f = Float.valueOf(public_price.get(0).getPrice());
										String pp = String.format("%.2f", f);
										pi_row.set(7, "CHF " + pp);
										if (DB_LANGUAGE.equals("de"))
											pi_row.set(11, ", SL");
										else if (DB_LANGUAGE.equals("fr"))
											pi_row.set(11, ", LS");
									} catch (NumberFormatException e) {
										if (SHOW_ERRORS)
											System.err.println("Number format exception (public price): " + swissMedicNo8 + " (" + public_price.size() + ")");
									}
								} 							
								// Add application area and therapeutic code
								if (!bag_application.isEmpty())
									pi_row.set(6, pi_row.get(6) + bag_application + " (BAG)");
								pi_row.set(9, therapeutic_code);
							}
						}
					}
				}
				num_preparations++;
			}

			stopTime = System.currentTimeMillis();
			if (SHOW_LOGS)
				System.out.println(num_preparations + " preparations in " + (stopTime-startTime)/1000.0f + " sec");					
			
			// Loop through all SwissmedicNo8 numbers
			for (Map.Entry<String, ArrayList<String>> entry : package_info.entrySet()) {
				String swissmedicno8 = entry.getKey();
				ArrayList<String> pi_row = entry.getValue();
			}
			
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();	    
		}		
	}
	
	static List<MedicalInformations.MedicalInformation> readAipsFile() {
		List<MedicalInformations.MedicalInformation> med_list = null;
		try {
			JAXBContext context = JAXBContext.newInstance(MedicalInformations.class);

			// Validation
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema( new File(FILE_MEDICAL_INFOS_XSD) );
			Validator validator = schema.newValidator();
			validator.setErrorHandler( new MyErrorHandler() );
			
			// Marshaller
			/*
			Marshaller ma = context.createMarshaller();
			ma.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			MedicalInformations medi_infos = new MedicalInformations();
			ma.marshal(medi_infos, System.out);
			*/
			// Unmarshaller	
			long startTime = System.currentTimeMillis();
			if (SHOW_LOGS)
				System.out.print("- Unmarshalling Swissmedic xml ... ");
			
			FileInputStream fis = new FileInputStream(new File(FILE_MEDICAL_INFOS_XML));			
			Unmarshaller um = context.createUnmarshaller();			
			MedicalInformations med_infos = (MedicalInformations) um.unmarshal(fis);
			med_list = med_infos.getMedicalInformation();
			
			long stopTime = System.currentTimeMillis();
			if (SHOW_LOGS)
				System.out.println(med_list.size() + " medis in " + (stopTime-startTime)/1000.0f + " sec");			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();	
		} catch (SAXException e) {
			e.printStackTrace();	
		}
		
		return med_list;		
	}
	
	static String[] extractHtmlSection(MedicalInformations.MedicalInformation m) {	
		// Extract section titles and section ids
		MedicalInformations.MedicalInformation.Sections med_sections = m.getSections();
		List<MedicalInformations.MedicalInformation.Sections.Section> med_section_list = med_sections.getSection();

		Document doc = Jsoup.parse(m.getContent());
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		
		// Clean html code
		HtmlUtils html_utils = new HtmlUtils(m.getContent());
		html_utils.clean();					
		
		// Extract registration number (swissmedic no5)
		String regnr_str = "";
		if (DB_LANGUAGE.equals("de"))
			regnr_str = html_utils.extractRegNrDE(m.getTitle());
		else if (DB_LANGUAGE.equals("fr"))
			regnr_str = html_utils.extractRegNrFR(m.getTitle());
		
		// Sanitize html
		String html_sanitized = "";								
		// First check for bad boys (version=1! but actually version>1!)
		if (!m.getVersion().equals("1") || m.getContent().substring(0, 20).contains("xml")) {
			for (int i=1; i<22; ++i) {
				html_sanitized += html_utils.sanitizeSection(i, m.getTitle(), DB_LANGUAGE);
			}
			html_sanitized = "<div id=\"monographie\">" + html_sanitized + "</div>" ;						
		} else {
			html_sanitized = m.getContent();
		}
		
		// Update "Packungen" section and extract therapeutisches index
		List<String> mTyIndex_list = new ArrayList<String>();						
		String mContent_str = updateSectionPackungen(m.getTitle(), package_info, regnr_str, html_sanitized, mTyIndex_list);
		m.setContent(mContent_str);
								
		// Fix problem with wrong div class in original Swissmedic file
		if (DB_LANGUAGE.equals("de")) {
			m.setStyle(m.getStyle().replaceAll("untertitel", "untertitle"));
			m.setStyle(m.getStyle().replaceAll("untertitel1", "untertitle1"));
		}
		
		// Correct formatting error introduced by Swissmedic
		m.setAuthHolder(m.getAuthHolder().replaceAll("&#038;","&"));
		
		// Extracts only *first* registration number
		/*
		List<String> swissmedicno5_list = Arrays.asList(regnr_str.split("\\s*,\\s*"));		
		String[] swno5_content_map = {swissmedicno5_list.get(0), mContent_str};
		*/
		// Extract *all* registration numbers
		String[] swno5_content_map = {regnr_str, mContent_str};
		
		return swno5_content_map; //mContent_str;
	}
	
	static String updateSectionPackungen(String title, Map<String,ArrayList<String>> pack_info, String regnr_str, String content_str, 
			List<String> tIndex_list) {		
		Document doc = Jsoup.parse(content_str, "UTF-16");
		List<String> pinfo_str = new ArrayList<String>();		
		int index = 0;
		
		// Extract swissmedicno5 registration numbers
		List<String> swissmedicno5_list = Arrays.asList(regnr_str.split("\\s*,\\s*"));
		for (String s : swissmedicno5_list) {
			// Extract original / generika info + Selbstbehalt info from "add_info_map"
			String orggen_str = "";
			String flagsb_str = "";
			String addinfo_str = add_info_map.get(s);
			if (addinfo_str!=null) {
				List<String> ai_list = Arrays.asList(addinfo_str.split("\\s*;\\s*"));
				if (ai_list!=null) {
					if (!ai_list.get(0).isEmpty())
						orggen_str = ", " + ai_list.get(0);				
					if (!ai_list.get(1).isEmpty())
						flagsb_str = ", " + ai_list.get(1);
				}
			}
			// Now generate many swissmedicno8 = swissmedicno5 + ***, check if they're keys and retrieve package info			
			String swissmedicno8_key = "";
			for (int n=0; n<1000; ++n) {
				if (n<10)
					swissmedicno8_key = s + String.valueOf(n).format("00%d", n);
				else if (n<100)
					swissmedicno8_key = s + String.valueOf(n).format("0%d", n);
				else
					swissmedicno8_key = s + String.valueOf(n).format("%d", n);
				// Check if swissmedicno8_key is a key of the map
				if (pack_info.containsKey(swissmedicno8_key)) {
					ArrayList<String> pi_row = package_info.get(swissmedicno8_key);
					if (pi_row!=null) {
						// --> Add "ausser Handel" information
						String withdrawn_str = "";
						if (pi_row.get(10).length()>0)
							withdrawn_str = ", " + pi_row.get(10);		
						// --> Add public price information
						if (pi_row.get(7).length()>0) {										
							// Remove double spaces in title
							String medtitle = capitalizeFully(pi_row.get(1).replaceAll("\\s+", " "), 1);							
							// Remove [QAP?] -> not an easy one!
							medtitle = medtitle.replaceAll("\\[(.*?)\\?\\] ", "");		
							pinfo_str.add("<p class=\"spacing1\">" + medtitle + ", " + pi_row.get(7) + withdrawn_str 
									+ " [" + pi_row.get(5) + pi_row.get(11) + pi_row.get(12) + flagsb_str + orggen_str + "]</p>");
						} else {
							// Remove double spaces in title					
							String medtitle = capitalizeFully(pi_row.get(1).replaceAll("\\s+", " "), 1);
							// Remove [QAP?] -> not an easy one!							
							medtitle = medtitle.replaceAll("\\[(.*?)\\?\\] ", "");														
							if (DB_LANGUAGE.equals("de")) {
								pinfo_str.add("<p class=\"spacing1\">" + medtitle + ", " + "k.A." + withdrawn_str 
									+ " [" + pi_row.get(5) + pi_row.get(11) + pi_row.get(12) + flagsb_str + orggen_str + "]</p>");
							} else if (DB_LANGUAGE.equals("fr")) {
								pinfo_str.add("<p class=\"spacing1\">" + medtitle + ", " + "prix n.s." + withdrawn_str 
										+ " [" + pi_row.get(5) + pi_row.get(11) + pi_row.get(12) + flagsb_str + orggen_str + "]</p>");
							}
						}
						// --> Add "tindex_str" and "application_str" (see SqlDatabase.java)
						if (index==0) {
							tIndex_list.add(pi_row.get(9));	// therapeutic index
							tIndex_list.add(pi_row.get(6));	// application area						
							index++;
						}
					}
				}
			}
		}
		// In case the pinfo_str is empty due to malformed XML
		/*
		if (pinfo_str.isEmpty())
			html_utils.extractPackSection();
		*/
		// In case nothing was found
		if (index==0) {
			tIndex_list.add("");
			tIndex_list.add("");					
		}
		// Replace original package information with pinfo_str
		String p_str = "";
		mPackSection_str = "";
		for (String p : pinfo_str) {
			p_str += p;
		}

		// Generate a html-deprived string file
		mPackSection_str = p_str.replaceAll("\\<p.*?\\>", "");
		mPackSection_str = mPackSection_str.replaceAll("<\\/p\\>", "\n");
		// Remove last \n
		if (mPackSection_str.length()>0)
			mPackSection_str = mPackSection_str.substring(0, mPackSection_str.length()-1);
		
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		Element div7800 = doc.select("[id=Section7800]").first();	
		if (div7800!=null) {
			div7800.html("<div class=\"absTitle\">Packungen</div>" + p_str);
		} else {
			Element div18 = doc.select("[id=section18]").first();
			if (div18!=null) {
				div18.html("<div class=\"absTitle\">Packungen</div>" + p_str);
			} else {
				if (SHOW_ERRORS)
					System.err.println(">> ERROR: elem is null, sections 18/7800 does not exist: " + title);
			}
		}		
		
		return doc.html();
	}	
	
	static String convertHtmlToXml(String med_title, String html_str, String regnr_str) {				
		Document mDoc = Jsoup.parse(html_str);
		mDoc.outputSettings().escapeMode(EscapeMode.xhtml);
		mDoc.outputSettings().prettyPrint(true);
		mDoc.outputSettings().indentAmount(4);
		
		// <div id="monographie"> -> <fi>
		mDoc.select("div[id=monographie]").tagName("fi").removeAttr("id");
		// <div class="MonTitle"> -> <title>
		mDoc.select("div[class=MonTitle]").tagName("title").removeAttr("class").removeAttr("id");
		// Beautify the title to the best of my possibilities ... still not good enough!
		String title_str = mDoc.select("title").text().trim().replaceAll("<br />","").replaceAll("(\\t|\\r?\\n)+","");
		if (!title_str.equals(med_title))
			if (SHOW_ERRORS)
				System.err.println(med_title + " differs from " + title_str);
		// Fallback solution: use title from the header AIPS.xml file - the titles look all pretty good!
		mDoc.select("title").first().text(med_title);
		// <div class="ownerCompany"> -> <owner>
		Element owner_elem = mDoc.select("div[class=ownerCompany]").first();
		if (owner_elem!=null) {
			owner_elem.tagName("owner").removeAttr("class");			
			String owner_str = mDoc.select("owner").text();		
			mDoc.select("owner").first().text(owner_str);
		} else {
			mDoc.select("title").after("<owner></owner>");
			if (DB_LANGUAGE.equals("de"))
				mDoc.select("owner").first().text("k.A.");
			else if (DB_LANGUAGE.equals("fr"))
				mDoc.select("owner").first().text("n.s.");				
		}
		
		// <div class="paragraph"> -> <paragraph>
		mDoc.select("div[class=paragraph]").tagName("paragraph").removeAttr("class").removeAttr("id");
		// <div class="absTitle"> -> <paragraphTitle>
		mDoc.select("div[class=absTitle]").tagName("paragraphtitle").removeAttr("class");
		// <div class="untertitle1"> -> <paragraphSubTitle>
		mDoc.select("div[class=untertitle1]").tagName("paragraphsubtitle").removeAttr("class");
		// <div class="untertitle"> -> <paragraphSubTitle>
		mDoc.select("div[class=untertitle]").tagName("paragraphsubtitle").removeAttr("class");		
		// <div class="shortCharacteristic"> -> <characteristic>
		mDoc.select("div[class=shortCharacteristic]").tagName("characteristic").removeAttr("class");
		// <div class="image">
		mDoc.select("div[class=image]").tagName("image").removeAttr("class");
		
		// <p class="spacing1"> -> <p> / <p class="noSpacing"> -> <p>
		mDoc.select("p[class]").tagName("p").removeAttr("class");
		// <span style="font-style:italic"> -> <i>
		mDoc.select("span").tagName("i").removeAttr("style");
		// <i class="indention1"> -> <i> / <i class="indention2"> -> <b-i> 
		mDoc.select("i[class=indention1]").tagName("i").removeAttr("class");
		mDoc.select("i[class=indention2]").tagName("i").removeAttr("class");
		// mDoc.select("p").select("i").tagName("i");
		// mDoc.select("paragraphtitle").select("i").tagName("para-i");
		// mDoc.select("paragraphsubtitle").select("i").tagName("parasub-i");
		Elements elems = mDoc.select("paragraphtitle");
		for (Element e : elems) {
			if (!e.text().isEmpty())
				e.text(e.text());
		}
		elems = mDoc.select("paragraphsubtitle");
		for (Element e : elems) {
			if (!e.text().isEmpty())
				e.text(e.text());
		}
		
		// Here we take care of tables
		// <table class="s21"> -> <table>
		mDoc.select("table[class]").removeAttr("class");
		mDoc.select("table").removeAttr("cellspacing").removeAttr("cellpadding").removeAttr("border");
		mDoc.select("colgroup").remove();
		mDoc.select("td").removeAttr("class").removeAttr("colspan").removeAttr("rowspan");
		mDoc.select("tr").removeAttr("class");
		elems = mDoc.select("div[class]");
		for (Element e : elems) {
			if (e.text().isEmpty())
				e.remove();
		}

		mDoc.select("tbody").unwrap();
		// Remove nested table (a nasty table-in-a-table
		Elements nested_table = mDoc.select("table").select("tr").select("td").select("table");
		if (!nested_table.isEmpty()) {
			nested_table.select("table").unwrap();
		}

		// Here we take care of the images
		mDoc.select("img").removeAttr("style").removeAttr("align").removeAttr("border");
		
		// Subs and sups
		mDoc.select("sub[class]").tagName("sub").removeAttr("class");
		mDoc.select("sup[class]").tagName("sup").removeAttr("class");
		mDoc.select("td").select("sub").tagName("td-sub");		
		mDoc.select("td").select("sup").tagName("td-sup");
		// Remove floating <td-sup> tags
		mDoc.select("p").select("td-sup").tagName("sup");		
		mDoc.select("p").select("td-sub").tagName("sub");
		
		// Box
		mDoc.select("div[class=box]").tagName("box").removeAttr("class");
		
		// Insert swissmedicno5 after <owner> tag
		mDoc.select("owner").after("<swissmedicno5></swissmedicno5");
		mDoc.select("swissmedicno5").first().text(regnr_str);
		
		// Remove html, head and body tags			
		String xml_str = mDoc.select("body").first().html();
				
		//xml_str = xml_str.replaceAll("<tbody>", "").replaceAll("</tbody>", "");
		xml_str = xml_str.replaceAll("<sup> </sup>", "");
		xml_str = xml_str.replaceAll("<sub> </sub>", "");		
		xml_str = xml_str.replaceAll("<p> <i>", "<p><i>");
		xml_str = xml_str.replaceAll("</p> </td>", "</p></td>");
		xml_str = xml_str.replaceAll("<p> </p>", "<p></p>");  // MUST be improved, the space is not a real space!!
		xml_str = xml_str.replaceAll("·", "- ");
		xml_str = xml_str.replaceAll("<br />", "");
		xml_str = xml_str.replaceAll("(?m)^[ \t]*\r?\n", "");

		// Remove multiple instances of <p></p>
		Scanner scanner = new Scanner(xml_str);
		String new_xml_str = "";
		int counter = 0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.trim().equals("<p></p>")) {
				counter++;
			} else 
				counter = 0;
			if (counter<3)
				new_xml_str += line;
		}	
		scanner.close();

		return new_xml_str;
	}
		
	static String addHeaderToXml(String xml_str) {	
		Document mDoc = Jsoup.parse("<kompendium>\n" + xml_str + "</kompendium>");
		mDoc.outputSettings().escapeMode(EscapeMode.xhtml);
		mDoc.outputSettings().prettyPrint(true);
		mDoc.outputSettings().indentAmount(4);
		
		// Add date
		Date df = new Date();
		String date_str = df.toString();
		mDoc.select("kompendium").first().prependElement("date");
		mDoc.select("date").first().text(date_str);
		// Add language
		mDoc.select("date").after("<lang></lang>");
		if (DB_LANGUAGE.equals("de"))
			mDoc.select("lang").first().text("DE");
		else if (DB_LANGUAGE.equals("fr"))
			mDoc.select("lang").first().text("FR");

		// Fool jsoup.parse which seems to have its own "life" 
		mDoc.select("tbody").unwrap();
		Elements img_elems = mDoc.select("img");
		for (Element img_e : img_elems) {
			if (!img_e.hasAttr("src"))
				img_e.unwrap();
		}
		mDoc.select("img").tagName("image");
		
		String final_xml_str = mDoc.select("kompendium").first().outerHtml();		
		
		return final_xml_str;
	}

	
	static String prettyFormat(String input) {
	    try {
	        Source xmlInput = new StreamSource(new StringReader(input));
	        StringWriter stringWriter = new StringWriter();
	        StreamResult xmlOutput = new StreamResult(stringWriter);
	        Transformer transformer = TransformerFactory.newInstance().newTransformer(); 
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");	        
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	        transformer.transform(xmlInput, xmlOutput);
	        return xmlOutput.getWriter().toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e); // simple exception handling, please review it
	    }
	}
	
	static String capitalizeFully(String s, int N) {
		// Split string
		String[] tokens = s.split("\\s");
		// Capitalize only first word!
		tokens[0] = tokens[0].toUpperCase();		
		// Reassemble string
		String full_s = "";
		if (tokens.length>1) {
			for (int i=0; i<tokens.length-1; i++) {
				full_s += (tokens[i] + " ");
			}
			full_s += tokens[tokens.length-1];
		} else {
			full_s = tokens[0];
		}
		return full_s;
	}
	
	static String readFromFile(String filename) {
		String file_str = "";		
        try {
        	FileInputStream fis = new FileInputStream(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                file_str += (line + "\n");
            }
            br.close();
        }
        catch (Exception e) {
        	System.err.println(">> Error in reading file");        	
        }
        
		return file_str;	
	}
	
	static void writeToFile(String string_to_write, String dir_name, String file_name) {
        try {
        	File wdir = new File(dir_name);
        	if (!wdir.exists())
        		wdir.mkdirs();
			File wfile = new File(dir_name+file_name);
			if (!wfile.exists())
				wfile.createNewFile();
			// FileWriter fw = new FileWriter(wfile.getAbsoluteFile());
        	CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        	encoder.onMalformedInput(CodingErrorAction.REPORT);
        	encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(wfile.getAbsoluteFile()), encoder);
			BufferedWriter bw = new BufferedWriter(osw);      			
			bw.write(string_to_write);
			bw.close();
 		} catch (IOException e) {
			e.printStackTrace();
 		}		
	}	
	
	static void zipToFile(String dir_name, String file_name) {
		byte[] buffer = new byte[1024];
		
		try {
			FileOutputStream fos = new FileOutputStream(dir_name + changeExtension(file_name, "zip"));
			ZipOutputStream zos = new ZipOutputStream(fos);
			ZipEntry ze = new ZipEntry(file_name);
			zos.putNextEntry(ze);
			FileInputStream in = new FileInputStream(dir_name + file_name);

			int len = 0;
			while ((len = in.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
			in.close();
			zos.closeEntry();
			zos.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	static String changeExtension(String orig_name, String new_extension) {
		int last_dot = orig_name.lastIndexOf(".");
		if (last_dot!=-1)
			return orig_name.substring(0, last_dot) + "." + new_extension;
		else
			return orig_name + "." + new_extension;
	}
	
	static class MyErrorHandler implements ErrorHandler {
		
		public void warning(SAXParseException exception) throws SAXException {
	        System.out.println("\nWARNING");
	        exception.printStackTrace();
	    }
	 
	    public void error(SAXParseException exception) throws SAXException {
	        System.out.println("\nERROR");
	        exception.printStackTrace();
	    }
	 
	    public void fatalError(SAXParseException exception) throws SAXException {
	        System.out.println("\nFATAL ERROR");
	        exception.printStackTrace();
	    }
	}	
}
