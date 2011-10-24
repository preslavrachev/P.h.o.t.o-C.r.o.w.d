package jobs;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import models.Gallery;
import models.Gallery.State;
import models.User;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.templates.JavaExtensions;
import utils.Twitter;
import utils.Twitter.QueryBuilder;
import utils.Twitter.QueryResult;
import utils.photoservice.PhotoServices;
import utils.photoservice.PhotoServices.PhotoResource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This job responsible to take all the tweets related to the gallery and end up
 * with getting the photo URL on the tweet.
 * 
 * @author uudashr@gmail.com
 * 
 */
public class RetrieveGalleryPhotosJob extends Job<Void> {
    private Gallery gallery;

    public RetrieveGalleryPhotosJob(Gallery crowdGallery) {
        this.gallery = crowdGallery;
    }

    @Override
    public void doJob() throws Exception {
        String searchQuery = buildQuery(gallery);
        Logger.debug("Searching '%1s'", searchQuery);
        final int rpp = Integer.parseInt(Play.configuration.getProperty(
                "twitter.search.rpp", "15"));
        if (gallery.state == State.NEW) {
        	Logger.debug("entering new twitter state");
            QueryResult res = Twitter.query(searchQuery).sinceId(0).rpp(rpp)
                    .execute();
            Logger.debug("found %1s tweet to process", res.getTweets().size());
            for (JsonElement tweet : res.getTweets()) {
                try {
                    processTweet(tweet);
                } catch (ReachStopIdException e) {
                    Logger.warn(e, "Should never reach stopId");
                }
            }
            
            gallery = Gallery.findById(this.gallery.id);
            
            if (res.hasNextPage()) {
                gallery.maxId = res.getMaxId();
                gallery.lastPage = res.getPage();
                gallery.state = State.FETCH_OLDER;
            } else {
                gallery.stopId = res.getMaxId();
                gallery.state = State.FETCH_YOUNGER;
            }
            Logger.debug("Switch to %1s", gallery.state);
            
            gallery.save();
        } else if (gallery.state == State.FETCH_OLDER) {
        	Logger.debug("entering fetch old twitter state");
            int newPage = gallery.lastPage + 1;
            Logger.debug("Query maxId=%1s page=%2s rpp=%3s", gallery.maxId,
                    newPage, rpp);
            QueryResult res = Twitter.query(searchQuery).maxId(gallery.maxId)
                    .page(newPage).rpp(100).execute();
            
            for (JsonElement tweet : res.getTweets()) {
                try {
                    processTweet(tweet);
                } catch (ReachStopIdException e) {
                    Logger.debug("Already reach stopId");
                    gallery = Gallery.findById(this.gallery.id);
                    gallery.stopId = res.getMaxId();
                    gallery.state = State.FETCH_YOUNGER;
                    gallery.save();
                    return;
                }
            }
            
            gallery = Gallery.findById(this.gallery.id);
            
            if (res.hasNextPage()) {
                gallery.lastPage = newPage;
            } else {
                gallery.stopId = res.getMaxId();
                gallery.state = State.FETCH_YOUNGER;
            }
            Logger.debug("Switch to %1s", gallery.state);
            
            gallery.save();
        } else if (gallery.state == State.FETCH_YOUNGER) {
        	Logger.debug("entering fetch younger twitter state");
            QueryResult res = Twitter.query(searchQuery).sinceId(gallery.maxId).rpp(rpp).execute();
            boolean passEndDate = new Date().after(gallery.endDate);
            
            for (JsonElement tweet : res.getTweets()) {
                try {
                    processTweet(tweet);
                } catch (ReachStopIdException e) {
                    Logger.debug("Already reach stopId");
                    gallery = Gallery.findById(this.gallery.id);
                    if (passEndDate) {
                        gallery.state = State.DONE;
                    } else {
                        gallery.stopId = res.getMaxId();
                        gallery.state = State.FETCH_YOUNGER;
                    }
                    gallery.save();
                    return;
                }
            }
            
            gallery = Gallery.findById(this.gallery.id);
            
            if (res.hasNextPage()) {
                gallery.maxId = res.getMaxId();
                gallery.lastPage = res.getPage();
                gallery.state = State.FETCH_OLDER;
            } if (passEndDate) {
                gallery.state = State.DONE;
            } else {
                gallery.stopId = res.getMaxId();
            }
            
            Logger.debug("Switch to %1s", gallery.state);
            
            gallery.save();
        }
        
    }
    
