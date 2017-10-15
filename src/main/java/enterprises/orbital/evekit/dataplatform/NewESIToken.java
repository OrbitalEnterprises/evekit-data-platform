package enterprises.orbital.evekit.dataplatform;

import enterprises.orbital.base.OrbitalProperties;
import enterprises.orbital.base.Stamper;

import javax.persistence.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A temporary record storing a new token we intend to create.  These keys
 * are normally removed when the new access key completes setup.  If creation
 * fails for some reason, then a separate process will periodically remove
 * all stale keys.
 */
@Entity
@Table(
    name = "esi_temp_token",
    indexes = {
        @Index(
            name = "keyIndex",
            columnList = "kid",
            unique = true),
        @Index(
            name = "credIndex",
            columnList = "stateKey",
            unique = true)
    })
@NamedQueries({
    @NamedQuery(
        name = "NewESIToken.findByID",
        query = "SELECT c FROM NewESIToken c where c.kid = :kid"),
    @NamedQuery(
        name = "NewESIToken.findByCred",
        query = "SELECT c FROM NewESIToken c where c.stateKey = :cred"),
    @NamedQuery(
        name = "NewESIToken.getExpired",
        query = "SELECT c FROM NewESIToken c where c.expiry <= :expiry")
})
public class NewESIToken {
  protected static final Logger log = Logger.getLogger(NewESIToken.class.getName());

  protected static ThreadLocal<ByteBuffer> assembly = new ThreadLocal<ByteBuffer>() {
    @Override
    protected ByteBuffer initialValue() {
      // Since we use the user's account name in the hash we need to
      // allocate to the largest possible size allowed for a data store
      // string (which is currently 500 bytes).
      return ByteBuffer.allocate(550);
    }
  };

