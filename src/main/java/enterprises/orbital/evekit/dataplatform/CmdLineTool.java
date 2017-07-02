package enterprises.orbital.evekit.dataplatform;

import enterprises.orbital.base.OrbitalProperties;
import enterprises.orbital.evekit.account.ESITokenManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Command line tool for use by data sources which don't necessarily run in java.
 */
public class CmdLineTool {

  protected static void finish(String msg, boolean stderr, int status) {
    if (stderr)
      System.err.println(msg);
    else
      System.out.println(msg);
    System.exit(status);
  }

  protected static void usage() {
    String usageString =
        "Usage: ekdptool -h\n" +
        "       ekdptool source create <name> <description>\n" +
        "       ekdptool source list\n" +
        "       ekdptool source -s <sid> start [<timestamp>]\n" +
        "       ekdptool source -s <sid> stop [<timestamp>]\n" +
        "       ekdptool source -s <sid> enable\n" +
        "       ekdptool source -s <sid> disable\n" +
        "       ekdptool source -s <sid> show\n" +
        "       ekdptool tracker -s <sid> create <type>\n" +
        "       ekdptool tracker -s <sid> [-d <dtype>] unfinished\n" +
        "       ekdptool tracker -s <sid> [-d <dtype>] last\n" +
        "       ekdptool tracker -s <sid> -t <tid> show\n" +
        "       ekdptool tracker -s <sid> -t <tid> start [<timestamp>]\n" +
        "       ekdptool tracker -s <sid> -t <tid> end [<timestamp>]\n" +
        "       ekdptool tracker -s <sid> -t <tid> status <NOT_STARTED|FINISHED|ERROR|OTHER> [msg]\n" +
        "       ekdptool token -k <kid> refresh -s <seconds>";
    finish(usageString, true, 1);
  }

