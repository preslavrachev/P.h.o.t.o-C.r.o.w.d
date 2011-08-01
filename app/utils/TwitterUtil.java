package utils;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TwitterUtil {
	public static final String BaseSearchUrl = "http://search.twitter.com/search.json";
	public static final int DefaultRpp = 100;
	
    public static SearchResponse searchTwitter(String q, Long sinceId) {
        String callUrl = "http://search.twitter.com/search.json?q=" + q
                + "&since_id=" + sinceId + "&rpp=100";
        Logger.info("Call twitter url:" + callUrl);
        HttpResponse resp = WS.url(callUrl).get();
        JsonObject jsonObj = resp.getJson().getAsJsonObject();
        if (resp.getStatus() != 200) {
            String errorMesssage = jsonObj.get("error").getAsString();
            Logger.error("Got non 200 HTTP Status code: %1s error=%2s", resp.getStatus(), errorMesssage);
        }
        return new SearchResponse(jsonObj);
    }
    
    public static SearchResponse searchTwitterLatest(String q, Long sinceId) {
        String queryString = q + "&since_id=" + sinceId;
        return searchTwitter(queryString);
        						
    }
    
    public static SearchResponse searchTwitterNext(String q, Long maxId, int page) {
    	String queryString;
    	if(maxId == 0){
    		queryString = "q=" + q + "&page=" + page;
    	}else{
    		queryString = "q=" + q + "&max_id=" + maxId + "&page=" + page;
    	}
    	
    	return searchTwitter(queryString);
    }
    
    public static SearchResponse searchTwitter(String queryString){
    	String callUrl = BaseSearchUrl.concat("?"+queryString).concat("&rpp="+DefaultRpp);
    	Logger.info("Search to twitter : %1s", callUrl);
    	HttpResponse resp = WS.url(callUrl).get();
        JsonObject jsonObj = resp.getJson().getAsJsonObject();
        if (resp.getStatus() != 200) {
            String errorMesssage = jsonObj.get("error").getAsString();
            Logger.error("Got non 200 HTTP Status code: %1s error=%2s", resp.getStatus(), errorMesssage);
        }
        return new SearchResponse(jsonObj);
    }
}
