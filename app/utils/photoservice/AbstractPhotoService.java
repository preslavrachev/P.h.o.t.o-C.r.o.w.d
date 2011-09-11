package utils.photoservice;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.Logger;

import sun.print.resources.serviceui;
import utils.Twitter;
import utils.Twitter.TwitterStatus;


/**
 * This is common implementation of {@link PhotoService}.
 * 
 * @author uudashr@gmail.com
 *
 */
public abstract class AbstractPhotoService implements PhotoService {
    private final String searchKey;
    private final Pattern urlPattern;
    
    public AbstractPhotoService(String searchKey, String urlPrefix) {
        this.urlPattern = Pattern.compile(urlPrefix + "/\\w+(/\\w+)*"); 
        this.searchKey = searchKey;
    }

    public String[] findURL(String text) {
        Matcher matcher = urlPattern.matcher(text);
        List<String> urls = new ArrayList<String>();
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        if (urls.isEmpty()) {
            // this cannot find the supported URL
            return null;
        }
        return urls.toArray(new String[urls.size()]);
    }
    
    
    @Override
	public String[] findUrlByTweetId(long tweetId) {
    	TwitterStatus status = Twitter.getStatus(tweetId);
    	List<String> urls = status.getExpendedUrls();
    	List<String> filteredUrl = new ArrayList<String>();
    	for (String url : urls) {    	
    		Logger.debug("Using service %1s to match this url: %2s", this.getClass().getName(), url);
			if(url.matches(urlPattern.pattern())){				
				filteredUrl.add(url);
			}
		}
    	if (urls.isEmpty()) {
            // this cannot find the supported URL
            return null;
        }
    	String[] stringUrls = new String[filteredUrl.size()];
		return filteredUrl.toArray(stringUrls);
	}

	@Override
    public boolean recognize(String photoUrl) {
        return urlPattern.matcher(photoUrl).matches();
    }
    
    @Override
    public String getSearchKey() {
        return searchKey;
    }
}
