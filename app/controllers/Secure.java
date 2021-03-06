package controllers;

import models.User;
import play.Logger;
import play.Play;
import play.libs.OAuth;
import play.libs.OAuth.Response;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.Router.Route;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author uudashr@gmail.com
 *
 */
public class Secure extends Controller {
    private static final String TWITTER_REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
    private static final String TWITTER_ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
    private static final String TWITTER_VERIFY_CREDENTIALS_URL = "http://api.twitter.com/1/account/verify_credentials.json";
    private static final String TWITTER_PROFILE_IMAGE_URL = "http://api.twitter.com/1/users/profile_image";
    private static final ServiceInfo twitterServiceInfo = new ServiceInfo(
            TWITTER_REQUEST_TOKEN_URL, TWITTER_ACCESS_TOKEN_URL,
            Play.configuration.getProperty("twitter.authorizationUrl",
                    "https://api.twitter.com/oauth/authorize"),
            Play.configuration.getProperty("twitter.consumer.key"),
            Play.configuration.getProperty("twitter.consumer.secret"));
    
    @Before(unless={"authenticate"})
    static void checkAccess() throws Throwable {
        if (!session.contains("loggedUser.id")) {
            flash.put("url", "GET".equals(request.method) ? request.url : "/");
            authenticate();
        }
        
        // Checks
        Check check = getActionAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
        check = getControllerInheritedAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
        
        renderArgs.put("loggedUser", Security.connectedUser());
    }
    
    private static void check(Check check) throws Throwable {
        for(String profile : check.value()) {
            if (profile.equals("admin") && !Security.connectedIsAdmin()) {
                forbidden("Restricted admin area");
            }
        }
    }
    
    public static void authenticate() {
        String deninedHash = params.get("denied");
        if (deninedHash != null) {
            Application.index();
        }
        
        if (OAuth.isVerifierResponse()) {
            Response accessTokenResp = OAuth.service(twitterServiceInfo)
                    .retrieveAccessToken(
                            session.get("twitter.token"),
                            session.get("twitter.secret"));
            
            JsonElement json = WS.url(TWITTER_VERIFY_CREDENTIALS_URL)
                    .oauth(twitterServiceInfo, 
                            accessTokenResp.token, 
                            accessTokenResp.secret).get().getJson();
            JsonObject jsonObj = json.getAsJsonObject();
            Logger.debug(jsonObj.toString());
            Long twitterId = jsonObj.getAsJsonPrimitive("id").getAsLong();
            String username = jsonObj.getAsJsonPrimitive("screen_name").getAsString();
            
            String profileImageMiniUrl = retrieveProfileImageUrl(username, "mini");
            String profileImageBiggerUrl = retrieveProfileImageUrl(username, "bigger");
            String profileImageOriginalUrl = retrieveProfileImageUrl(username, "original");
            
            User user = User.findByTwitterId(twitterId);
            if (user == null) {
                user = new User();
                user.twitterId = twitterId;
            }
            
            user.username = username;
            user.profileImageUrl = jsonObj.getAsJsonPrimitive("profile_image_url").getAsString();
            user.profileImageMiniUrl = profileImageMiniUrl;
            user.profileImageBiggerUrl = profileImageBiggerUrl;
            user.profileImageOriginalUrl = profileImageOriginalUrl;
            user.accessToken = accessTokenResp.token;
            user.secretToken = accessTokenResp.secret;
            user.save();
            
            session.put("loggedUser.id", user.id);
            
            String url = flash.get("url");
            if(url != null) {
                redirect(url);
            }
            Galleries.index();
        }
        
        OAuth twitt = OAuth.service(twitterServiceInfo);
        Response reqTokenResp = twitt.retrieveRequestToken(
                Play.configuration.getProperty("twitter.callbackUrl", 
                        "http://localhost:9000/secure/authenticate"));
        
        session.put("twitter.secret", reqTokenResp.secret);
        session.put("twitter.token", reqTokenResp.token);
        flash.keep("url");
        redirect(twitt.redirectUrl(reqTokenResp.token));

    }
    
    public static void signOut() {
        session.clear();
        Application.index();
    }
    
    private static String retrieveProfileImageUrl(String username, String size) {
        HttpResponse resp = WS.url(TWITTER_PROFILE_IMAGE_URL).setParameter("screen_name", username).setParameter("size", size).followRedirects(false).get();
        if (resp.getStatus() == 302) {
            return resp.getHeader("location");
        }
        return null;
    }
    
    static void redirectToOriginalURL() throws Throwable {
        String url = flash.get("url");
        if(url == null) {
            url = "/";
        }
        redirect(url);
    }

    public static class Security extends Controller {
        
        static boolean isConnected() {
            return session.get("loggedUser.id") != null;
        }
        
        static String connected() {
            return session.get("loggedUser.id");
        }
        
        static User connectedUser() {
            String userId = connected();
            if (userId == null) {
                return null;
            }
            return User.findById(Long.valueOf(userId));
        }
        
        static boolean connectedIsAdmin() {
            User connectedUser = connectedUser();
            return "andriavi".equals(connectedUser.username) || "uudashr".equals(connectedUser.username);
        }
    }
}