  public static final void main(String[] argv) throws IOException {
    // Populate properties
    OrbitalProperties.addPropertyFile("EveKitDataPlatform.properties");
    if (!hasRequiredLength(1, 0, argv)) usage();
    // Process arguments
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("tracker")) {
        i += cmdTracker(Arrays.copyOfRange(argv, i + 1, argv.length));
      } else if (argv[i].equals("token")) {
        i += cmdToken(Arrays.copyOfRange(argv, i + 1, argv.length));
      } else if (argv[i].equals("source")) {
        i += cmdSource(Arrays.copyOfRange(argv, i + 1, argv.length));
      } else
        usage();
    }
    System.exit(0);
  }

  protected static boolean hasRequiredLength(int count, int index, String[] args) {
    return args.length - index >= count;
  }

  protected static int cmdSource(String[] argv) {
    // Check for create command
    int i = 0;
    if (!hasRequiredLength(1, i, argv)) usage();
    if (argv[i].equals("create")) {
      i++;
      if (!hasRequiredLength(2, i, argv)) usage();
      String     name        = argv[i++];
      String     description = argv[i++];
      DataSource newSource   = DataSource.createSource();
      if (newSource == null) finish("Error creating new source", true, 1);
      newSource.setName(name);
      newSource.setDescription(description);
      newSource = DataSource.update(newSource);
      finish(String.valueOf(newSource.getSid()), false, 0);
    } else if (argv[i].equals("list")) {
      i++;
      for (DataSource next : DataSource.getAll()) {
        System.out.println(next.toString());
      }
    } else {
      // First argument must always be the source ID
      if (!hasRequiredLength(2, i, argv) || !argv[i].equals("-s")) usage();
      i++;
      long sourceID = Long.valueOf(argv[i]);
      i++;
      // Retrieve DataSource object, fail if not found
      DataSource source = DataSource.get(sourceID);
      if (source == null) finish("Failed to find source with ID: " + sourceID, true, 1);
      // Parse remainder of command
      if (!hasRequiredLength(1, i, argv)) usage();
      switch (argv[i]) {
        case "start":
          i++;
          long startTime = hasRequiredLength(1, i, argv) ? Long.valueOf(argv[i++]) : OrbitalProperties.getCurrentTime();
          source.setStartDate(startTime);
          source = DataSource.update(source);
          if (source == null) finish("Internal error updating source", true, 1);
          break;

        case "stop":
          i++;
          long stopTime = hasRequiredLength(1, i, argv) ? Long.valueOf(argv[i++]) : OrbitalProperties.getCurrentTime();
          source.setEndDate(stopTime);
          source = DataSource.update(source);
          if (source == null) finish("Internal error updating source", true, 1);
          break;

        case "enable":
          i++;
          source.setEnabled(true);
          source = DataSource.update(source);
          if (source == null) finish("Internal error updating source", true, 1);
          break;

        case "disable":
          i++;
          source.setEnabled(false);
          source = DataSource.update(source);
          if (source == null) finish("Internal error updating source", true, 1);
          break;

        case "show":
          i++;
          finish(source.toString(), false, 0);
          break;

        default:
          usage();
      }
    }
    return i;
  }

  protected static int cmdTracker(String[] argv) {
    // First argument must always be the source ID
    int i = 0;
    if (!hasRequiredLength(2, i, argv) || !argv[i].equals("-s")) usage();
    i++;
    long sourceID = Long.valueOf(argv[i]);
    i++;
    // Retrieve DataSource object, fail if not found
    DataSource source = DataSource.get(sourceID);
    if (source == null) finish("Failed to find source with ID: " + sourceID, true, 1);
    // Parse remainder of command
    for ( ; i < argv.length; i++) {
      if (argv[i].equals("create")) {
        i++;
        if (!hasRequiredLength(1, i, argv)) usage();
        String typeInfo = argv[i];
        i++;
        DataSourceUpdateTracker newTracker = DataSourceUpdateTracker.createTracker(source, typeInfo);
        if (newTracker == null) finish("Internal error creating new tracker", true, 1);
        finish(String.valueOf(newTracker.getTid()), false, 0);
      } else if (argv[i].equals("-d")) {
        // data source type included in query
        i++;
        if (!hasRequiredLength(1, i, argv)) usage();
        String dType = argv[i++];
        if (!hasRequiredLength(1, i, argv)) usage();
        if (argv[i].equals("unfinished")) {
          i++;
          DataSourceUpdateTracker unfinished = DataSourceUpdateTracker.getUnfinishedTracker(source, dType);
          if (unfinished != null) System.out.println(unfinished.toString());
        } else if (argv[i].equals("last")) {
          i++;
          DataSourceUpdateTracker last = DataSourceUpdateTracker.getLatestFinishedTrackers(source, dType);
          if (last != null) System.out.println(last.toString());
        } else
          usage();
      } else if (argv[i].equals("unfinished")) {
        i++;
        List<DataSourceUpdateTracker> unfinished = DataSourceUpdateTracker.getAllUnfinishedTracker(source);
        if (unfinished != null) {
          for (DataSourceUpdateTracker next : unfinished) {
            System.out.println(next.toString());
          }
        }
      } else if (argv[i].equals("last")) {
        i++;
        List<DataSourceUpdateTracker> last = DataSourceUpdateTracker.getAllLatestFinishedTrackers(source);
        if (last != null) {
          for (DataSourceUpdateTracker next : last) {
            System.out.println(next.toString());
          }
        }
      } else if (argv[i].equals("-t")) {
        i++;
        if (!hasRequiredLength(1, i, argv)) usage();
        long trackerID = Long.valueOf(argv[i]);
        i++;
        // Retrieve DataSourceUpdateTracker, fail if not found
        DataSourceUpdateTracker curTracker = DataSourceUpdateTracker.get(source, trackerID);
        if (curTracker == null) finish("Failed to find tracker with ID: " + trackerID, true, 1);
        if (!hasRequiredLength(1, i, argv)) usage();
        switch (argv[i]) {
          case "show":
            i++;
            finish(curTracker.toString(), false, 0);
            break;

          case "start":
            i++;
            long startTime = hasRequiredLength(1, i, argv) ? Long.valueOf(argv[i++]) : OrbitalProperties.getCurrentTime();
            curTracker.setTrackerStart(startTime);
            curTracker = DataSourceUpdateTracker.updateTracker(curTracker);
            if (curTracker == null) finish("Internal error updating tracker", true, 1);
            break;

          case "end":
            i++;
            long endTime = hasRequiredLength(1, i, argv) ? Long.valueOf(argv[i++]) : OrbitalProperties.getCurrentTime();
            curTracker.setTrackerEnd(endTime);
            curTracker = DataSourceUpdateTracker.updateTracker(curTracker);
            if (curTracker == null) finish("Internal error updating tracker", true, 1);
            break;

          case "status":
            i++;
            if (!hasRequiredLength(1, i, argv)) usage();
            DataSourceUpdateTracker.UpdateStatus status = DataSourceUpdateTracker.UpdateStatus.valueOf(argv[i++]);
            String msg = hasRequiredLength(1, i, argv) ? argv[i++] : "";
            curTracker.setTrackerStatus(status);
            curTracker.setTrackerDetail(msg);
            curTracker = DataSourceUpdateTracker.updateTracker(curTracker);
            if (curTracker == null) finish("Internal error updating tracker", true, 1);
            break;

          default:
            usage();
        }
      } else
        usage();
    }
    return i;
  }

  protected static int cmdToken(String[] argv) {
    // First argument must always be the ESI token ID
    int i = 0;
    if (!hasRequiredLength(2, 0, argv) || !argv[i].equals("-k")) usage();
    i++;
    long tokenID = Long.valueOf(argv[i]);
    i++;
    for ( ; i < argv.length; i++) {
      if (argv[i].equals("refresh")) {
        i++;
        if (!hasRequiredLength(2, i, argv) || !argv[i].equals("-s")) usage();
        i++;
        long seconds = Long.valueOf(argv[i]);
        i++;
        String token = null;
        try {
          token = ESITokenManager.refreshToken(tokenID, seconds * 1000L,
                                               OrbitalProperties.getGlobalProperty(
                                                   DataPlatformProvider.PROP_EVE_TOKEN_CLIENT_ID),
                                               OrbitalProperties.getGlobalProperty(
                                                          DataPlatformProvider.PROP_EVE_TOKEN_SECRET_KEY));
        } catch (IOException e) {
          finish("IO exception while refreshing token: " + e.getStackTrace(), true, 1);
        }
        finish(token, false, 0);
      } else
        usage();
    }
    return i;
  }

}
