package enterprises.orbital.evekit.dataplatform;

import com.fasterxml.jackson.annotation.JsonProperty;
import enterprises.orbital.base.OrbitalProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
 * Generic update tracker for evekit data sources.  Each tracker has a unique ID, a data source ID,
 * an source-specific "update type" ID, a start datetime, an end datetime, and a status.  Each data source
 * may create as many open (i.e. started but not finished) trackers as it deems necessary.  This
 * table simply keeps a history of updates.
 */
@Entity
@Table(
    name = "evekit_dp_tracker",
    indexes = {
        @Index(
            name = "trackerIndex",
            columnList = "tid",
            unique = true),
        @Index(
            name = "trackerSourceIndex",
            columnList = "tid, sid",
            unique = true),
        @Index(
            name = "finishedIndex",
            columnList = "tid, sid, dataSourceType, trackerEnd",
            unique = false)
    })
@NamedQueries({
    @NamedQuery(
        name = "DataSourceUpdateTracker.get",
        query = "SELECT c FROM DataSourceUpdateTracker c where c.source = :source and c.tid = :tid"),
    @NamedQuery(
        name = "DataSourceUpdateTracker.getUnfinished",
        query = "SELECT c FROM DataSourceUpdateTracker c where c.source = :source and c.dataSourceType = :dtype and c.trackerEnd = -1"),
    @NamedQuery(
        name = "DataSourceUpdateTracker.getAllUnfinished",
        query = "SELECT c FROM DataSourceUpdateTracker c where c.source = :source and c.trackerEnd = -1"),
    @NamedQuery(
        name = "DataSourceUpdateTracker.getLatestFinished",
        query = "SELECT c FROM DataSourceUpdateTracker c where c.source = :source and c.dataSourceType = :dtype and c.trackerEnd <> -1 order by c.trackerEnd desc"),
    @NamedQuery(
        name = "DataSourceUpdateTracker.getAllLatestFinished",
        query = "SELECT c FROM DataSourceUpdateTracker c where c.source = :source and c.trackerEnd <> -1 order by c.trackerEnd desc"),
})
@ApiModel(description = "EveKit Data Source Update Tracker")
public class DataSourceUpdateTracker {
  private static final Logger log = Logger.getLogger(DataSourceUpdateTracker.class.getName());

  // Status of an update
  public enum UpdateStatus {
    NOT_STARTED, // this update has not started yet
    FINISHED, // this update is finished
    ERROR, // this update finished with an error
    OTHER // this update finished with some other data source specific status
  }

