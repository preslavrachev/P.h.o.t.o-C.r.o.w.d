package utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import controllers.Secure;

/**
 * @author uudashr@gmail.com
 *
 */
public class Twitter {
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    
    /**
     * A method to get a single tweet status
     * @param tweetId
     * @return
     */
    public static TwitterStatus getStatus(long tweetId){
    	WSRequest req = WS.url("http://api.twitter.com/1/statuses/show/"+tweetId+".json?include_entities=true");
    	HttpResponse res = req.get();
    	JsonObject jsonObj = res.getJson().getAsJsonObject();
    	if(res.getStatus()!=200){
    		String errorMessage = jsonObj.get("error").getAsString();
            String message = String.format("Found non 200 HTTP status code: statusCode = %1s; %2s", res.getStatus(), errorMessage);
            Logger.error(message);
            throw new QueryExecutionException(res.getStatus(), message);
    	}
    	return new TwitterStatus(jsonObj);
    }
    
    /**
     * A method to get a single tweet status
     * @param tweetId
     * @return
     */
    public static TwitterUser getUserDetails(long twitterUserId){
    	WSRequest req = WS.url("http://api.twitter.com/1/users/show.json");
    	req.setParameter("user_id", twitterUserId);
    	HttpResponse res = req.get();
    	JsonObject jsonObj = res.getJson().getAsJsonObject();
    	if(res.getStatus()!=200){
    		String errorMessage = jsonObj.get("error").getAsString();
            String message = String.format("Found non 200 HTTP status code: statusCode = %1s; %2s", res.getStatus(), errorMessage);
            Logger.error(message);
            throw new QueryExecutionException(res.getStatus(), message);
    	}
    	return new TwitterUser(jsonObj);
    }
    
    public static String getProfileImageMiniUrl(String screenName){
		return retrieveProfileImageUrl(screenName, "mini");
	}
	public static String getProfileImageBiggerUrl(String screenName){
		return retrieveProfileImageUrl(screenName, "bigger");
	}
	public static String getProfileImageOriginalUrl(String screenName){
		return retrieveProfileImageUrl(screenName, "original");
	}
	
    public static String retrieveProfileImageUrl(String username, String size) {
        HttpResponse resp = WS.url("http://api.twitter.com/1/users/profile_image").setParameter("screen_name", username).setParameter("size", size).followRedirects(false).get();
        if (resp.getStatus() == 302) {
            return resp.getHeader("location");
        }
        return null;
    }
    
    
    public static TwitterQuery query(String query) {
        return new TwitterQuery(query);
    }
    
    public static class TwitterQuery {
        private String query;
        private Long sinceId;
        private Long maxId;
        private Integer page;
        private Integer rpp;
        
        public TwitterQuery(String query) {
            this.query = query;
        }
        
        public TwitterQuery sinceId(Integer sinceId) {
            this.sinceId = sinceId.longValue();
            return this;
        }
        
        public TwitterQuery sinceId(Long sinceId) {
            this.sinceId = sinceId;
            return this;
        }
        
        public TwitterQuery maxId(Long maxId) {
            this.maxId = maxId;
            return this;
        }
        
        public TwitterQuery page(Integer page) {
            this.page = page;
            return this;
        }
        
        public TwitterQuery rpp(Integer rpp) {
            this.rpp = rpp;
            return this;
        }

        public QueryResult execute() throws QueryExecutionException {
            Logger.debug("Query %1s", query);
            WSRequest req = WS.url("http://search.twitter.com/search.json").setParameter("q", query);
            
            if (sinceId != null) {
                req.setParameter("since_id", sinceId);
            }
            
            if (maxId != null) {
                req.setParameter("max_id", maxId);
            }
            
            if (page != null) {
                req.setParameter("page", page);
            }
            
            if (rpp != null) {
                req.setParameter("rpp", rpp);
            }
            Logger.debug("requesting to %1s", req.url + " params " + req.parameters);
            HttpResponse resp = req.get();
            JsonObject jsonObj = resp.getJson().getAsJsonObject();
            if (resp.getStatus() != 200) {
                String errorMessage = jsonObj.get("error").getAsString();
                String message = String.format("Found non 200 HTTP status code: statusCode = %1s; %2s", resp.getStatus(), errorMessage);
                Logger.error(message);
                throw new QueryExecutionException(resp.getStatus(), message);
            }
            Logger.debug("search result %1s", jsonObj.toString());
            return new QueryResult(jsonObj);
        }
    }
    
