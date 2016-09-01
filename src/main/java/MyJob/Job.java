package MyJob;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

@SuppressWarnings("deprecation")
public class Job {
	
  
	public String execute(String param) throws ClientProtocolException, IOException {
		
		String str="";
		HttpClient httpclient = new DefaultHttpClient();  
		HttpGet gets=new HttpGet("http://www.baidu.com");
		HttpResponse response=httpclient.execute(gets);
		HttpEntity entity=response.getEntity();
		if(entity!=null){
			InputStream instreams = entity.getContent();    
            str = convertStreamToString(instreams);  
		}
		return str;
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