  // Unique tracker ID
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
      value = "Uniquer update tracker ID")
  @JsonProperty("tid")
  private long       tid;
  // Unique data source to which this tracker is attached
  @ManyToOne
  @JoinColumn(
      name = "sid",
      referencedColumnName = "sid")
  @ApiModelProperty(value = "Data source to which this tracker is attached")
  @JsonProperty("account")
  private DataSource source;
  // Tracker start time
  @ApiModelProperty(
      value = "Tracker start time (milliseconds UTC)")
  @JsonProperty("trackerStart")
  protected long trackerStart = -1;
  // Tracker end time
  @ApiModelProperty(
      value = "Tracker end time (milliseconds UTC)")
  @JsonProperty("trackerEnd")
  protected long trackerEnd   = -1;
  @ApiModelProperty(value = "Data source specific data type information")
  @JsonProperty("dataSourceType")
  protected String       dataSourceType;
  @ApiModelProperty(
      value = "Tracker status")
  @JsonProperty("trackerStatus")
  private   UpdateStatus trackerStatus;
  @ApiModelProperty(
      value = "Tracker detail message")
  @JsonProperty("trackerDetail")
  private   String       trackerDetail;

  public long getTid() {
    return tid;
  }

  public DataSource getSource() {
    return source;
  }

  public long getTrackerStart() {
    return trackerStart;
  }

  public long getTrackerEnd() {
    return trackerEnd;
  }

  public String getDataSourceType() {
    return dataSourceType;
  }

  public UpdateStatus getTrackerStatus() {
    return trackerStatus;
  }

  public String getTrackerDetail() {
    return trackerDetail;
  }

  public void setTrackerStart(long trackerStart) {
    this.trackerStart = trackerStart;
  }

  public void setTrackerEnd(long trackerEnd) {
    this.trackerEnd = trackerEnd;
  }

  public void setTrackerStatus(UpdateStatus trackerStatus) {
    this.trackerStatus = trackerStatus;
  }

  public void setTrackerDetail(String trackerDetail) {
    this.trackerDetail = trackerDetail;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DataSourceUpdateTracker that = (DataSourceUpdateTracker) o;

    if (tid != that.tid) return false;
    if (trackerStart != that.trackerStart) return false;
    if (trackerEnd != that.trackerEnd) return false;
    if (!source.equals(that.source)) return false;
    if (dataSourceType != null ? !dataSourceType.equals(that.dataSourceType) : that.dataSourceType != null)
      return false;
    if (trackerStatus != that.trackerStatus) return false;
    return trackerDetail != null ? trackerDetail.equals(that.trackerDetail) : that.trackerDetail == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (tid ^ (tid >>> 32));
    result = 31 * result + source.hashCode();
    result = 31 * result + (int) (trackerStart ^ (trackerStart >>> 32));
    result = 31 * result + (int) (trackerEnd ^ (trackerEnd >>> 32));
    result = 31 * result + (dataSourceType != null ? dataSourceType.hashCode() : 0);
    result = 31 * result + trackerStatus.hashCode();
    result = 31 * result + (trackerDetail != null ? trackerDetail.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DataSourceUpdateTracker{" +
        "tid=" + tid +
        ", source=" + source +
        ", trackerStart=" + trackerStart +
        ", trackerEnd=" + trackerEnd +
        ", dataSourceType='" + dataSourceType + '\'' +
        ", trackerStatus=" + trackerStatus +
        ", trackerDetail='" + trackerDetail + '\'' +
        '}';
  }

  public static DataSourceUpdateTracker createTracker(final DataSource source, final String dtype) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        DataSourceUpdateTracker tracker = new DataSourceUpdateTracker();
        tracker.source = source;
        tracker.dataSourceType = dtype;
        tracker.trackerStatus = UpdateStatus.NOT_STARTED;
        return DataPlatformProvider.getFactory().getEntityManager().merge(tracker);
      });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataSourceUpdateTracker finishTracker(final DataSourceUpdateTracker tracker,
                                                      final UpdateStatus status,
                                                      final String msg) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        tracker.setTrackerEnd(OrbitalProperties.getCurrentTime());
        tracker.setTrackerStatus(status);
        tracker.setTrackerDetail(msg);
        return DataPlatformProvider.getFactory().getEntityManager().merge(tracker);
      });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataSourceUpdateTracker updateTracker(final DataSourceUpdateTracker tracker) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        return DataPlatformProvider.getFactory().getEntityManager().merge(tracker);
      });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataSourceUpdateTracker get(final DataSource source, final long tid) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        TypedQuery<DataSourceUpdateTracker> getter = DataPlatformProvider.getFactory().getEntityManager().createNamedQuery(
            "DataSourceUpdateTracker.get", DataSourceUpdateTracker.class);
        getter.setParameter("source", source);
        getter.setParameter("tid", tid);
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

  public static DataSourceUpdateTracker getUnfinishedTracker(final DataSource source, final String dtype) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        TypedQuery<DataSourceUpdateTracker> getter = DataPlatformProvider.getFactory().getEntityManager().createNamedQuery(
            "DataSourceUpdateTracker.getUnfinished", DataSourceUpdateTracker.class);
        getter.setParameter("source", source);
        getter.setParameter("dtype", dtype);
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

  public static List<DataSourceUpdateTracker> getAllUnfinishedTracker(final DataSource source) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        TypedQuery<DataSourceUpdateTracker> getter = DataPlatformProvider.getFactory().getEntityManager().createNamedQuery(
            "DataSourceUpdateTracker.getAllUnfinished", DataSourceUpdateTracker.class);
        getter.setParameter("source", source);
        return getter.getResultList();
      });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

  public static DataSourceUpdateTracker getLatestFinishedTrackers(final DataSource source, final String dtype) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        TypedQuery<DataSourceUpdateTracker> getter = DataPlatformProvider.getFactory().getEntityManager().createNamedQuery(
            "DataSourceUpdateTracker.getLatestFinished", DataSourceUpdateTracker.class);
        getter.setParameter("source", source);
        getter.setParameter("dtype", dtype);
        getter.setMaxResults(1);
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

  public static List<DataSourceUpdateTracker> getAllLatestFinishedTrackers(final DataSource source) {
    try {
      return DataPlatformProvider.getFactory().runTransaction(() -> {
        TypedQuery<DataSourceUpdateTracker> getter = DataPlatformProvider.getFactory().getEntityManager().createNamedQuery(
            "DataSourceUpdateTracker.getAllLatestFinished", DataSourceUpdateTracker.class);
        getter.setParameter("source", source);
        return getter.getResultList();
      });
    } catch (Exception e) {
      log.log(Level.SEVERE, "query error", e);
    }
    return null;
  }

}