    /**
     * Get the query part to filter tweets only for the available photo service
     * only.
     * 
     * @return the query part.
     */
    private static final String photoServiceQueryPart() {
        String[] prefixes = PhotoServices.getSearchKeys();
        return "(" + JavaExtensions.join(Arrays.asList(prefixes), " OR ") + ")";
    }
    
    /**
     * Build the query based on the given <tt>Gallery</tt>.
     * 
     * @param gallery is the <tt>Gallery</tt>.
     * @return the query.
     */
    private static String buildQuery(Gallery gallery) {
        QueryBuilder queryBuilder = new QueryBuilder(
                "#" + gallery.hashtag 
                + " " + photoServiceQueryPart() + " -RT");
        if (gallery.startDate != null) {
            queryBuilder.since(gallery.startDate);
            if(Play.configuration.getProperty("twitter.search.until.enabled", "true").equals("true")) {
                if (gallery.endDate != null) {
                    queryBuilder.until(gallery.endDate);
                }
            }
        }
        
        if (gallery.location != null && gallery.location.trim().length() != 0) {
            queryBuilder.near(gallery.location);
        }
        return queryBuilder.toString();
    }
    
    /**
     * Process the tweet from the given tweet entry.
     * 
     * @param tweet is the tweet entry.
     * @throws ReachStopIdException if the id is lower than gallery.stopId.
     */
    private void processTweet(JsonElement tweet) throws ReachStopIdException {
        JsonObject tweetObject = tweet.getAsJsonObject();
        
        long id = tweetObject.getAsJsonPrimitive("id").getAsLong();
        String tweetText = tweetObject.getAsJsonPrimitive("text").getAsString();
        String username = tweetObject.getAsJsonPrimitive("from_user")
                .getAsString();
        long twitterId = tweetObject.getAsJsonPrimitive("from_user_id").getAsLong();
        String createdDateStr = tweetObject.getAsJsonPrimitive("created_at")
                .getAsString();
        
        User user = User.findByTwitterId(twitterId);
        if (user == null) {
        	Logger.debug("creating a new user %1s", username);
            user = new User(twitterId, username).save();
        }
        
        DateFormat dateFormat = newDateFormat();
        Date createdDate = null;
        try {
            createdDate = dateFormat.parse(createdDateStr);
        } catch (ParseException e) {
            Logger.error("Invalid date format of created_date %1s", createdDateStr);
            return;
        }
        
        if (id < gallery.stopId) {
            throw new ReachStopIdException("The tweet already reach the stopId");
        }
        
        if (gallery.endDate != null && createdDate.after(gallery.endDate)) {
            // skip the endDate since we can disabled/not using the twitter
            // "until" operator
            return;
        }
        
        
        List<String> urlList = getExpandedUrls(tweetObject);
        String [] urlArray = new String[urlList.size()];
        
        Logger.debug("expandeed url %s", Arrays.toString(urlList.toArray(urlArray)));        
        PhotoResource[] tweetPhotos = PhotoServices.mapUrlToPhotoService(urlList.toArray(urlArray));
        
        Logger.debug("processing tweet with text:%1s", tweetText);
        if (tweetPhotos != null) {
            Logger.debug("Found recognize url from tweet: %1s", tweetText);
            for (PhotoResource tweetPhoto : tweetPhotos) {
                new RetrievePhotoUrlJob(gallery, tweetPhoto, user.id, tweetText).now();
            }
        }
    }

	public List<String> getExpandedUrls(JsonObject tweetObject) {
		//Get real link of the image urls
        List<String> urlList = new ArrayList<String>();
        JsonElement entities = tweetObject.get("entities");
        if( entities != null){
        	JsonArray urls = entities.getAsJsonObject().get("urls").getAsJsonArray();
        	if(urls !=null){
        		for (JsonElement urlObject : urls) {
        			urlList.add(urlObject.getAsJsonObject().get("expanded_url").getAsString());
				}
        	}
        }
		return urlList;
	}
    
    /**
     * Create the date formatter compatible with twitter date.
     * 
     * @return the date formatter.
     */
    private static DateFormat newDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss ZZZZZZ", Locale.US);
        dateFormat.setLenient(false);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat;
    }
    
    
    /**
     * This exception thrown if the processed tweet already reach the stopId.
     * 
     * @author uudashr@gmail.com
     *
     */
    private static class ReachStopIdException extends Exception {
        public ReachStopIdException(String message) {
            super(message);
        }
    }
}
