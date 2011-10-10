package utils.photograbber;

import org.junit.Test;

import play.test.UnitTest;
import utils.photoservice.ImageAndThumbnailUrlHolder;
import utils.photoservice.TwitpicPhotoService;

/**
 * @author uudashr@gmail.com
 *
 */
public class TwitpicPhotoGrabberTest extends UnitTest {
    
    @Test
    public void middleParamExpires() {
        String twitpicThumbUrl = "http://s3.amazonaws.com/twitpic/photos/large/368848220.jpg?AWSAccessKeyId=AKIAJF3XCCKACR3QDMOA&Expires=1312926252&Signature=xwC90ofLaWfFVpoHmX0b4AuSmsI%3D";
        assertEquals("1312926252", TwitpicPhotoService.extractExpires(twitpicThumbUrl));
    }
    
    @Test
    public void firstParamExpires() {
        String twitpicThumbUrl = "http://s3.amazonaws.com/twitpic/photos/large/368848220.jpg?Expires=1312926252&AWSAccessKeyId=AKIAJF3XCCKACR3QDMOA&Signature=xwC90ofLaWfFVpoHmX0b4AuSmsI%3D";
        assertEquals("1312926252", TwitpicPhotoService.extractExpires(twitpicThumbUrl));
    }
    
    @Test
    public void lastParamExpires() {
        String twitpicThumbUrl = "http://s3.amazonaws.com/twitpic/photos/large/368848220.jpg?AWSAccessKeyId=AKIAJF3XCCKACR3QDMOA&Signature=xwC90ofLaWfFVpoHmX0b4AuSmsI%3D&Expires=1312926252";
        assertEquals("1312926252", TwitpicPhotoService.extractExpires(twitpicThumbUrl));
    }
    
    @Test
    public void grabFullUrl(){
    	TwitpicPhotoService service = new TwitpicPhotoService();
    	String url = "http://s3.amazonaws.com/twitpic/photos/full/415470193.png";
    	String twitpicUrl = "http://twitpic.com/6vcyup";
    	ImageAndThumbnailUrlHolder holder = service.grab(twitpicUrl);
    	assertEquals(url, holder.url.substring(0, url.length()));
    	
    }
    
    @Test
    public void isUrlIsUsingTwitpicService(){
    	String twitpicUrl = "http://twitpic.com/6vcyup";
    	TwitpicPhotoService service = new TwitpicPhotoService();
    	assertTrue(service.isUsingService(twitpicUrl));
    }
    
}