    /**
     * This is a query builder for twitter search. We can specify such as keyword,
     * date range and location.
     * 
     * @author uudashr@gmail.com
     * 
     */
    public static class QueryBuilder {
        private String keyword;
        private String since;
        private String until;
        private String location;
        
        /**
         * Construct by specifying the keyword.
         * 
         * @param keyword is the keyword.
         */
        public QueryBuilder(String keyword) {
            this.keyword = keyword;
        }

        /**
         * Set since criteria. The format will be yyyy-MM-dd.
         * 
         * @param date is the formatted {@link String} date.
         * return the current builder.
         */
        public QueryBuilder since(String date) {
            since = date;
            return this;
        }

        /**
         * Set since criteria.
         * 
         * @param date is the date.
         * @return the current builder.
         */
        public QueryBuilder since(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            return since(dateFormat.format(date));
        }
        
        /**
         * Set until criteria. The format of <code>date</code> will be yyyy-MM-dd.
         * 
         * @param date is the formatted {@link String} of date.
         * @return the current builder.
         */
        public QueryBuilder until(String date) {
            until = date;
            return this;
        }
        
        /**
         * Set the until criteria.
         * 
         * @param date is the date.
         * @return the current builder.
         */
        public QueryBuilder until(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            return until(dateFormat.format(date));
        }
        
        /**
         * Set near location criteria.
         * 
         * @param location is the location.
         * @return the current builder.
         */
        public QueryBuilder near(String location) {
            // remove the (") character.
            this.location = location.replaceAll("\"", "");
            return this;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(keyword);
            if (location != null) {
                builder.append(" near:");
                if (location.contains(" ")) {
                    builder.append("\"").append(location).append("\"");
                } else {
                    builder.append(location);
                }
            }
            
            if (since != null) {
                builder.append(" since:").append(since);
            }
            
            if (until != null) {
                builder.append(" until:").append(until);
            }
            
            return builder.toString();
        }
    }
    
    /**
     * This is a wrapper of Twitter search response.
     * 
     * @author uudashr@gmail.com
     *
     */
    public static class QueryResult {
        private JsonObject jsonObj;
        
        public QueryResult(JsonObject jsonObj) {
            this.jsonObj = jsonObj;
        }

        public JsonArray getTweets() {
            return jsonObj.getAsJsonArray("results");
        }
        
        public long getMaxId() {
            return Long.valueOf(jsonObj.get("max_id_str").getAsString());
        }
        
        public int getPage() {
            return jsonObj.get("page").getAsInt();
        }
        
        public boolean hasNextPage() {
            return jsonObj.get("next_page") != null;
        }
        
        public JsonObject getJsonObject() {
            return jsonObj;
        }
    }
    
    public static class TwitterStatus {
    	private JsonObject jsonObj;
    	
    	public TwitterStatus(JsonObject jsonObj){
    		this.jsonObj = jsonObj;
    	}
    	
    	public List<String> getExpendedUrls(){
    		List<String> urls = new ArrayList<String>();
    		JsonArray jsonArray = jsonObj.getAsJsonObject("entities").getAsJsonArray("urls");
    		for (JsonElement jsonElement : jsonArray) {
				String url = jsonElement.getAsJsonObject().get("expanded_url").getAsString();
				urls.add(url);				
			}
    		return urls;
    	}
    }
    
    public static class TwitterUser {
    	private JsonObject jsonObj;
    	
    	public TwitterUser(JsonObject jsonObj){
    		this.jsonObj = jsonObj;
    	}
    	
    	public String getScreenName(){
    		return jsonObj.get("screen_name").getAsString();
    	}

    }
    
    public static class QueryExecutionException extends RuntimeException {
        private Integer status;
        
        public QueryExecutionException(Integer status, String message) {
            super(message);
            this.status = status;
        }
        
        public Integer getStatus() {
            return status;
        }
    }
}
