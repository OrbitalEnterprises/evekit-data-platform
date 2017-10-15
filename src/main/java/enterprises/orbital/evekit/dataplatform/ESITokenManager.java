package enterprises.orbital.evekit.dataplatform;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import enterprises.orbital.base.OrbitalProperties;
import enterprises.orbital.oauth.EVEApi;
import enterprises.orbital.oauth.EVEAuthHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Library for creating and maintaining auto-refreshed EVE Swagger Interface (ESI) tokens.
 * These tokens are tied to an orbital OAuth UserAccount.  Once setup, a token can be configured
 * to auto-refresh so that the token is valid just before it is needed.
 *
 * The process for creating a new key is as follows:
 *
 * <ol>
 * <li>Call createToken with a uid and a set of request scopes.  This method will pass a redirect string
 * which should be passed back to the authenticating user.</li>
 * <li>When the OAuth process invokes your callback, call the processTokenCallback with OAuth configuration
 * information including the servlet request.</li>
 * </ol>
 */
public class ESITokenManager {
  public static final String PROP_TOTAL_TOKEN_LIMIT = "enterprises.orbital.tokenLimit";
  public static final int DEF_TOTAL_TOKEN_LIMIT = 50;
  public static final  String            PROP_TEMP_TOKEN_LIFETIME = "enterprises.orbital.tempTokenLifetime";
  public static final  long              DEF_TEMP_TOKEN_LIFETIME  = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
  private static final Logger            log                      = Logger.getLogger(ESITokenManager.class.getName());
  private static boolean cleanerStarted = false;

  public static void init() {
    synchronized (ESITokenManager.class) {
      if (cleanerStarted) return;
      new Thread(() -> {
        while (true) {
          try {
            long now = OrbitalProperties.getCurrentTime();
            NewESIToken.cleanExpired(now);
            Thread.sleep(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
          } catch (Throwable e) {
            // Catch everything but log it
            log.log(Level.WARNING, "caught error in state cleanup loop (ignoring)", e);
          }
        }
      }).start();
      cleanerStarted = true;
    }
  }

  public static String createToken(HttpServletRequest req, DataPlatformUserAccount userAccount, String scopes,
                                   String callback, long existing, String eveClientID, String eveSecretKey)
      throws IOException, URISyntaxException {
    long now = OrbitalProperties.getCurrentTime();
    long expiry = now + OrbitalProperties.getLongGlobalProperty(PROP_TEMP_TOKEN_LIFETIME,
                                                                DEF_TEMP_TOKEN_LIFETIME);
    // If existing is not -1, then verify the existing key still exists and is owned by the specified user
    if (existing != -1) {
      ESIToken eKey = ESIToken.getKeyByID(existing);
      if (eKey == null || !eKey.getUserAccount().equals(userAccount)) return null;
    }
    NewESIToken key = NewESIToken.createKey(userAccount, now, expiry, scopes, existing);
    // Start the OAuth flow to authenticate the listed scopes
    return EVEAuthHandler.doGet(eveClientID, eveSecretKey, callback, scopes, key.getStateKey(), req);
  }

  public static boolean processTokenCallback(HttpServletRequest req, String verifyURL, String eveClientID,
                                             String eveSecretKey)
      throws IOException, URISyntaxException {
    // Extract key information associated with state.  Fail if no key information found.
    String       stateKey = req.getParameter("state");
    if (stateKey == null) return false;
    NewESIToken keyState = NewESIToken.getKeyByState(stateKey);
    if (keyState == null) return false;
    NewESIToken.deleteKey(keyState.getKid());

    // Construct the service to use for verification.
    OAuth20Service service = new ServiceBuilder().apiKey(eveClientID).apiSecret(eveSecretKey).build(EVEApi.instance());

    // Exchange for access token
    OAuth2AccessToken accessToken = service.getAccessToken(req.getParameter("code"));

    // Retrieve character selected for login. We add this to the access key to make it easier to identify the character associated with each key.
    OAuthRequest request = new OAuthRequest(Verb.GET, verifyURL, service.getConfig());
    service.signRequest(accessToken, request);
    com.github.scribejava.core.model.Response response = request.send();
    if (!response.isSuccessful()) throw new IOException("credential request was not successful!");
    String charName = (new Gson()).fromJson(
        (new JsonParser()).parse(response.getBody()).getAsJsonObject().get("CharacterName"), String.class);

    ESIToken update;
    if (keyState.getExistingKid() != -1) {
      // Re-authenticate an existing token
      update = ESIToken.getKeyByID(keyState.getExistingKid());
      if (update == null || !update.getUserAccount().equals(keyState.getUserAccount())) return false;
    } else {
      // Create the new token.
      update = ESIToken.createKey(keyState.getUserAccount(), keyState.getScopes(), charName);
    }
    update.setScopes(keyState.getScopes());
    update.setAccessToken(accessToken.getAccessToken());
    update.setAccessTokenExpiry(OrbitalProperties.getCurrentTime() +
                                    TimeUnit.MILLISECONDS.convert(accessToken.getExpiresIn(), TimeUnit.SECONDS));
    update.setRefreshToken(accessToken.getRefreshToken());
    ESIToken.update(update);

    return true;
  }

  public static String refreshToken(long kid, long expiryWindow, String eveClientID, String eveSecretKey)
      throws IOException {
    // Find token
    ESIToken key = ESIToken.getKeyByID(kid);
    if (key == null) throw new IOException("No key with ID: " + kid);
    // Ensure the access token is valid, if not attempt to renew it
    if (key.getAccessTokenExpiry() - OrbitalProperties.getCurrentTime() < expiryWindow) {
      // Key within expiry window, refresh
      String refreshToken = key.getRefreshToken();
      if (refreshToken == null) throw new IOException("No valid refresh token for key: " + kid);
      OAuth2AccessToken newToken     = EVEAuthHandler.doRefresh(eveClientID, eveSecretKey, refreshToken);
      if (newToken == null) {
        // Invalidate refresh token
        key.setRefreshToken(null);
        ESIToken.update(key);
        throw new IOException("Failed to refresh token for key: " + kid);
      }
      key.setAccessToken(newToken.getAccessToken());
      key.setAccessTokenExpiry(OrbitalProperties.getCurrentTime() +
                                   TimeUnit.MILLISECONDS.convert(newToken.getExpiresIn(), TimeUnit.SECONDS));
      key.setRefreshToken(newToken.getRefreshToken());
      key = ESIToken.update(key);
      if (key == null) throw new IOException("Failed to save refreshed token for key: " + kid);
    }
    return key.getAccessToken();
  }

}
