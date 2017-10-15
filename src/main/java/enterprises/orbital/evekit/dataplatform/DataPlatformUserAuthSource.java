package enterprises.orbital.evekit.dataplatform;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import enterprises.orbital.base.OrbitalProperties;
import enterprises.orbital.oauth.UserAccount;
import enterprises.orbital.oauth.UserAuthSource;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User authentication sources. There may be multiple entries associated with a single UserAccount.
 */
@Entity
@Table(
    name = "evekit_dp_auth_source",
    indexes = {
        @Index(
            name = "accountIndex",
            columnList = "uid",
            unique = false),
        @Index(
            name = "sourceAndScreenIndex",
            columnList = "source, screenName",
            unique = false)
    })
@NamedQueries({
    @NamedQuery(
        name = "DataPlatformUserAuthSource.findByAcctAndSource",
        query = "SELECT c FROM DataPlatformUserAuthSource c where c.account = :account and c.source = :source"),
    @NamedQuery(
        name = "DataPlatformUserAuthSource.allSourcesByAcct",
        query = "SELECT c FROM DataPlatformUserAuthSource c where c.account = :account order by c.last desc"),
    @NamedQuery(
        name = "DataPlatformUserAuthSource.all",
        query = "SELECT c FROM DataPlatformUserAuthSource c"),
    @NamedQuery(
        name = "DataPlatformUserAuthSource.allBySourceAndScreenname",
        query = "SELECT c FROM DataPlatformUserAuthSource c where c.source = :source and c.screenName = :screenname"),
})
@ApiModel(
    description = "Authentication source for a user")
@JsonSerialize(
    typing = JsonSerialize.Typing.DYNAMIC)
