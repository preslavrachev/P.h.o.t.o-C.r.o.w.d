package jobs;

import java.io.UnsupportedEncodingException;

import models.Gallery;
import play.Logger;
import play.jobs.Job;
import utils.SearchQueryBuilder;
import utils.StringUtils;
import utils.TwitterUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GalleryJob extends Job<Void> {
    private Gallery gallery;

    public GalleryJob(Gallery crowdGallery) {
        this.gallery = crowdGallery;
    }

    @Override
    public void doJob() throws Exception {
        String searchQuery = buildQuery(gallery);
        Logger.debug("Searching '%1s'", searchQuery);
        JsonArray results = TwitterUtil.searchTwitter(
                searchQuery, 
                gallery.lastId);

        Logger.debug("tweet search result:" + results);
        for (JsonElement tweet : results) {
            processTweet(tweet);
        }
        saveTweetLastId(results);
    }
    
    private static String buildQuery(Gallery gallery) throws UnsupportedEncodingException {
        String hashtagQuery = gallery.hashtag;
        if (!gallery.hashtag.startsWith("#")) {
            hashtagQuery = "#" + gallery.hashtag;
        }
        SearchQueryBuilder queryBuilder = new SearchQueryBuilder(hashtagQuery);
        
        if (gallery.startDate != null) {
            queryBuilder.since(gallery.startDate);
            if (gallery.endDate != null) {
                queryBuilder.until(gallery.endDate);
            }
        }
        
        if (gallery.location != null && gallery.location.trim().length() != 0) {
            queryBuilder.near(gallery.location);
        }
        return queryBuilder.toEncodedURL("UTF-8");
    }
    
    private void saveTweetLastId(JsonArray results) {
        Gallery gallery = Gallery.findById(this.gallery.id);
        if (results.size() > 0) {
            Long lastId = results.get(0).getAsJsonObject().getAsJsonPrimitive("id")
                    .getAsLong();
            Logger.debug("Tweet last Id" + lastId);
            Logger.debug("crowd gallery id" + gallery.id);
            
            Logger.info("Crowd gallery" + gallery);
            if (lastId == null) {
                return;
            }
            gallery.lastId = lastId;
        } else {
            gallery.state = false;
        }
        gallery.save();
    }

    private void processTweet(JsonElement tweet) {
        JsonObject tweetObject = tweet.getAsJsonObject();
        String tweetText = tweetObject.getAsJsonPrimitive("text").getAsString();
        String username = tweetObject.getAsJsonPrimitive("from_user")
                .getAsString();
        Logger.debug("tweet text:" + tweetText);
        String[] urls = StringUtils.grabImageServiceURLs(tweetText);
        for (String url : urls) {
            initPhotoJob(tweetText, username, url);
        }
    }

    private void initPhotoJob(String tweetText, String username, String url) {
        PhotoJob photoJob = new PhotoJob(gallery, url, username, tweetText);
        photoJob.now();
    }

}
