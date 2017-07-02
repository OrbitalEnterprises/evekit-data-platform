package enterprises.orbital.evekit.dataplatform;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry entry for an EveKit data source.
 */
@Entity
@Table(
    name = "evekit_dp_source",
    indexes = {
        @Index(
            name = "sourceIndex",
            columnList = "sid",
            unique = true
        )
    })
@NamedQueries({
    @NamedQuery(
        name = "DataSource.get",
        query = "SELECT c FROM DataSource c where c.sid = :sid"),
    @NamedQuery(
        name = "DataSource.getAll",
        query = "SELECT c FROM DataSource c")
})
@ApiModel(description = "EveKit Data Source Definition")
public class DataSource {
  protected static final Logger log = Logger.getLogger(DataSource.class.getName());

  // Unique source ID
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "ekdp_seq")
  @SequenceGenerator(
      name = "ekdp_seq",
      initialValue = 100000,
      allocationSize = 10,
      sequenceName = "dp_sequence")
  @JsonProperty("sid")
  private long   sid;
  // Name of source
  @JsonProperty("name")
  private String name;
  // Short description of source
  @Lob
  @Column(
      length = 102400)
  @JsonProperty("description")
  private String description;
  // Date when collection started
  @JsonProperty("startDate")
  private long startDate = -1;
  // Date when collection ended
  @JsonProperty("endDate")
  private long endDate = -1;
  // If true, then data source should continue collecting on schedule.
  @JsonProperty("enabled")
  private boolean enabled;

  public long getSid() { return sid; }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getStartDate() {
    return startDate;
  }

  public void setStartDate(long startDate) {
    this.startDate = startDate;
  }

  public long getEndDate() {
    return endDate;
  }

  public void setEndDate(long endDate) {
    this.endDate = endDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DataSource that = (DataSource) o;

    if (sid != that.sid) return false;
    if (startDate != that.startDate) return false;
    if (endDate != that.endDate) return false;
    if (enabled != that.enabled) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    return description != null ? description.equals(that.description) : that.description == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (sid ^ (sid >>> 32));
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (int) (startDate ^ (startDate >>> 32));
    result = 31 * result + (int) (endDate ^ (endDate >>> 32));
    result = 31 * result + (enabled ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DataSource{" +
        "sid=" + sid +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", startDate=" + startDate +
        ", endDate=" + endDate +
        ", enabled=" + enabled +
        '}';
  }

  public static DataSource createSource() {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
          DataSource result = new DataSource();
          return DataPlatformProvider.getFactory().getEntityManager().merge(result);
        });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataSource get(final long sid) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
          TypedQuery<DataSource> getter = DataPlatformProvider.getFactory().getEntityManager()
              .createNamedQuery("DataSource.get", DataSource.class);
          getter.setParameter("sid", sid);
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

  public static List<DataSource> getAll() {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
          TypedQuery<DataSource> getter = DataPlatformProvider.getFactory().getEntityManager()
              .createNamedQuery("DataSource.getAll", DataSource.class);
          return getter.getResultList();
        });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static boolean deleteSource(final long sid) {
    try {
      DataPlatformProvider.getFactory().runTransaction(() -> {
          DataSource source = get(sid);
          if (source != null)
            DataPlatformProvider.getFactory().getEntityManager().remove(source);
        });
      return true;
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return false;
  }

  public static DataSource update(final DataSource source) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
          return DataPlatformProvider.getFactory().getEntityManager().merge(source);
        });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
      return null;
    }
  }

}
