package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Controller;
import play.mvc.Http.Header;
import controllers.Secure.Security;


public class Application extends Controller {

    public static void index() {
		if(Security.isConnected()){
			Galleries.index();
		}
    	render();
    }
    
    public static void test(){
    	WSRequest req = WS.url("http://t.co/nTwHeSMt").followRedirects(false);
    	HttpResponse res = req.get();
    	Logger.debug("test:"+res.getString());
    	Logger.debug("location : %1s", res.getHeader("Location"));
    	List<Header> list = res.getHeaders();
    	for (Header header : list) {
			String name = header.name;
			String value = header.value();
			Logger.debug("keyname: %1s value %2s", name, value);
		}
    }
    
    public static void json(){
    	Map<String, Object> photos = new HashMap<String, Object>();
    	List<Map<String, String>> photosList = new ArrayList<Map<String,String>>();
    	Map<String, String> photo1 = new HashMap<String, String>();
    	photo1.put("url", "images/photos/opening-ceremony-penfield-fall-2009-00.jpg");
    	photo1.put("poster", "aviandri");
    	photo1.put("text", "Deploy then launch.. Makan2 jgn lupa.. RT: andriavi Kereen RT @uudashr: Done refactoring... pass all test");
    	
    	Map<String, String> photo2 = new HashMap<String, String>();
    	photo2.put("url", "images/photos/opening-ceremony-penfield-fall-2009-00.jpg");
    	photo2.put("poster", "aviandri");
    	photo2.put("text", "Deploy then launch.. Makan2 jgn lupa.. RT: andriavi Kereen RT @uudashr: Done refactoring... pass all test");
    	
    	photosList.add(photo1);
    	photosList.add(photo2);
    	
    	photos.put("photos", photosList);
    	
    }
}
