package models;

import javax.persistence.Column;
import javax.persistence.Entity;

import play.data.validation.Required;

@Entity
public class User extends Model {
    @Required
    @Column(nullable = false, unique = true)
    public String username;

    @Required
    @Column(nullable = false, name = "secret_token")
    public String secretToken;

    @Required
    @Column(nullable = false, name = "access_token")
    public String accessToken;

    @Required
    @Column(nullable = false, name = "twitter_id")
    public Long twitterId;

    @Column(name = "profile_image_url")
    public String profileImageUrl;
    
    @Column(name = "profile_image_mini_url")
    public String profileImageMiniUrl;
    
    @Column(name = "profile_image_bigger_url")
    public String profileImageBiggerUrl;
    
    @Column(name = "profile_image_original_url")
    public String profileImageOriginalUrl;

    public static User findByUsername(String username) {
        return User.find("byUsername", username).first();
    }

    public static User findByTwitterId(Long twitterId) {
        return User.find("byTwitterId", twitterId).first();
    }

}
