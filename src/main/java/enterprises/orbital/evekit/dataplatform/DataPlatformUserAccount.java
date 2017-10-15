package enterprises.orbital.evekit.dataplatform;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import enterprises.orbital.base.OrbitalProperties;
import enterprises.orbital.base.PersistentPropertyKey;
import enterprises.orbital.oauth.UserAccount;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data platform user accounts.
 */
@Entity
@Table(
    name = "evekit_dp_users")
@NamedQueries({
    @NamedQuery(
        name = "DataPlatformUserAccount.findByUid",
        query = "SELECT c FROM DataPlatformUserAccount c where c.uid = :uid"),
    @NamedQuery(
        name = "DataPlatformUserAccount.allAccounts",
        query = "SELECT c FROM DataPlatformUserAccount c"),
})
@ApiModel(
    description = "Data platform user account")
@JsonSerialize(
    typing = JsonSerialize.Typing.DYNAMIC)
public class DataPlatformUserAccount implements UserAccount, PersistentPropertyKey<String> {
  private static final Logger log = Logger.getLogger(DataPlatformUserAccount.class.getName());

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
      value = "Unique user ID")
  @JsonProperty("uid")
  protected long uid;

  @ApiModelProperty(
      value = "Date (milliseconds UTC) when account was created")
  @JsonProperty("created")
  protected long created = -1;

  @ApiModelProperty(
      value = "True if user is an admin, false otherwise")
  @JsonProperty("admin")
  protected boolean admin;

  @ApiModelProperty(
      value = "Last time (milliseconds UTC) user logged in")
  @JsonProperty("last")
  protected long last = -1;

  @Override
  public boolean isDisabled() {
    return false;
  }

  public long getID() {
    return uid;
  }

  @Override
  public String getUid() {
    return String.valueOf(uid);
  }

  public long getCreated() {
    return created;
  }

  public void setCreated(
      long created) {
    this.created = created;
  }

  public boolean isAdmin() {
    return admin;
  }

  public void setAdmin(
      boolean admin) {
    this.admin = admin;
  }

  public long getLast() {
    return last;
  }

  public void setLast(
      long last) {
    this.last = last;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DataPlatformUserAccount that = (DataPlatformUserAccount) o;

    if (uid != that.uid) return false;
    if (created != that.created) return false;
    if (admin != that.admin) return false;
    return last == that.last;
  }

  @Override
  public int hashCode() {
    int result = (int) (uid ^ (uid >>> 32));
    result = 31 * result + (int) (created ^ (created >>> 32));
    result = 31 * result + (admin ? 1 : 0);
    result = 31 * result + (int) (last ^ (last >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "DataPlatformUserAccount{" +
        "uid=" + uid +
        ", created=" + created +
        ", admin=" + admin +
        ", last=" + last +
        '}';
  }

  /**
   * Create a new user account.
   *
   * @param admin true if this user should be created with administrative privileges.
   * @return the new DataPlatformUserAccount.
   */
  public static DataPlatformUserAccount createNewUserAccount(final boolean admin) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   DataPlatformUserAccount result = new DataPlatformUserAccount();
                                   result.created = OrbitalProperties.getCurrentTime();
                                   result.admin = admin;
                                   result.last = result.created;
                                   return DataPlatformProvider.getFactory()
                                                              .getEntityManager()
                                                              .merge(result);
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  /**
   * Retrieve the user account with the given id.
   *
   * @param uid the ID of the user account to retrieve.
   * @return the given UserAccount, or null if no such user exists.
   */
  public static DataPlatformUserAccount getAccount(final long uid) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<DataPlatformUserAccount> getter = DataPlatformProvider.getFactory()
                                                                                                    .getEntityManager()
                                                                                                    .createNamedQuery("DataPlatformUserAccount.findByUid",
                                                                                                                      DataPlatformUserAccount.class);
                                   getter.setParameter("uid", uid);
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

  /**
   * Update the "last" time for this user to the current time.
   *
   * @param user the UserAccount to update.
   * @return returns the newly persisted User.
   */
  public static DataPlatformUserAccount touch(final DataPlatformUserAccount user) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   DataPlatformUserAccount result = getAccount(user.uid);
                                   if (result == null)
                                     throw new IOException("No user found with UUID " + user.getUid());
                                   result.last = OrbitalProperties.getCurrentTime();
                                   return DataPlatformProvider.getFactory()
                                                              .getEntityManager()
                                                              .merge(result);
                                 });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  /**
   * Return list of all user accounts.
   *
   * @return the list of all current user accounts.
   */
  public static List<DataPlatformUserAccount> getAllAccounts() {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> DataPlatformProvider.getFactory()
                                                                           .getEntityManager()
                                                                           .createNamedQuery("DataPlatformUserAccount.allAccounts",
                                                                                             DataPlatformUserAccount.class)
                                                                           .getResultList());
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  @Override
  public void touch() {
    touch(this);
  }

  @Override
  public Date getJoinTime() {
    return new Date(created);
  }

  @Override
  public Date getLastSignOn() {
    return new Date(last);
  }

  @Override
  public String getPeristentPropertyKey(
      String field) {
    // Key scheme: DataPlatformUserAccount.<UUID>.<field>
    return "DataPlatformUserAccount." + String.valueOf(uid) + "." + field;
  }

  public static DataPlatformUserAccount update(
      final DataPlatformUserAccount data) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> DataPlatformProvider.getFactory()
                                                                           .getEntityManager()
                                                                           .merge(data));
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

}
