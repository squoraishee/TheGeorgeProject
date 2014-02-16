import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class DataExtract {

	long count = 0;
	
	Thread dataExtractionThread;
	String currency1;
	String currency2;
	JSONObject lastData;
	ArrayList<JSONObject> dataList;
	
	//Currency List
	//String [] currencyList = {"ltc","bqc","btb","buk","cdc","cmc","cnc","dgc","doge","dtc","exc","frc","max","mec","mmc","nec","nmc","nxt","ppc","pts","qrk","src","tag","yac","vtc","wdc","xpm","zcc","zet"};
	String [] currencyList = {"ltc","bqc","btb","buk"};
	
	//Test some file stream stuff
	FileOutputStream out;
	
	
	public static void main(String args[]) throws Exception {
		
		int interval = 0;
		String CSVName = "";
		
		//make assumptions about the arguments
		switch(args.length) {
		
			case 0 : {
				interval = 1000;
				CSVName = "data.csv";
				break;
			}
			case 1 : {
				interval = Integer.parseInt(args[0]);
				CSVName = "data.csv";
				break;
			}
			case 2 : {
				interval = Integer.parseInt(args[1]);
				CSVName = args[2];
				break;
			}

		}
		
		//remmeber you can construct a non static reference of the class witing the clas sbut htis is th eonly non static referecne
		DataExtract http = new DataExtract(interval,CSVName);
		
		http.start();
		//http.sendGet("btc","cny");
		
	}
	
	DataExtract(final int interval,String fileName) {
		
		//init data array
		dataList = new ArrayList<JSONObject>();
		
		//output file stream for appending data
				try {
					out = new FileOutputStream(fileName);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		//repeat data extraction over and over
		dataExtractionThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				//get the current formatted time stamp
				DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
				Date date = new Date();
				
				String dataColumn = "Timestamp,";
				
				//add initital table
				for (String cur : currencyList) {
					dataColumn += cur + ",";
				}
				
				addDataToCSV(dataColumn);

				
				while(true){ 
					try {
						Thread.sleep(interval);
						dataColumn = dateFormat.format(date) + ",";	//we use the dat format object to convert the date arument into the resultant string forma
						for (String cur : currencyList) {
							dataColumn += obtainPrice(sendGet(cur,"btc"));
						}
						addDataToCSV(dataColumn);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						System.out.println("Connection refused...");
						e.printStackTrace();
					}
				}
			}
			
		});
		
		
		
	}
	
	void start() {
		dataExtractionThread.start();
	}
	
	// HTTP GET request
		private String sendGet(String currency1, String currency2) throws Exception {
	 
			String url = "http://data.bter.com/api/1/ticker/" + currency1 + "_" + currency2;
			//url = "http://www.google.com";
			
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			// optional default is GET
			con.setRequestMethod("GET");
	 
			//add request header
			con.setRequestProperty("User-Agent", "CHROME");
	 
			int responseCode = con.getResponseCode();
			//System.out.println("\nSending 'GET' request to URL : " + url);
			//System.out.println("Response Code : " + responseCode);
	 
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			String result = response.toString();
			//print result
			System.out.println("read #" + count + result);
			
			//increase read count
			count++;
			return result; //will return all the relavent data to put into a table
		}
		
		public String obtainPrice(String result) {
			
			
			JSONObject fullData;
			try {
				fullData = new JSONObject(result);
				//return (String) fullData.get("buy");
				return (Object)fullData.get("buy") + ",";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "Error" + ",";
		}
		
		//add the data row to the CSV file
		void addDataToCSV(String result) {
			for(char c : result.toCharArray()) {
				try {
					out.write(c);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			try {
				out.write('\n');
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
}
