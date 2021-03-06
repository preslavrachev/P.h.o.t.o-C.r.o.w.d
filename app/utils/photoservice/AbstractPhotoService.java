package utils.photoservice;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.print.resources.serviceui;


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
        this.urlPattern = Pattern.compile(urlPrefix + "/\\S+(/\\S+)*");
        this.searchKey = searchKey;
    }

    @Override
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
    public String[] filter(String[] urls) {
        List<String> filteredUrls = new ArrayList<String>();
        for (int i = 0; i < urls.length; i++) {
            if (urlPattern.matcher(urls[i]).matches()) {
                filteredUrls.add(urls[i]);
            }
        }
        if (filteredUrls.isEmpty()) {
            // this cannot find the supported URL
            return null;
        }
        return filteredUrls.toArray(new String[filteredUrls.size()]);
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
