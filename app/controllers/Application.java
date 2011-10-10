package controllers;

import java.util.List;

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
    
    public static void newtest(){
    	WSRequest req = WS.url("http://192.168.1.100/~aviandri/tweet_data.json");
    	HttpResponse res = req.get();
    	JsonElement element = res.getJson();
    	String url = element.getAsJsonObject().get("entities").getAsJsonObject().get("urls").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
    	Logger.debug("the url found is %s", url);
    }
}
