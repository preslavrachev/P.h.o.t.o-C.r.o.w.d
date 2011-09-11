package controllers;

import java.util.List;

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
    	WSRequest req = WS.url("http://t.co/imJQKe4");
    	HttpResponse res = req.get();
    	String key = res.getHeader("Location");
    	Logger.debug("location header:"+key);
    	List<Header> headers = res.getHeaders();
    	for (Header header : headers) {
			Logger.debug("header:"+header.name +":"+ header.value());
		}
    	
    	
    }
}
