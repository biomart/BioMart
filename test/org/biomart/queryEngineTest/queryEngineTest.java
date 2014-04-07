package org.biomart.queryEngineTest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;

import org.biomart.common.resources.Log;

public class queryEngineTest {
	public static void main(String args[]){
		if(args.length==0){
			Log.error("Please specify a data file! File should be tab-delimited with the format: URL|FILENAME\tURL\tQUERY\t[QUERY2]");
			System.exit(-1);
		}
		try {
			BufferedReader inputFile = new BufferedReader(new FileReader(args[0]));

			String input;
			int i = 0;
			while((input = inputFile.readLine()) != null){
				++i;
				String[] inputLine = input.split("\t",-1);

				String firstInput = inputLine[0];
				String secondInput = inputLine[1];
				String query1 = URLEncoder.encode(inputLine[2],"UTF-8");
				String query2;
				String name = Integer.toString(i);
				if(inputLine.length>3)
					query2 = URLEncoder.encode(inputLine[3],"UTF-8");
				else
					query2 = query1; 

				HashSet<String> firstResults = null;
				try{
					new URL(firstInput);
					firstResults = readURLData(firstInput + query1);
				} catch (MalformedURLException e){
					try{
						firstResults = readFileData(firstInput);
					} catch (FileNotFoundException f) {
						System.err.println("File " + firstInput + " not found!");
						f.printStackTrace();
					}
				}

				HashSet<String> secondResults = readURLData(secondInput + query2);

				System.out.print(name + "\t");
				if(secondResults.containsAll(firstResults) && firstResults.containsAll(secondResults))
					System.out.println("same");
				else
					System.out.println("different");

			}
			inputFile.close();
		} catch (FileNotFoundException e) {
			System.err.println("File " + args[0] + " not found!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O error");
			e.printStackTrace();
		}

	}

	private static HashSet<String> readURLData(String queryURL) throws IOException{
		URL query = new URL(queryURL);
		BufferedReader in = new BufferedReader(new InputStreamReader(query.openStream()));

		return readData(in);
	}
	
	private static HashSet<String> readFileData(String file) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(file));

		return readData(in);
	}
	
	private static HashSet<String> readData(BufferedReader in) throws IOException{
		HashSet<String> results = new HashSet<String>();

		String inputLine;

		while ((inputLine = in.readLine()) != null){
			if(!inputLine.startsWith("#")){
				results.add(inputLine);
			}
		}

		in.close();

		return results;
	}
}


