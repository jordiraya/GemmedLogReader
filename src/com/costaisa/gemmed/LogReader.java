package com.costaisa.gemmed;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogReader {
	
	private static final String VERSION = "1.3";
	private static final boolean CLI_MODE = true;
	
	private static final String DEFAULT_MODE = "1";
	private static final String DEFAULT_DIR = "C:\\gemmed\\desconegut_GMED-HUB-1";
	private static final String DEFAULT_MINDATE = "2023-10-01";
	private static final String DEFAULT_MAXDATE = "2023-11-30";
	private static final String DEFAULT_PATH_CSVFILE = "C:\\gemmed\\00_analisis\\desconegut_GMED-HUB-1_" + DEFAULT_MINDATE + "_" + DEFAULT_MAXDATE + ".csv";
	
	private static final String REGEX_ECG = "Request WL with AE = (\\S*)";		
	private static final Pattern pattern = Pattern.compile(REGEX_ECG);
	
	private static final String EXGEX_ECG_VALID = "^[a-zA-Z0-9_]*$";
	private static final Pattern patternExgValid = Pattern.compile(EXGEX_ECG_VALID);
	
	
	public static void main(String[] args) {		
		String mode = null;
		String dir = null;
		String minDate = null;
		String maxDate = null;
		String pathCSVFile = null;
		
		if (CLI_MODE) {
			if (args != null && args.length == 4) {
				mode = args[0];
				dir = args[1];
				minDate = args[2];
				maxDate = args[3];				
			} else if (args != null && args.length == 5) {
				mode = args[0];
				dir = args[1];
				minDate = args[2];
				maxDate = args[3];
				pathCSVFile = args[4];
			} else {
				System.out.println("usage: java LogReader <mode> <pathLogs> <init date yyyy-mm-dd> <end date yyyy-mm-dd> [pathCSVExport]");
				System.out.println("mode 1: lists ECGs, only names");
				System.out.println("mode 2: lists ECGs and number of connections for each ECG");
				System.out.println("mode 3: writes CSV file with ECGs, date and number of connection for each ECG and date, requires pathCSVExport");
				return;				
			}	
		} else {
			mode = DEFAULT_MODE;
			dir = DEFAULT_DIR;
			minDate = DEFAULT_MINDATE;
			maxDate = DEFAULT_MAXDATE;
			pathCSVFile = DEFAULT_PATH_CSVFILE;			
		}
		
		System.out.println("Gemmed log reader v." + VERSION + " - reading files in " + dir);
		if (minDate != null && maxDate != null) {
			System.out.println("between " + minDate + " and " + maxDate);
		}
		
		LogReader logReader = new LogReader();
		
		try {
			switch (mode) {
			case "1":
				System.out.println("mode 1 (list ECGs)");
				logReader.listEcgs(dir, minDate, maxDate);
				break;
			case "2":
				System.out.println("mode 2 (list ECGs with occurrences)");
				logReader.listEcgsWithCount(dir, minDate, maxDate);
				break;
			case "3":
				System.out.println("mode 3 (export ECGs with dates and occurrences to CSV file)");
				logReader.exportEcgsWithDatesAndCounts(dir, minDate, maxDate, Paths.get(pathCSVFile));
				break;
			default:
				System.out.println("unrecognized mode " + mode);				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public void listEcgs(String dir, String minDate, String maxDate) throws IOException {	
		Set<String> ecgs = getEcgs(dir, minDate, maxDate);			
		System.out.println("ecgs found: " + ecgs.size());
		System.out.println("--------------");
		
		for(String ecg : ecgs) {
			System.out.println(ecg);
		}
	}
	
	public void listEcgsWithCount(String dir, String minDate, String maxDate) throws IOException {
		Map<String, Integer> ecgsWithCount = getEcgsWithCount(dir, minDate, maxDate);
		System.out.println("ecgs found: " + ecgsWithCount.size());
		System.out.println("--------------");			
		
		Set<String> ecgs = ecgsWithCount.keySet();
		for(String ecg : ecgs) {
			System.out.println(ecg + " [" + ecgsWithCount.get(ecg) + "]");
		}
	}
	
	public void exportEcgsWithDatesAndCounts(String dir, String minDate, String maxDate, Path pathFile) throws IOException {
		Map<String, Integer> ecgsWithDatesOccurs = getEcgsWithDateAndCount(dir, minDate, maxDate);
		Set<String> ecgsDatesOccurs = ecgsWithDatesOccurs.keySet();
		ArrayList<String> lines = new ArrayList<>();
		
		for(String ecgDateOccurs : ecgsDatesOccurs) {
			String ecg = ecgDateOccurs.substring(0, ecgDateOccurs.lastIndexOf('_'));
			String date = ecgDateOccurs.substring(ecgDateOccurs.lastIndexOf('_') + 1);
			Integer occurs = ecgsWithDatesOccurs.get(ecgDateOccurs);
			lines.add(ecg + "," + date + "," + occurs);
			// System.out.println(ecg + "," + date + "," + occurs);
		}
		
		System.out.println("--------------");
		System.out.println("exporting data to file " + pathFile);
		Files.write(pathFile, lines);
		System.out.println("finished");	
	}

	private Set<String> getEcgs(String dir, String minDate, String maxDate) throws IOException {
		Set<String> ecgs = new TreeSet<>(); // ordered set
		
		Set<Path> pathsFiles = getPathsFiles(dir, minDate, maxDate);
		System.out.println("logfiles found: " + pathsFiles.size());
		
		for (Path pathFile : pathsFiles) {
			ecgs.addAll(readLogFile(pathFile));
		}		
		return ecgs;		
	}	
	
	private Map<String, Integer> getEcgsWithCount(String dir, String minDate, String maxDate) throws IOException {
		Map<String, Integer> ecgs = new TreeMap<>(); // ordered Map
		
		Set<Path> pathsFiles = getPathsFiles(dir, minDate, maxDate);
		System.out.println("logfiles found: " + pathsFiles.size());
		
		for (Path pathFile : pathsFiles) {
			readLogFileWithCount(pathFile, ecgs);
		}		
		return ecgs;		
	}
	
	private Map<String, Integer> getEcgsWithDateAndCount(String dir, String minDate, String maxDate) throws IOException {
		Map<String, Integer> ecgsDatesOccurs = new TreeMap<>(); // ordered Map
		
		Set<Path> pathsFiles = getPathsFiles(dir, minDate, maxDate);
		System.out.println("logfiles found: " + pathsFiles.size());
		
		for (Path pathFile : pathsFiles) {
			readLogFileWithDateAndCount(pathFile, ecgsDatesOccurs);
		}
		return ecgsDatesOccurs;
	}
	
	
	private Set<Path> getPathsFiles(String dir, String minDate, String maxDate) throws IOException {
		if (minDate == null || maxDate == null) {
		    return Files.list(Paths.get(dir))
		    		.filter(file -> !Files.isDirectory(file))
		    		.collect(Collectors.toSet());			
		}
	    return Files.list(Paths.get(dir))
	    		.filter(file -> !Files.isDirectory(file))
	    		.map(Path::getFileName)
	    		.map(Path::toString)
	    		.filter(file -> file.substring(file.lastIndexOf('.') + 1).compareTo(minDate) >= 0)
	    		.filter(file -> file.substring(file.lastIndexOf('.') + 1).compareTo(maxDate) <= 0)
	    		.map(file -> Paths.get(dir + File.separator + file))
	    		.collect(Collectors.toSet());	
	}

	
	private Set<String> readLogFile(Path pathFile) throws IOException {		
		Set<String> ecgs = new HashSet<>();
		
		String logContents = Files.readAllLines(pathFile, StandardCharsets.ISO_8859_1).stream()
				.map(str -> str + "\n")
				.collect(Collectors.joining());			
		Matcher matcher = pattern.matcher(logContents);
		
		while (matcher.find()) {
			String ecgName = matcher.group(1); // here it is the ECG name
			if (!isEcgValid(ecgName)) {
				continue;
			}
			ecgs.add(ecgName);
		}
		return ecgs;
	}	
	
	private void readLogFileWithCount(Path pathFile, Map<String, Integer> ecgs) throws IOException {		
		String logContents = Files.readAllLines(pathFile, StandardCharsets.ISO_8859_1).stream()
				.map(str -> str + "\n")
				.collect(Collectors.joining());			
		Matcher matcher = pattern.matcher(logContents);
		
		while (matcher.find()) {
			String ecgName = matcher.group(1); // here it is the ECG name
			if (!isEcgValid(ecgName)) {
				continue;
			}	
			if (ecgs.containsKey(ecgName)) {
				int newOccurrences = ecgs.get(ecgName) + 1;
				ecgs.replace(ecgName, newOccurrences);
			} else {
				ecgs.put(ecgName, 1);
			}
		}
	}
	
	private void readLogFileWithDateAndCount(Path pathFile, Map<String, Integer> ecgsDatesOccurs) throws IOException {				
		String date = pathFile.getFileName().toString().substring( pathFile.getFileName().toString().lastIndexOf('.') + 1 );
		
		String logContents = Files.readAllLines(pathFile, StandardCharsets.ISO_8859_1).stream()
				.map(str -> str + "\n")
				.collect(Collectors.joining());			
		Matcher matcher = pattern.matcher(logContents);
		
		while (matcher.find()) {			
			String ecgName = matcher.group(1); // here it is the ECG name
			if (!isEcgValid(ecgName)) {
				continue;
			}			
			String ecgNameDate = ecgName + "_" + date;					
			if (ecgsDatesOccurs.containsKey(ecgNameDate)) {				
				int newOccurrences = ecgsDatesOccurs.get(ecgNameDate) + 1;
				ecgsDatesOccurs.replace(ecgNameDate, newOccurrences);				
			} else {
				ecgsDatesOccurs.put(ecgNameDate, 1);
			}
		}
	}
	
	private boolean isEcgValid(String ecgName) {
		if (ecgName == null) {
			return false;
		}
		String ecgTrimmed = ecgName.trim();		
		if (ecgTrimmed.length() <= 3) {
			return false;
		}
		Matcher matcher = patternExgValid.matcher(ecgTrimmed);
		if (!matcher.matches()) {
			return false;
		}		
		return true;
	} 
	
}
