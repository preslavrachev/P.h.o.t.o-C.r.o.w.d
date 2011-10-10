package utils.photoservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;

/**
 * This is utility to extract photo resource from various photo services.
 * 
 * @author uudashr@gmail.com
 *
 */
public class PhotoServices {
    private static final Map<String, PhotoService> photoServices = new HashMap<String, PhotoService>();
    
    static {
        photoServices.put("lockerz", new LockerzPhotoService());
        photoServices.put("twitgoo", new TwitgooPhotoService());
        photoServices.put("twitpic", new TwitpicPhotoService());
        
    }
    
    /**
     * This will extract the photo resources from a tweet text.
     * 
     * @param tweet is the tweet text
     * @return the photo resources.
     */
    public static PhotoResource[] extractPhotoResource(String tweet) {
        for (PhotoService service : photoServices.values()) {
            Logger.debug("Using service %1s to search tweet: %2s", service.getClass().getName(), tweet);
            String[] urls = service.findURL(tweet);
            Logger.debug("Found %1s", Arrays.toString(urls));
            if (urls != null) {
                PhotoResource[] tweetPhotos = new PhotoResource[urls.length];
                for (int i = 0; i < urls.length; i++) {
                    tweetPhotos[i] = new PhotoResource(urls[i], service);
                }
                return tweetPhotos;
            }
        }
        return null;
    }
    
    public static PhotoResource[] mapUrlToPhotoService(String[] urls){
    	List<PhotoResource> photoResources = new ArrayList<PhotoServices.PhotoResource>();
    	for (String url : urls) {
    		
			for(PhotoService service : photoServices.values()){
				if(service.isUsingService(url)){
					Logger.debug("Found photo url %s using %s photo service" , url, service.getClass().getName());
					photoResources.add(new PhotoResource(url, service));
				}
			}
		}
    	if(photoResources.size()>0){
        	PhotoResource[] photoResArray = new PhotoResource[photoResources.size()];
        	return photoResources.toArray(photoResArray);
        }
        return null;
    }
    
    /**
     * This will extract the photo resources from a tweet text.
     * 
     * @param tweet is the tweet text
     * @return the photo resources.
     */
    public static PhotoResource[] extractPhotoResourceFromTweetId(long tweetId) {
    	List<PhotoResource> urlList = new ArrayList<PhotoResource>();
        for (PhotoService service : photoServices.values()) {
        	Logger.debug("using photoservice %1s", service.getClass().getName());
            String[] urls = service.findUrlByTweetId(tweetId);
            Logger.debug("Found %1s", Arrays.toString(urls));
            if (urls.length >0) {
                for (int i = 0; i < urls.length; i++) {
                    urlList.add(new PhotoResource(urls[i], service));
                }
            }
        }
        if(urlList.size()>0){
        	PhotoResource[] urls = new PhotoResource[urlList.size()];
        	return urlList.toArray(urls);
        }
        return null;
    }
    
    public static PhotoResource[] cratePhotoResources(String[] urls){
    	List<PhotoResource> urlList = new ArrayList<PhotoResource>();
        for (PhotoService service : photoServices.values()) {
            if (urls.length >0) {
                for (int i = 0; i < urls.length; i++) {
                	String[] matchUrls = service.findURL(urls[i]);
                	if(matchUrls !=null){
                    	Logger.debug("using photoservice %1s", service.getClass().getName());           
                        Logger.debug("Found %1s", Arrays.toString(matchUrls));
                		for (String string : matchUrls) {
                    		urlList.add(new PhotoResource(string, service));
    					}                	
                	}                	
                }
            }
        }
        if(urlList.size()>0){
        	PhotoResource[] photoServices = new PhotoResource[urlList.size()];
        	return urlList.toArray(photoServices);
        }
        return null;
    }
    

    /**
     * Return all the search keys from the available photo services.
     * 
     * @return search keys.
     */
    public static String[] getSearchKeys() {
        String[] prefixes = new String[photoServices.size()];
        int i = 0;
        for (PhotoService photoService : photoServices.values()) {
            prefixes[i++] = photoService.getSearchKey();
        }
        return prefixes;
    }


    /**
     * Represent the photo resource from photo service.
     * 
     * @author uudashr@gmail.com
     *
     */
    public static class PhotoResource {
        
        public final String url;
        private PhotoService photoService;
        
        /**
         * Represent the tweet photo resource, URL that contained on the tweet.
         * 
         * @param url is the tweet photo URL.
         * @param photoService
         */
        public PhotoResource(String url, PhotoService photoService) {
            this.url = url;
            this.photoService = photoService;
        }
        
        /**
         * This will grab the URL of original size image and the thumbnail.
         * 
         * @return the holder of original size and thumbnail.
         */
        public ImageAndThumbnailUrlHolder grab() {
            return photoService.grab(url);
        }
        
    }

}
