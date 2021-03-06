package gov.usgs.volcanoes.vdx.in.tilt;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.data.GenericDataMatrix;
import gov.usgs.volcanoes.core.legacy.Arguments;
import gov.usgs.volcanoes.core.legacy.pinnacle.Client;
import gov.usgs.volcanoes.core.legacy.pinnacle.StatusBlock;
import gov.usgs.volcanoes.core.util.ResourceReader;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.vdx.data.tilt.SQLTiltDataSource;
import gov.usgs.volcanoes.vdx.db.VDXDatabase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import pinnacle data - from pinnacle server socket or from files.
 *
 * @author Dan Cervelli
 */
public class ImportPinnServer extends Client {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImportPinnServer.class);
  private static final String CONFIG_FILE = "PinnClient.config";
  private SQLTiltDataSource dataSource;
  private String channel;

  /**
   * Constructor.
   *
   * @param h host
   * @param p port
   */
  public ImportPinnServer(String h, int p) {
    super(h, p);
  }

  /**
   * Fabric method to create instance.
   *
   * @param fn configuration file name
   */
  public static ImportPinnServer createImportPinnServer(String fn) {
    if (fn == null) {
      fn = CONFIG_FILE;
    }
    ConfigFile cf = new ConfigFile(fn);
    String host = cf.getString("server.host");
    int port = StringUtils.stringToInt(cf.getString("server.port"), 17000);
    ImportPinnServer ips = new ImportPinnServer(host, port);

    ips.channel = cf.getString("channel");

    String driver = cf.getString("vdx.driver");
    String url = cf.getString("vdx.url");
    String prefix = cf.getString("vdx.prefix");
    if (prefix == null) {
      throw new RuntimeException("can't find config parameter vdx.prefix");
    }

    String name = cf.getString("vdx.name");

    ips.dataSource = new SQLTiltDataSource();

    VDXDatabase database = new VDXDatabase(driver, url, prefix);
    // TODO: work out new initialization
    // ips.dataSource.setDatabase(database);
    // ips.dataSource.setName(name);

    if (!ips.dataSource.databaseExists()) {
      if (ips.dataSource.createDatabase()) {
        ips.LOGGER.info("created database.");
      }
    }

    // this is commented out for now until i figure out how i want to handle channel existence
    // if (!ips.dataSource.defaultChannelExists("etilt", ips.channel)) {
    if (ips.dataSource
        .createChannel(ips.channel, ips.channel, Double.NaN, Double.NaN, Double.NaN, 1, 0, 0)) {
      ips.LOGGER.info("created channel.");
    }
    // }
    return ips;
  }

  /**
   * Method to handle with data block which was got from pinnacle server.
   *
   * @see gov.usgs.volcanoes.core.legacy.pinnacle.Client
   */
  public void handleStatusBlock(StatusBlock sb) {
    System.out.println(sb);
    DoubleMatrix2D dm = DoubleFactory2D.dense.make(1, 5);
    dm.setQuick(0, 0, sb.getJ2K());
    dm.setQuick(0, 1, sb.getXMillis());
    dm.setQuick(0, 2, sb.getYMillis());
    dm.setQuick(0, 3, sb.getTemperature());
    dm.setQuick(0, 4, sb.getVoltage());
    String[] columnNames = {"j2ksec", "xTilt", "yTilt", "holeTemp", "instVolt"};
    GenericDataMatrix gdm = new GenericDataMatrix(dm);
    gdm.setColumnNames(columnNames);
    dataSource.defaultInsertData(channel, gdm, dataSource.getTranslationsFlag(),
        dataSource.getRanksFlag(), 1);
  }

  /**
   * Import from file which contains pinnacle blocks.
   *
   * @param fn file name
   */
  public void importFile(String fn) {
    try {
      ResourceReader rr = ResourceReader.getResourceReader(fn);
      if (rr == null) {
        return;
      }
      LOGGER.info("importing: {}", fn);

      String s = rr.nextLine();
      while (s != null) {
        if (s.substring(21, 24).equals("SB:")) {
          String sub = s.substring(25);
          int n = sub.length() / 2;
          byte[] buf = new byte[n];
          for (int i = 0; i < n; i++) {
            String ss = sub.substring(i * 2, i * 2 + 2);
            int j = Integer.parseInt(ss, 16);
            buf[i] = (byte) j;
          }
          StatusBlock sb = new StatusBlock(buf);
          handleStatusBlock(sb);
        }
        s = rr.nextLine();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Main method Syntax is:
   * java gov.usgs.volcanoes.vdx.data.tilt.ImportPinnServer [-c configFile] [files...]
   * If file names are given than import files, else connect and listen for pinnacle server
   *
   * @param as command line args
   */
  public static void main(String[] as) {
    String cf = null;
    Set<String> flags;
    Set<String> keys;

    flags = new HashSet<String>();
    keys = new HashSet<String>();
    keys.add("-c");

    Arguments args = new Arguments(as, flags, keys);

    if (args.contains("-c")) {
      cf = args.get("-c");
    }

    List<String> unusedArgs = args.unused();
    ImportPinnServer ips = ImportPinnServer.createImportPinnServer(cf);

    if (unusedArgs.size() > 0) {
      for (String file : unusedArgs) {
        ips.importFile(file);
      }
    } else {
      ips.startListening();
    }
  }
}
