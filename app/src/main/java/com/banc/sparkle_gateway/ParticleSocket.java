package com.banc.sparkle_gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class ParticleSocket {
	//live server
	String cloudServer = "54.208.229.4";
	//staging server
//	String cloudServer = "staging-device.spark.io";
	//local server
//	String cloudServer = "10.1.10.175";
	int cloudPort = 5683;

	private int CloudServiceID = 1;

	Socket socket;
	InputStream inputStream;
	OutputStream outputStream;
	
	public ParticleSocket() {
		
	}
	
	public void Connect() throws UnknownHostException, IOException {
		
		class Retrievedata extends AsyncTask<String, Void, String> {
			@Override
			    protected String doInBackground(String... params) {
			         try{
			        	 Log.d("SparkleCloudInterface", "Connecting to Spark Cloud");
			        	 InetAddress serveraddress=InetAddress.getByName(cloudServer);
			        	 Log.d("ParticleSocket", "Got Here!");
			        	 Log.d("SparkleCloudInterface", "We should have the IP Address " + serveraddress);
			        	 socket = new Socket(cloudServer, cloudPort);
			        	 Log.d("SparkleCloudInterface", "Did we connect?");
			        	 Log.d("SparkleCloudInterface", Boolean.toString(socket.isConnected()));
			        	 inputStream = socket.getInputStream();
			        	 outputStream = socket.getOutputStream();    
			         }
			         catch (Exception e)
			         {
			        	 e.printStackTrace();
			         }
			         return null;
			    }
			}
		String params = "";
		new Retrievedata().execute(params);
	}
	public void Disconnect() throws UnknownHostException, IOException {
		inputStream.close();
		outputStream.close();
		socket.close();
		inputStream = null;
		outputStream = null;
		socket = null;
	}
	public void Write(byte[] data) throws IOException {
//		Log.d("SparkleCloudInterface", "When writing, are we connected: " + Boolean.toString(socket.isConnected()));
		outputStream.write(data);
		outputStream.flush();
		StringBuilder sb = new StringBuilder();

	    for (byte b : data) {
	        sb.append(String.format("%02X ", b));
	    }
//	    Log.d("SparkleCloudInterface", "Sending data: " + sb.toString());
		Log.d("SparkleCloudInterface", "Sending data to Cloud of size: " + data.length);
	}
	public int Available() throws IOException {
		if (inputStream != null) {
			return inputStream.available();
		}
		return 0;
	}
	public boolean Connected() {
		if (inputStream != null) {
			return socket.isConnected();
		} else {
			return false;
		}
	}
	public byte[] Read() throws IOException {
		//need to append our service ID and socket number to the beginning of the data
		byte[] data = new byte[0];
		if (inputStream != null) {
			int bytesAvailable = inputStream.available();

			try {
				Thread.sleep(500);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			bytesAvailable = inputStream.available();
			data = new byte[bytesAvailable];

//		//copy the cloud data in with the service ID and socket number
			inputStream.read(data, 0, bytesAvailable);


//		byte[] data = new byte[inputStream.available()];
//		inputStream.read(data, 0, inputStream.available());

			StringBuilder sb = new StringBuilder();
			for (byte b : data) {
				sb.append(String.format("%02X ", b));
			}
			Log.d("SparkleCloudInterface", "Got data: " + sb.toString());
			Log.d("SparkleCloudInterface", "Got data from Cloud of size: " + data.length);
		}

//	    System.out.println(sb.toString());
		return data;
	}
}
