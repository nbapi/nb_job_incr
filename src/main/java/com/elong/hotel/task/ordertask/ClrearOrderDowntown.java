package com.elong.hotel.task.ordertask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.elong.hotel.schedule.entity.TaskResult;

@SuppressWarnings("deprecation") 
public class ClrearOrderDowntown {
public TaskResult execute(String param) throws ClientProtocolException, IOException {
		TaskResult r=new TaskResult();
		String dateString="";
		try{  
			    
			Calendar calendar=Calendar.getInstance();  
			calendar.setTime(new Date());
			 
			calendar.set(Calendar.DAY_OF_MONTH,calendar.get(Calendar.DAY_OF_MONTH)-30);//让日期加1  
			//System.out.println(calendar.get(Calendar.DATE));//加1之后的日期Top 
			
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			dateString = format.format(calendar.getTime());
			//Date date=new Date();
			
			 
			HttpClient httpclient = new DefaultHttpClient();  
			HttpGet gets=new HttpGet(String.format("http://%s:9000/REST/com.eLong.Hotel.Order.Services/OrderStatRESTService/Clear?date=%s&count=100", param,dateString));
			HttpResponse response=httpclient.execute(gets);
			HttpEntity entity=response.getEntity();
			if(entity!=null){
				InputStream instreams = entity.getContent();    
	            r.setMessage(convertStreamToString(instreams));  
			}
		}
		catch(Exception e)
		{ 
			String errString="";
			try {
				for(int index = 0; index < e.getStackTrace().length; index++){
	    			errString=errString+e.getStackTrace()[index].toString();
										
				}
	    		if(!e.getMessage().isEmpty()){
	    			errString=e.getMessage()+errString;
	    		}
			} catch (Exception e2) {
				// TODO: handle exception
			}
    		
			r.setCode(-1);
			r.setMessage(errString);
		} 
		
		return r;
	}
	
	 public static String convertStreamToString(InputStream is) {      
	        BufferedReader reader = new BufferedReader(new InputStreamReader(is));      
	        StringBuilder sb = new StringBuilder();      
	       
	        String line = null;      
	        try {      
	            while ((line = reader.readLine()) != null) {  
	                sb.append(line + "\n");      
	            }      
	        } catch (IOException e) {      
	            e.printStackTrace();      
	        } finally {      
	            try {       
	                is.close();      
	            } catch (IOException e) {      
	               e.printStackTrace();      
	            }      
	        }      
	        return sb.toString();      
	    }  
}
