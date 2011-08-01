package jobs;

import java.io.UnsupportedEncodingException;

import models.Gallery;
import play.Logger;
import play.jobs.Job;
import utils.SearchQueryBuilder;
import utils.SearchResponse;
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
		SearchResponse resp;
		if (gallery.refresh) {
			resp = TwitterUtil.searchTwitterLatest(searchQuery, gallery.lastId);
		} else {
			resp = TwitterUtil.searchTwitterNext(searchQuery, gallery.lastId,
					gallery.page);
		}
		if(resp.getTweets() == null){
			return;
		}
		for (JsonElement tweet : resp.getTweets()) {
			processTweet(tweet);
		}
		prepareForNextCall(resp);
	}

	private static String buildQuery(Gallery gallery)
			throws UnsupportedEncodingException {
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

	private void prepareForNextCall(SearchResponse resp) {
		Gallery gallery = Gallery.findById(this.gallery.getId());
		if (resp.getPage() == 1) {
			gallery.lastId = resp.getMaxId();
		}
		if (resp.hasNextPage()) {
			gallery.page = gallery.page++;
		} else {
			gallery.refresh = true;
			gallery.page = 1;
		}
		gallery.save();
	}

	private void initPhotoJob(String tweetText, String username, String url) {
		GrabPhotoJob photoJob = new GrabPhotoJob(gallery, url, username,
				tweetText);
		photoJob.now();
	}

}
