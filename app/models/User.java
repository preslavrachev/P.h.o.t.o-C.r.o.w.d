package models;

import javax.persistence.Column;
import javax.persistence.Entity;

import play.Logger;
import play.data.validation.Required;
import utils.Twitter;
import utils.Twitter.TwitterUser;

@Entity
public class User extends Model {
    @Required
    @Column(nullable = false, unique = true)
    public String username;
    
    @Required
    @Column(nullable = false, name = "twitter_id", unique = true)
    public Long twitterId;

    @Required
    @Column(name = "secret_token")
    public String secretToken;

    @Required
    @Column(name = "access_token")
    public String accessToken;

    @Column(name = "profile_image_url")
    public String profileImageUrl;
    
    @Column(name = "profile_image_mini_url")
    public String profileImageMiniUrl;
    
    @Column(name = "profile_image_bigger_url")
    public String profileImageBiggerUrl;
    
    @Column(name = "profile_image_original_url")
    public String profileImageOriginalUrl;
    
    public User() {
    }
    
    public User(Long twitterId, String username) {
        this.twitterId = twitterId;
        this.username = username;
    }
    
    public static User findByUsername(String username) {
        return User.find("byUsername", username).first();
    }

    public static User findByTwitterId(Long twitterId) {
        return User.find("byTwitterId", twitterId).first();
    }
    
    public static User findOrCreateByTwitterId(Long twitterId) {
    	Logger.debug("trying to find or craete user with twitter id : %1s", twitterId);
        User user = User.find("byTwitterId", twitterId).first();
    	if(user==null){
    		TwitterUser tUser = Twitter.getUserDetails(twitterId);
    		user = new User();
    		user.username = tUser.getScreenName();
    		user.profileImageMiniUrl = Twitter.getProfileImageMiniUrl(tUser.getScreenName());
    		user.profileImageBiggerUrl = Twitter.getProfileImageBiggerUrl(tUser.getScreenName());
    		user.profileImageOriginalUrl = Twitter.getProfileImageOriginalUrl(tUser.getScreenName());
    		user.twitterId = twitterId;
    		user.save();
    	}
    	return user;
    }

}