public class DataPlatformUserAuthSource implements UserAuthSource {
  private static final Logger log = Logger.getLogger(DataPlatformUserAuthSource.class.getName());

  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "ekdp_seq")
  @SequenceGenerator(
      name = "ekdp_seq",
      initialValue = 100000,
      allocationSize = 10,
      sequenceName = "dp_sequence")
  @ApiModelProperty(
      value = "Unique source ID")
  @JsonProperty("sid")
  protected long sid;

  @ManyToOne
  @JoinColumn(
      name = "uid",
      referencedColumnName = "uid")
  @JsonProperty("account")
  private DataPlatformUserAccount account;

  @ApiModelProperty(
      value = "Name of authentication source")
  @JsonProperty("source")
  private String source;

  @ApiModelProperty(
      value = "Screen name for this source")
  @JsonProperty("screenName")
  private String screenName;

  @ApiModelProperty(
      value = "Source specific authentication details")
  @JsonProperty("details")
  @Lob
  @Column(
      length = 102400)
  private String details;

  @ApiModelProperty(
      value = "Last time (milliseconds UTC) this source was used to authenticate")
  @JsonProperty("last")
  private long last = -1;

  public DataPlatformUserAccount getUserAccount() {
    return account;
  }

  @Override
  public DataPlatformUserAccount getOwner() {
    return account;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public String getScreenName() {
    return screenName;
  }

  public void setScreenName(
      String screenName) {
    this.screenName = screenName;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(
      String details) {
    this.details = details;
  }

  public long getLast() {
    return last;
  }

  public void setLast(
      long last) {
    this.last = last;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((account == null) ? 0 : account.hashCode());
    result = prime * result + ((details == null) ? 0 : details.hashCode());
    result = prime * result + (int) (last ^ (last >>> 32));
    result = prime * result + ((screenName == null) ? 0 : screenName.hashCode());
    result = prime * result + (int) (sid ^ (sid >>> 32));
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    return result;
  }

  @Override
  public boolean equals(
      Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DataPlatformUserAuthSource other = (DataPlatformUserAuthSource) obj;
    if (account == null) {
      if (other.account != null) return false;
    } else if (!account.equals(other.account)) return false;
    if (details == null) {
      if (other.details != null) return false;
    } else if (!details.equals(other.details)) return false;
    if (last != other.last) return false;
    if (screenName == null) {
      if (other.screenName != null) return false;
    } else if (!screenName.equals(other.screenName)) return false;
    if (sid != other.sid) return false;
    if (source == null) {
      if (other.source != null) return false;
    } else if (!source.equals(other.source)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "DataPlatformUserAuthSource [sid=" + sid + ", account=" + account + ", source=" + source + ", screenName=" + screenName + ", details=" + details
        + ", last=" + last + "]";
  }

  public static DataPlatformUserAuthSource getSource(
      final DataPlatformUserAccount acct,
      final String source) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<DataPlatformUserAuthSource> getter = DataPlatformProvider.getFactory()
                                                                                                       .getEntityManager()
                                                                                                       .createNamedQuery("DataPlatformUserAuthSource.findByAcctAndSource", DataPlatformUserAuthSource.class);
                                   getter.setParameter("account", acct);
                                   getter.setParameter("source", source);
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

  public static List<DataPlatformUserAuthSource> getAllSources(
      final DataPlatformUserAccount acct) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<DataPlatformUserAuthSource> getter = DataPlatformProvider.getFactory()
                                                                                                       .getEntityManager()
                                                                                                       .createNamedQuery("DataPlatformUserAuthSource.allSourcesByAcct", DataPlatformUserAuthSource.class);
                                   getter.setParameter("account", acct);
                                   return getter.getResultList();
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataPlatformUserAuthSource getLastUsedSource(
      final DataPlatformUserAccount acct) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<DataPlatformUserAuthSource> getter = DataPlatformProvider.getFactory()
                                                                                                       .getEntityManager()
                                                                                                       .createNamedQuery("DataPlatformUserAuthSource.allSourcesByAcct", DataPlatformUserAuthSource.class);
                                   getter.setParameter("account", acct);
                                   getter.setMaxResults(1);
                                   List<DataPlatformUserAuthSource> results = getter.getResultList();
                                   return results.isEmpty() ? null : results.get(0);
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static List<DataPlatformUserAuthSource> getAll() throws IOException {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<DataPlatformUserAuthSource> getter = DataPlatformProvider.getFactory()
                                                                                                       .getEntityManager()
                                                                                                       .createNamedQuery("DataPlatformUserAuthSource.all",
                                                                                                                         DataPlatformUserAuthSource.class);
                                   return getter.getResultList();
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataPlatformUserAuthSource getBySourceScreenname(
      final String source,
      final String screenName) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<DataPlatformUserAuthSource> getter = DataPlatformProvider.getFactory()
                                                                                                       .getEntityManager()
                                                                                                       .createNamedQuery("DataPlatformUserAuthSource.allBySourceAndScreenname", DataPlatformUserAuthSource.class);
                                   getter.setParameter("source", source);
                                   getter.setParameter("screenname", screenName);
                                   getter.setMaxResults(1);
                                   List<DataPlatformUserAuthSource> results = getter.getResultList();
                                   return results.isEmpty() ? null : results.get(0);
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataPlatformUserAuthSource updateAccount(
      final DataPlatformUserAuthSource src,
      final DataPlatformUserAccount newAccount) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   DataPlatformUserAuthSource result = getSource(src.getUserAccount(), src.getSource());
                                   if (result == null)
                                     throw new IOException("Input source could not be found: " + src.toString());
                                   DataPlatformUserAccount account = DataPlatformUserAccount.getAccount(Long.valueOf(newAccount.getUid()));
                                   if (account == null)
                                     throw new IOException("New account could not be found: " + newAccount.getUid());
                                   result.account = newAccount;
                                   return DataPlatformProvider.getFactory()
                                                              .getEntityManager()
                                                              .merge(result);
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataPlatformUserAuthSource createSource(
      final DataPlatformUserAccount owner,
      final String source,
      final String screenName,
      final String details) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   DataPlatformUserAuthSource result = getSource(owner, source);
                                   if (result != null) return result;
                                   result = new DataPlatformUserAuthSource();
                                   result.account = owner;
                                   result.source = source;
                                   result.setScreenName(screenName);
                                   result.setDetails(details);
                                   result.setLast(OrbitalProperties.getCurrentTime());
                                   return DataPlatformProvider.getFactory()
                                                              .getEntityManager()
                                                              .merge(result);
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static void removeSourceIfExists(
      final DataPlatformUserAccount owner,
      final String source) {
    try {
      DataPlatformProvider.getFactory()
                          .runTransaction(() -> {
                            DataPlatformUserAuthSource result = getSource(owner, source);
                            if (result != null) DataPlatformProvider.getFactory()
                                                                    .getEntityManager()
                                                                    .remove(result);
                          });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
  }

  public static DataPlatformUserAuthSource touch(
      final DataPlatformUserAuthSource source) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   DataPlatformUserAuthSource result = getSource(source.getUserAccount(), source.getSource());
                                   if (result == null)
                                     throw new IOException("Input source could not be found: " + source.toString());
                                   result.setLast(OrbitalProperties.getCurrentTime());
                                   return DataPlatformProvider.getFactory()
                                                              .getEntityManager()
                                                              .merge(result);
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  @Override
  public String getBody() {
    return details;
  }

  @Override
  public void touch() {
    touch(this);
  }

  @Override
  public void updateAccount(
      UserAccount existing) {
    assert existing instanceof DataPlatformUserAccount;
    updateAccount(this, (DataPlatformUserAccount) existing);
  }

  @Override
  public Date getLastSignOn() {
    return new Date(last);
  }

}
