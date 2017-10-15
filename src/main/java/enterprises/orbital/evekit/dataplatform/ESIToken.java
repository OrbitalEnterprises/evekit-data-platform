package enterprises.orbital.evekit.dataplatform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import javax.persistence.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An key/hash pair for accessing ESI via the proxy. This object records:
 *
 * <ul>
 * <li>The latest access token for an EVE user.
 * <li>The latest refresh token for an EVE user.
 * <li>The list of scopes associated with this key when it was created.
 * <li>The character name associated with the key when it was authenticated.
 * </ul>
 */
@Entity
@Table(
    name = "esi_token",
    indexes = {
        @Index(
            name = "accountIndex",
            columnList = "uid"),
        @Index(
            name = "keyIndex",
            columnList = "kid",
            unique = true)
    })
@NamedQueries({
    @NamedQuery(
        name = "ESIToken.findByID",
        query = "SELECT c FROM ESIToken c where c.kid = :kid"),
    @NamedQuery(
        name = "ESIToken.findAllByUser",
        query = "SELECT c FROM ESIToken c where c.userAccount = :userAccount")
})
@ApiModel(
    description = "ESI access key")
@JsonIgnoreProperties({
    "accessToken", "refreshToken"
})
public class ESIToken {
  protected static final Logger log = Logger.getLogger(ESIToken.class.getName());

  // Unique key ID
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "ekdp_seq")
  @SequenceGenerator(
      name = "ekdp_seq",
      initialValue = 100000,
      allocationSize = 10,
      sequenceName = "dp_sequence")
  @JsonProperty("kid")
  private long kid;

  // User which owns this key
  @ManyToOne
  @JoinColumn(
      name = "uid",
      referencedColumnName = "uid")
  @JsonProperty("userAccount")
  private DataPlatformUserAccount userAccount;

  // Space delimited list of scopes attached to this key when it was created
  @Lob
  @Column(
      length = 102400)
  @JsonProperty("scopes")
  private String scopes;

  // The character name associated with this key. This was the character used during OAuth authentication.
  @JsonProperty("characterName")
  private String characterName;

  // Latest access token
  private String accessToken;

  // Expiry date (millis UTC) of access token
  @JsonProperty("accessTokenExpiry")
  private long accessTokenExpiry;

  // Latest refresh token
  private String refreshToken;

  // True if refresh token is non-null and non-empty, false otherwise.
  // Set before returning token data to web client.
  @Transient
  @JsonProperty("valid")
  private boolean valid;

  public long getKid() {
    return kid;
  }

  public DataPlatformUserAccount getUserAccount() {
    return userAccount;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String scopes) { this.scopes = scopes; }

  public String getCharacterName() {
    return characterName;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public long getAccessTokenExpiry() {
    return accessTokenExpiry;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void setAccessTokenExpiry(long accessTokenExpiry) {
    this.accessTokenExpiry = accessTokenExpiry;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public boolean isValid() { return valid; }

  public void updateValid() {
    valid = refreshToken != null && !refreshToken.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ESIToken esiToken = (ESIToken) o;

    if (kid != esiToken.kid) return false;
    if (!userAccount.equals(esiToken.userAccount)) return false;
    if (!scopes.equals(esiToken.scopes)) return false;
    return characterName.equals(esiToken.characterName);
  }

  @Override
  public int hashCode() {
    int result = (int) (kid ^ (kid >>> 32));
    result = 31 * result + userAccount.hashCode();
    result = 31 * result + scopes.hashCode();
    result = 31 * result + characterName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ESIToken{" +
        "kid=" + kid +
        ", userAccount=" + userAccount +
        ", scopes='" + scopes + '\'' +
        ", characterName='" + characterName + '\'' +
        ", accessToken='" + accessToken + '\'' +
        ", accessTokenExpiry=" + accessTokenExpiry +
        ", refreshToken='" + refreshToken + '\'' +
        '}';
  }

  public static ESIToken createKey(final DataPlatformUserAccount userAccount, final String scopes,
                                   final String characterName) {
    ESIToken newKey = null;
    try {
      newKey = DataPlatformProvider.getFactory()
                                   .runTransaction(() -> {
                                     ESIToken result = new ESIToken();
                                     result.userAccount = userAccount;
                                     result.scopes = scopes;
                                     result.characterName = characterName;
                                     return DataPlatformProvider.getFactory()
                                                                .getEntityManager()
                                                                .merge(result);
                                   });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return newKey;
  }

  public static ESIToken getKeyByID(final long kid) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<ESIToken> getter = DataPlatformProvider.getFactory()
                                                                                     .getEntityManager()
                                                                                     .createNamedQuery("ESIToken.findByID", ESIToken.class);
                                   getter.setParameter("kid", kid);
                                   try {
                                     return getter.getSingleResult();
                                   } catch (NoResultException e) {
                                     return null;
                                   }
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static List<ESIToken> getAllKeys(final DataPlatformUserAccount userAccount) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<ESIToken> getter = DataPlatformProvider.getFactory()
                                                                                     .getEntityManager()
                                                                                     .createNamedQuery("ESIToken.findAllByUser", ESIToken.class);
                                   getter.setParameter("userAccount", userAccount);
                                   return getter.getResultList();
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static boolean deleteKey(final DataPlatformUserAccount userAccount, final long kid) {
    try {
      DataPlatformProvider.getFactory()
                          .runTransaction(() -> {
                            ESIToken key = getKeyByID(kid);
                            if (key != null && key.getUserAccount()
                                                  .equals(userAccount))
                              DataPlatformProvider.getFactory()
                                                  .getEntityManager()
                                                  .remove(key);
                          });
      return true;
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return false;
  }

  public static ESIToken update(final ESIToken key) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> DataPlatformProvider.getFactory()
                                                                           .getEntityManager()
                                                                           .merge(key));
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
      return null;
    }
  }


}
