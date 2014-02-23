package com.mobilonix.cryptocur;
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

import org.json.JSONArray;
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
	String [] currencyList = {"ltc","bqc","btb","buk","cdc","cmc","cnc","dgc","doge","dtc","exc","frc","max","mec","mmc","nec","nmc","nxt","ppc","pts","qrk","src","tag","yac","vtc","wdc","xpm","zcc","zet"};
	
	//Test some file stream stuff
	FileOutputStream out;
	
	//Display debug
	boolean displayDebug = true;
	
	//ticker URL
	static String TICKER_API_URL = "http://data.bter.com/api/1/ticker/";
	static String TRADE_API_URL = "http://data.bter.com/api/1/trade/";
	
	//data URL, inter-changeable
	String DATA_URL = "";
	
	//data item to tabulate a list for
	String dataItem = "buy";
	
	//total list of allowed data items
	ArrayList<String> dataItemList;
	
	public static void main(String args[]) throws Exception {

		String item = "buy";
		int interval = 0;
		String CSVName = "";
		
		System.out.println("Collecting currency/btc data. " + args.length + " arguments available...");
		
		//make assumptions about the arguments
		switch(args.length) {
		
			case 0 : {
				interval = 1000;
				CSVName = "data.csv";
				item = "buy";
				break;
			}
			case 1 : {
				interval = 1000; 
				CSVName = "data.csv";
				item = args[0];
				break;
			}
			case 2 : {
				interval = Integer.parseInt(args[1]);
				CSVName = "data.csv";
				item = args[0];
				break;
			}
			case 3 : {
				interval = Integer.parseInt(args[1]);
				CSVName = args[2];
				item = args[0];
				break;
			}

		}
		
		
		System.out.println("Initalizing data extractor...");
		System.out.println("Extracting category: " + item);
		//create self-instance to circumvent static entry point
		DataExtract dataExtractor = new DataExtract(interval,CSVName,item);
		
		//start data extraction process
		dataExtractor.start();
		//http.sendGet("btc","cny");
		//bjjdhsj
	}
	
	//constructor
	public DataExtract(final int interval,String fileName,String item) {
		
		//by default chose the ticker URL
		DATA_URL = TICKER_API_URL;
		
		//get all available data Items
		populateDataItemList();
		
		//init data array
		dataList = new ArrayList<JSONObject>();
		
		//output file stream for appending data
		try {
			
			//volume tables
			out = new FileOutputStream(fileName);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
				
		//repeat data extraction over and over
		dataExtractionThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				//get the current formatted time stamp
				DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
				Date date = new Date();
				
				String dataColumn = "Local Timestamp,";
				
				//add initital table
				for (String cur : currencyList) {
					dataColumn += cur + " Time, " + cur + " Order Id, " + cur + " Volume, " + cur + " BTC Volume, " + cur + " Price, " + cur + " Order Type";
				}
				
				//add item to table
				addDataToCSV(dataColumn);

				//rinse wash repeat
				while(true){ 
					try {
						Thread.sleep(interval);
						date = new Date();
						dataColumn = dateFormat.format(date
								
								) + ",";	//we use the dat format object to convert the date arument into the resultant string forma
						for (String cur : currencyList) {
							
							//combine column data from all relavent URLs here
							String tickerResponse = sendGet(TICKER_API_URL,cur,"btc");
							String tradeResponse = sendGet(TRADE_API_URL,cur,"btc");
							
							//making a BIG assumption AND approximation that the latest trade on both APIs correpsonds to the same data
							dataColumn += obtainMarketTime(tradeResponse);
							dataColumn += obtainOrderId(tradeResponse);
							dataColumn += obtainVolumeNumerator(tickerResponse,cur);
							dataColumn += obtainVolumeDenominator(tickerResponse);
							dataColumn += obtainOrderType(tradeResponse);
							dataColumn += obtainPrice(tradeResponse);
							
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
	
	//will convert to feed read string
	public void populateDataItemList() {
		
		
		/* Example ticker currencey return
		{
			result: "true",
			last: "0.00000210",
			high: "0.00000217",
			low: "0.00000209",
			avg: "0.00000212",
			sell: "0.00000210",
			buy: "0.00000210",
			vol_doge: 158564482.398,
			vol_btc: 336.03439485
		}
		*/
		dataItemList = new ArrayList<String>();
		
		dataItemList.add("last");
		dataItemList.add("high");
		dataItemList.add("low");
		dataItemList.add("avg");
		dataItemList.add("sell");
		dataItemList.add("buy");
		
	}
	
	//test if the Argument is within the bounds off acceptable JSON keys
	public String checkDataItemArgument(String arg){
		
		System.out.println("Checking for a valid argument...");
		
		for(int i = 0; i < dataItemList.size(); i++) {
			if (arg == dataItemList.get(i)) {
				return dataItemList.get(i);
			}
		}
		
		//assume the buy key always exists
		return "buy";
	}
	
	//start execution cycle
	void start() {
		dataExtractionThread.start();
	}
	
		// ***************************************HTTP REQUEST METHODS*****************************************/
		private String sendGet(String dataURL, String currency1, String currency2) throws Exception {
			
			//Construct URL
			String url = dataURL + currency1 + "_" + currency2;
			
			System.out.println("Sending request to: " + url);
			
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			// optional default is GET
			con.setRequestMethod("GET");
	 
			//add request header
			con.setRequestProperty("User-Agent", "CHROME");
	 
			int responseCode = con.getResponseCode();
			//System.out.println("\nSending 'GET' request to URL : " + url);
			
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
		
		/************************************************CATEGORY PARSING METHODS*************************************************/
		
		public String obtainOrderType(String result) {
			
			//in order to get a JSON array you can get it from anywhere in the object no  matter how far it's nested
			
			JSONObject fullData;
			try {
				fullData = new JSONObject(result);
				JSONArray dataArray = fullData.getJSONArray("data");
				
				//get the object at the bottom of the trade API feed (Should be the latest)
				JSONObject category = dataArray.getJSONObject(dataArray.length() - 1);
				
				//return (String) fullData.get("buy");
				return (String)category.get("type") + ",";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "Error" + ",";
		}
		
		public String obtainOrderId(String result) {
			
			//in order to get a JSON array you can get it from anywhere in the object no  matter how far it's nested
			
			JSONObject fullData;
			try {
				fullData = new JSONObject(result);
				JSONArray dataArray = fullData.getJSONArray("data");
				
				//get the object at the bottom of the trade API feed (Should be the latest)
				JSONObject category = dataArray.getJSONObject(dataArray.length() - 1);
				
				//return (String) fullData.get("buy");
				return (String)category.get("tid") + ",";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "Error" + ",";
		}
		
		
		public String obtainVolumeDenominator(String result) {
			
			
			JSONObject fullData;
			try {
				fullData = new JSONObject(result);
				//return (String) fullData.get("buy");
				return (Object)fullData.get("vol_btc") + ",";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "Error" + ",";
		}
		
		public String obtainVolumeNumerator(String result, String cur) {
			
			
			
			JSONObject fullData;
			try {
				fullData = new JSONObject(result);
				//return (String) fullData.get("buy");
				return (Object)fullData.get("vol_" + cur) + ",";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "Error" + ",";
		}
		
		//get last market time.  Note this may not correlate between each API
		public String obtainMarketTime(String result) {
			
			//in order to get a JSON array you can get it from anywhere in the object no  matter how far it's nested
			
			JSONObject fullData;
			try {
				fullData = new JSONObject(result);
				JSONArray dataArray = fullData.getJSONArray("data");
				
				//get the object at the bottom of the trade API feed (Should be the latest)
				JSONObject category = dataArray.getJSONObject(dataArray.length() - 1);
				
				//return (String) fullData.get("buy");
				return (Object)category.get("date") + ",";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "Error" + ",";
		}
		
		public String obtainPrice(String result) {
			
			
			//in order to get a JSON array you can get it from anywhere in the object no  matter how far it's nested
			
			JSONObject fullData;
			try {
				fullData = new JSONObject(result);
				JSONArray dataArray = fullData.getJSONArray("data");
				
				//get the object at the bottom of the trade API feed (Should be the latest)
				JSONObject category = dataArray.getJSONObject(dataArray.length() - 1);
				
				//return (String) fullData.get("buy");
				return (Object)category.get("price") + ",";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "Error" + ",";
		}
		
		/*******************************************AUXILLARY METHODS*************************************************/
		
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
