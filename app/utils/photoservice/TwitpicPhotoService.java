package utils.photoservice;

import java.io.IOException;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

/**
 * Photo service implementation of Twitpic.
 * 
 * @author uudashr@gmail.com
 *
 */
public class TwitpicPhotoService extends AbstractPhotoService {
    public static final String URL_PREFIX = "http://twitpic.com/";
    
    public TwitpicPhotoService() {
        super("twitpic.com", "http://twitpic.com");
    }

    @Override
    public ImageAndThumbnailUrlHolder grab(String photoUrl) {
        String imageId = parseId(photoUrl);
        
        String imageUrl = grabImageUrl(photoUrl);
        if (imageUrl == null) {
            return null;
        }
        
        String thumbnailUrl = grabThumbnailUrl(imageId);
        if (thumbnailUrl == null) {
            return null;
        }
        
        long imageUrlExpires = Long.MAX_VALUE;
        try {
            imageUrlExpires = extractExpiresToTimestamp(imageUrl);
        } catch (IllegalArgumentException e) {
            Logger.warn("Photo url %1s doesn't contains expiry time", photoUrl);
        }
        
        long thumbnailUrlExpires = Long.MAX_VALUE;
        try {
            thumbnailUrlExpires = extractExpiresToTimestamp(thumbnailUrl);
        } catch (IllegalArgumentException e) {
            Logger.warn("Photo thumbnail url %1s doesn't contains expiry time", photoUrl);
        }
        
        long expires = Math.min(imageUrlExpires, thumbnailUrlExpires);
        
        if (expires != Long.MAX_VALUE) {
            return new ImageAndThumbnailUrlHolder(photoUrl, imageUrl, thumbnailUrl, expires);
        }
        
        return new ImageAndThumbnailUrlHolder(photoUrl, imageUrl, thumbnailUrl);
    }
    
    private static String grabImageUrl(String photoUrl) {
        HttpResponse res = WS.url(photoUrl).get();
        StringBuffer html = new StringBuffer(res.getString());
        Source source;
        try {
            source = new Source(new ByteInputStream(html.toString().getBytes(),
                    html.toString().length()));
            Element el = source.getElementById("photo-display");
            if (el == null) {
                Logger.warn("Cannot find element photo-display from URL %s", photoUrl);
                return null;
            }
            String url = el.getAttributeValue("src");
            
            return url;
        } catch (IOException e) {
            Logger.error(e, "Failed getting photo from twitpic using URL %s", photoUrl);
        }
        return null;
    }
    
    private static String grabThumbnailUrl(String imageId) {
        HttpResponse resp = WS.url("http://twitpic.com/show/large/%s", imageId).followRedirects(false).get();
        if (resp.getStatus() != 302) {
            Logger.warn("Get large thumbnail from twitpic for imageId %s doen't return 302 status, instead of %s", imageId, resp.getStatus());
            return null;
        }
        return resp.getHeader("Location");
    }
    
    public static String parseId(String twitpicUrl) {
        if (!twitpicUrl.startsWith(URL_PREFIX)) {
            throw new IllegalArgumentException("Twitpic URL should start with 'http://twitpic.com/'");
        }
        return twitpicUrl.substring(URL_PREFIX.length());
    }
    
    public static String extractExpires(String twitpicImageUrl) {
        final String expiresPrefix = "Expires=";
        int index = twitpicImageUrl.indexOf(expiresPrefix);
        
        if (index < 0) {
            throw new IllegalArgumentException("The twitpicImageUrl doesn't contains Expires parameter: " + twitpicImageUrl);
        }
        
        int indexOfEnd = twitpicImageUrl.indexOf("&", index);
        if (indexOfEnd > 0) {
            return twitpicImageUrl.substring(index + expiresPrefix.length(), indexOfEnd);
        }
        return twitpicImageUrl.substring(index + expiresPrefix.length());
    }
    
    public static long extractExpiresToTimestamp(String twitpicImageUrl) {
        return Long.parseLong(extractExpires(twitpicImageUrl)) * 1000;
    }
}