  // Unique temporary key ID
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "ekdp_seq")
  @SequenceGenerator(
      name = "ekdp_seq",
      initialValue = 100000,
      allocationSize = 10,
      sequenceName = "dp_sequence")
  private long kid;

  // OAuth UserAccount ID which this new key will be associated with
  @ManyToOne
  @JoinColumn(
      name = "uid",
      referencedColumnName = "uid")
  private DataPlatformUserAccount userAccount;

  // Time when request was created
  private long createTime;

  // Time when this request will expire
  private long expiry;

  // Named scopes requested for this key
  @Lob
  @Column(
      length = 102400)
  private String scopes;

  // Fixed at the time this key is created, we use this field to randomize the hash.
  private long randomSeed;

  // OAuth state presented as part of request
  @Lob
  @Column(
      length = 102400)
  private String stateKey;

  // Pre-existing key if this is a re-authentication request, otherwise -1
  private long existingKid = -1;

  public long getKid() {
    return kid;
  }

  public DataPlatformUserAccount getUserAccount() {
    return userAccount;
  }

  public long getCreateTime() {
    return createTime;
  }

  public long getExpiry() {
    return expiry;
  }

  public String getScopes() {
    return scopes;
  }

  public long getRandomSeed() {
    return randomSeed;
  }

  public String getStateKey() {
    return stateKey;
  }

  public long getExistingKid() { return existingKid; }

  public void setExistingKid(long pkid) { this.existingKid = pkid; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NewESIToken that = (NewESIToken) o;

    if (kid != that.kid) return false;
    if (createTime != that.createTime) return false;
    if (expiry != that.expiry) return false;
    if (randomSeed != that.randomSeed) return false;
    if (existingKid != that.existingKid) return false;
    if (!userAccount.equals(that.userAccount)) return false;
    return scopes.equals(that.scopes);
  }

  @Override
  public int hashCode() {
    int result = (int) (kid ^ (kid >>> 32));
    result = 31 * result + userAccount.hashCode();
    result = 31 * result + (int) (createTime ^ (createTime >>> 32));
    result = 31 * result + (int) (expiry ^ (expiry >>> 32));
    result = 31 * result + scopes.hashCode();
    result = 31 * result + (int) (randomSeed ^ (randomSeed >>> 32));
    result = 31 * result + (int) (existingKid ^ (existingKid >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "NewESIToken{" +
        "kid=" + kid +
        ", userAccount=" + userAccount +
        ", createTime=" + createTime +
        ", expiry=" + expiry +
        ", scopes='" + scopes + '\'' +
        ", randomSeed=" + randomSeed +
        ", stateKey='" + stateKey + '\'' +
        ", existingKid=" + existingKid +
        '}';
  }

  public static NewESIToken createKey(final DataPlatformUserAccount userAccount, final long createTime,
                                      final long expiry,
                                      final String scopes, final long existingKid) {
    NewESIToken newKey = null;
    try {
      // Generate and save the initial key
      newKey = DataPlatformProvider.getFactory()
                                   .runTransaction(() -> {
                                     long seed = new Random(OrbitalProperties.getCurrentTime()).nextLong();
                                     NewESIToken result = new NewESIToken();
                                     result.userAccount = userAccount;
                                     result.createTime = createTime;
                                     result.existingKid = existingKid;
                                     result.expiry = expiry;
                                     result.scopes = scopes;
                                     result.randomSeed = seed;
                                     return DataPlatformProvider.getFactory()
                                                                .getEntityManager()
                                                                .merge(result);
                                   });
      // If successful, then set the hash on the key and return it.  We need this in the
      // database since this is what we'll select when the OAuth pass completes.
      final NewESIToken tempKey = newKey;
      newKey = DataPlatformProvider.getFactory()
                                   .runTransaction(() -> {
                                     TypedQuery<NewESIToken> getter = DataPlatformProvider.getFactory()
                                                                                          .getEntityManager()
                                                                                          .createNamedQuery("NewESIToken.findByID", NewESIToken.class);
                                     getter.setParameter("kid", tempKey.kid);
                                     try {
                                       NewESIToken result = getter.getSingleResult();
                                       result.stateKey = generateHash(result);
                                       return DataPlatformProvider.getFactory()
                                                                  .getEntityManager()
                                                                  .merge(result);
                                     } catch (NoResultException e) {
                                       return null;
                                     }
                                   });

    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return newKey;
  }

  public static NewESIToken getKeyByID(final long kid) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<NewESIToken> getter = DataPlatformProvider.getFactory()
                                                                                        .getEntityManager()
                                                                                        .createNamedQuery("NewESIToken.findByID", NewESIToken.class);
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

  public static NewESIToken getKeyByState(final String state) {
    try {
      return DataPlatformProvider.getFactory()
                                 .runTransaction(() -> {
                                   TypedQuery<NewESIToken> getter = DataPlatformProvider.getFactory()
                                                                                        .getEntityManager()
                                                                                        .createNamedQuery("NewESIToken.findByCred", NewESIToken.class);
                                   getter.setParameter("cred", state);
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

  public static void cleanExpired(final long limit) {
    try {
      DataPlatformProvider.getFactory()
                          .runTransaction(() -> {
                            TypedQuery<NewESIToken> getter = DataPlatformProvider.getFactory()
                                                                                 .getEntityManager()
                                                                                 .createNamedQuery("NewESIToken.getExpired", NewESIToken.class);
                            getter.setParameter("expiry", limit);
                            for (NewESIToken next : getter.getResultList()) {
                              DataPlatformProvider.getFactory()
                                                  .getEntityManager()
                                                  .remove(next);
                            }
                          });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
  }

  public static boolean deleteKey(final long kid) {
    try {
      DataPlatformProvider.getFactory()
                          .runTransaction(() -> {
                            NewESIToken key = getKeyByID(kid);
                            if (key != null) DataPlatformProvider.getFactory()
                                                                 .getEntityManager()
                                                                 .remove(key);
                          });
      return true;
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return false;
  }

  private static String generateHash(NewESIToken ref) {
    ByteBuffer assemble = assembly.get();
    assemble.clear();

    assemble.putLong(ref.kid);
    assemble.putLong(ref.getRandomSeed());
    assemble.limit(assemble.position());
    assemble.rewind();

    return Stamper.digest(assemble);
  }

}
