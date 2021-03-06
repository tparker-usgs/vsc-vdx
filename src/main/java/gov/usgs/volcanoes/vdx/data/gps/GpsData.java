package gov.usgs.volcanoes.vdx.data.gps;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

import gov.usgs.volcanoes.core.data.BinaryDataSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that deals with GPS data.  The data are stored in four matrices: an Nx1 time matrix, an
 * Nx1 rank matrix, a (3*N)x1 position matrix, and a sparse (3*N)x(3*N) covariance matrix, where N
 * is the number of observations.
 *
 * @author Dan Cervelli
 */
public class GpsData implements BinaryDataSet {

  private static final Logger LOGGER = LoggerFactory.getLogger(GpsData.class);
  private static final DoubleFactory2D DENSE = DoubleFactory2D.dense;
  private static final DoubleFactory2D SPARSE = DoubleFactory2D.sparse;
  private static final DoubleMatrix2D I3X3 = DENSE.identity(3);
  private static final DoubleMatrix2D ZERO3X3 = DENSE.make(3, 3);
  private static final double SECONDSPERYEAR = 31557600;


  private DoubleMatrix2D tdata;
  private DoubleMatrix2D rdata;
  private DoubleMatrix2D xyzData;
  private DoubleMatrix2D covData;
  private DoubleMatrix2D lenData;

  /**
   * Generic empty constructor.
   */
  public GpsData() {
  }

  /**
   * Constructor.
   *
   * @param pts list of DataPoints
   */
  public GpsData(List pts) {
    setToList(pts);
  }

  /**
   * Initialize internal matrices from list of DataPoints.
   *
   * @param pts list of DataPoints
   */
  public void setToList(List pts) {
    int rows = pts.size();
    DataPoint origin = (DataPoint) pts.get(0);
    boolean hasLen = !Double.isNaN(origin.len);
    tdata = DENSE.make(rows, 1);
    rdata = DENSE.make(rows, 1);
    xyzData = DENSE.make(rows, 3);
    covData = DENSE.make(rows, 6);
    lenData = DENSE.make(rows, 1);
    for (int i = 0; i < rows; i++) {
      DataPoint dp = (DataPoint) pts.get(i);
      // set times
      tdata.setQuick(i, 0, dp.timeVal);

      // set ranks
      rdata.setQuick(i, 0, dp.rankVal);

      // set xyz
      xyzData.setQuick(i, 0, dp.xcoord);
      xyzData.setQuick(i, 1, dp.ycoord);
      xyzData.setQuick(i, 2, dp.zcoord);

      // set sigmas
      covData.setQuick(i, 0, dp.sxx);
      covData.setQuick(i, 1, dp.syy);
      covData.setQuick(i, 2, dp.szz);
      covData.setQuick(i, 3, dp.sxy);
      covData.setQuick(i, 4, dp.sxz);
      covData.setQuick(i, 5, dp.syz);

      //set length
      if (hasLen) {
        lenData.setQuick(i, 0, dp.len);
      } else {
        double dx = dp.xcoord - origin.xcoord;
        double dy = dp.ycoord - origin.ycoord;
        double dz = dp.zcoord - origin.zcoord;
        lenData.setQuick(i, 0, Math.sqrt(dx * dx + dy * dy + dz * dz));
      }
    }
  }

  /**
   * Get binary GpsData representation.
   *
   * @return ByteBuffer of binary GpsData
   */
  public ByteBuffer toBinary() {
    int rows = observations();
    ByteBuffer bb = ByteBuffer.allocate(4 + rows * 12 * 8);
    bb.putInt(rows);
    for (int i = 0; i < rows; i++) {
      bb.putDouble(tdata.getQuick(i, 0)); // timeVal

      bb.putDouble(rdata.getQuick(i, 0)); // rankVal

      for (int j = 0; j < 3; j++) {
        bb.putDouble(xyzData.getQuick(i, j));
      }
      for (int j = 0; j < 6; j++) {
        bb.putDouble(covData.getQuick(i, j));
      }

      bb.putDouble(lenData.getQuick(i, 0)); // len
    }
    return bb;
  }

  /**
   * Initialize GpsData from binary representation.
   *
   * @param bb ByteBuffer of GpsData
   */
  public void fromBinary(ByteBuffer bb) {
    int rows = bb.getInt();
    tdata = DENSE.make(rows, 1);
    rdata = DENSE.make(rows, 1);
    xyzData = DENSE.make(rows, 3);
    covData = DENSE.make(rows, 6);
    lenData = DENSE.make(rows, 1);
    DataPoint dp = new DataPoint();

    for (int i = 0; i < rows; i++) {
      dp.fromBinary(bb);
      // set times
      tdata.setQuick(i, 0, dp.timeVal);

      // set ranks
      rdata.setQuick(i, 0, dp.rankVal);

      // set xyz
      xyzData.setQuick(i, 0, dp.xcoord);
      xyzData.setQuick(i, 1, dp.ycoord);
      xyzData.setQuick(i, 2, dp.zcoord);

      // set sigmas
      covData.setQuick(i, 0, dp.sxx);
      covData.setQuick(i, 1, dp.syy);
      covData.setQuick(i, 2, dp.szz);
      covData.setQuick(i, 3, dp.sxy);
      covData.setQuick(i, 4, dp.sxz);
      covData.setQuick(i, 5, dp.syz);

    }
  }

  /**
   * Sets the data matrices.
   *
   * @param t the time matrix
   * @param xyz the position matrix
   * @param cov the covariance matrix
   */
  public void setData(DoubleMatrix2D t, DoubleMatrix2D r, DoubleMatrix2D xyz, DoubleMatrix2D cov) {
    tdata = t;
    rdata = r;
    xyzData = xyz;
    covData = cov;
  }

  /**
   * Gets the number of observations.
   *
   * @return the number of observations
   */
  public int observations() {
    return tdata.rows();
  }

  /**
   * Adds a value to the time column (for time zone management).
   *
   * @param adj the time adjustment
   */
  public void adjustTime(double adj) {
    for (int i = 0; i < tdata.rows(); i++) {
      tdata.setQuick(i, 0, tdata.getQuick(i, 0) + adj);
    }
  }

  /**
   * Gets the time matrix.
   *
   * @return the time matrix
   */
  public DoubleMatrix2D getTimes() {
    return tdata;
  }

  /**
   * Gets the rank matrix.
   *
   * @return the rank matrix
   */
  public DoubleMatrix2D getRanks() {
    return rdata;
  }

  /**
   * Gets the position matrix.
   *
   * @return the position matrix
   */
  public DoubleMatrix2D getXyz() {
    return xyzData;
  }

  /**
   * Gets the covariance matrix.
   *
   * @return the covariance matrix
   */
  public DoubleMatrix2D getCovariance() {
    return covData;
  }

  /**
   * Gets the origin (the first position).
   *
   * @return xyz of the first position
   */
  public double[] getOrigin() {
    return new double[]{xyzData.get(0, 0), xyzData.get(0, 1), xyzData.get(0, 2)};
  }

  /**
   * Subtracts position and adds covariance of specified baseline data at subset of times common to
   * both this data and the baseline data. Data need to be sorted in time ascending order.
   *
   * @param baseline the baseline data
   */
  public void applyBaseline(GpsData baseline) {
    int k = 0;
    List<DataPoint> data = new ArrayList<DataPoint>(observations());

    // if either the data or the baseline is null,
    // then the resultant baseline plot should be null too
    if (Double.isNaN(tdata.getQuick(0, 0)) || Double.isNaN(baseline.tdata.getQuick(0, 0))) {
      DataPoint dp = new DataPoint();
      dp.timeVal = Double.NaN;
      dp.rankVal = Double.NaN;
      dp.xcoord = Double.NaN;
      dp.ycoord = Double.NaN;
      dp.zcoord = Double.NaN;
      dp.sxx = Double.NaN;
      dp.syy = Double.NaN;
      dp.szz = Double.NaN;
      dp.sxy = Double.NaN;
      dp.sxz = Double.NaN;
      dp.syz = Double.NaN;
      data.add(dp);

      // if there is some data in each of the data arrays, then apply the normal baseline comparison
    } else {

      for (int i = 0; i < baseline.observations(); i++) {
        if (tdata.getQuick(0, 0) == baseline.tdata.getQuick(i, 0)) {
          k = i;
        }
      }

      for (int i = 0; i < observations(); i++) {
        for (int j = k; j < baseline.observations(); j++) {
          if (tdata.getQuick(i, 0) == baseline.tdata.getQuick(j, 0)) {
            DataPoint dp = new DataPoint();

            dp.timeVal = tdata.getQuick(i, 0);

            dp.xcoord = xyzData.getQuick(i, 0) - baseline.xyzData.getQuick(j, 0);
            dp.ycoord = xyzData.getQuick(i, 1) - baseline.xyzData.getQuick(j, 1);
            dp.zcoord = xyzData.getQuick(i, 2) - baseline.xyzData.getQuick(j, 2);

            dp.sxx = covData.getQuick(i, 0) + baseline.covData.getQuick(j, 0);
            dp.syy = covData.getQuick(i, 1) + baseline.covData.getQuick(j, 1);
            dp.szz = covData.getQuick(i, 2) + baseline.covData.getQuick(j, 2);
            dp.sxy = covData.getQuick(i, 3) + baseline.covData.getQuick(j, 3);
            dp.sxz = covData.getQuick(i, 4) + baseline.covData.getQuick(j, 4);
            dp.syz = covData.getQuick(i, 5) + baseline.covData.getQuick(j, 5);

            dp.len = Math.sqrt(dp.xcoord * dp.xcoord + dp.ycoord * dp.ycoord + dp.zcoord
                * dp.zcoord);
            data.add(dp);

            k = j + 1;

            break;
          }
        }
      }
    }
    setToList(data);

  }

  /**
   * Converts the XYZ position data to east/north/up (ENU) position data based on a specified
   * origin.
   *
   * @param lon origin longitude
   * @param lat origin latitude
   */
  public void toEnu(double lon, double lat) {
    double x;
    double y;
    double z;
    double sxx;
    double sxy;
    double sxz;
    double syy;
    double syz;
    double szz;

    double s1 = Math.sin(Math.toRadians(lon));
    double c1 = Math.cos(Math.toRadians(lon));
    double s2 = Math.sin(Math.toRadians(lat));
    double c2 = Math.cos(Math.toRadians(lat));

    for (int i = 0; i < observations(); i++) {
      x = xyzData.getQuick(i, 0);
      y = xyzData.getQuick(i, 1);
      z = xyzData.getQuick(i, 2);

      xyzData.setQuick(i, 0, -s1 * x + c1 * y);
      xyzData.setQuick(i, 1, -s2 * (c1 * x + s1 * y) + c2 * z);
      xyzData.setQuick(i, 2, c2 * (c1 * x + s1 * y) + s2 * z);

      sxx = covData.getQuick(i, 0);
      syy = covData.getQuick(i, 1);
      szz = covData.getQuick(i, 2);
      sxy = covData.getQuick(i, 3);
      sxz = covData.getQuick(i, 4);
      syz = covData.getQuick(i, 5);

      covData.setQuick(i, 0, s1 * s1 * sxx - 2 * c1 * s1 * sxy + c1 * c1 * syy);
      covData.setQuick(i, 1,
          s2 * (c1 * c1 * s2 * sxx + 2 * c1 * s2 * s1 * sxy - 2 * c2 * c1 * sxz + s2 * s1 * s1 * syy
              - 2 * c2 * s1 * syz) + c2 * c2 * szz);
      covData.setQuick(i, 2,
          c2 * (c2 * (c1 * c1 * sxx + 2 * c1 * s1 * sxy + s1 * s1 * syy) + 2 * s2 * (c1 * sxz
              + s1 * syz)) + s2 * s2 * szz);
      covData.setQuick(i, 3,
          -c1 * c1 * s2 * sxy + s1 * (s2 * s1 * sxy - c2 * sxz) + c1 * s2 * s1 * (sxx - syy)
              + c2 * c1 * syz);
      covData.setQuick(i, 4,
          -s1 * (c2 * c1 * sxx + c2 * s1 * sxy + s2 * sxz) + c1 * (c2 * c1 * sxy + c2 * s1 * syy
              + s2 * syz));
      covData.setQuick(i, 5,
          -c1 * s2 * (c2 * c1 * sxx + c2 * s1 * sxy + s2 * sxz) - s2 * s1 * (c2 * c1 * sxy
              + c2 * s1 * syy + s2 * syz) + c2 * (c2 * c1 * sxz + c2 * s1 * syz + s2 * szz));

    }
  }

  /**
   * Generates position/time time-series data for plotting by Valve.  The returned time-series data
   * is simply an array of j2ksec, rank, e, n, u, length arrays.
   *
   * @param baseline baseline data or null for none
   * @return time-series data
   */
  public DoubleMatrix2D toTimeSeries(GpsData baseline) {
    double[] origin = getOrigin();
    double[] originLlh = Gps.xyz2llh(origin[0], origin[1], origin[2]);
    if (baseline != null) {
      applyBaseline(baseline);
    }
    toEnu(originLlh[0], originLlh[1]);
    DoubleMatrix2D bigMatrix = DoubleFactory2D.dense
        .compose(new DoubleMatrix2D[][]{{tdata, rdata, xyzData, lenData}});

    return bigMatrix;
  }

  /**
   * Creates the kernel matrix (g) for modeling velocities using weighted least squares.
   *
   * @return the velocity kernel matrix
   */
  public DoubleMatrix2D createVelocityKernel() {
    double t0 = tdata.getQuick(0, 0);
    DoubleMatrix2D[][] g = new DoubleMatrix2D[observations()][2];
    double ta;
    DoubleMatrix2D temp;
    for (int i = 0; i < observations(); i++) {
      ta = (tdata.getQuick(i, 0) - t0) / SECONDSPERYEAR;
      temp = DENSE.make(3, 3);
      temp.setQuick(0, 0, ta);
      temp.setQuick(1, 1, ta);
      temp.setQuick(2, 2, ta);
      g[i][0] = temp;
      g[i][1] = I3X3;
    }
    return SPARSE.compose(g);
  }

  /**
   * Creates the kernel matrix (g) for modeling a displacement at a specified time using weighted
   * least squares.
   *
   * @param dt the displacement time (j2ksec)
   * @return the displacement kernel matrix
   */
  public DoubleMatrix2D createDisplacementKernel(double dt) {
    DoubleMatrix2D[][] g = new DoubleMatrix2D[observations()][2];
    for (int i = 0; i < observations(); i++) {
      if (tdata.getQuick(i, 0) == dt) {
        g[i][0] = ZERO3X3;
        g[i][1] = ZERO3X3;
      } else {
        if (tdata.getQuick(i, 0) < dt) {
          g[i][0] = ZERO3X3;
        } else {
          g[i][0] = I3X3;
        }
        g[i][1] = I3X3;
      }
    }
    return SPARSE.compose(g);
  }

  /**
   * Creates the kernel matrix (g) for modeling a displacement at a specified time using weighted
   * least squares while accounting for the pre-existing linear trend first.
   *
   * @param dt the displacement time (j2ksec)
   * @return the displacement kernel matrix
   */
  public DoubleMatrix2D createDetrendedDisplacementKernel(double dt) {
    double t0 = tdata.getQuick(0, 0);
    DoubleMatrix2D[][] g = new DoubleMatrix2D[observations()][3];
    double ta;
    DoubleMatrix2D temp;
    for (int i = 0; i < observations(); i++) {
      if (tdata.getQuick(i, 0) < dt) {
        g[i][0] = ZERO3X3;
      } else {
        g[i][0] = I3X3;
      }
      ta = (tdata.getQuick(i, 0) - t0) / (86400 * 365.25);
      temp = DENSE.make(3, 3);
      temp.setQuick(0, 0, ta);
      temp.setQuick(1, 1, ta);
      temp.setQuick(2, 2, ta);
      g[i][1] = temp;
      g[i][2] = I3X3;
    }
    return SPARSE.compose(g);
  }

  /**
   * Get first observation from internal matrices.
   *
   * @return DataPoint of first observation
   */
  public DataPoint getFirstObservation() {
    if (observations() <= 0) {
      return null;
    }

    DataPoint dp = new DataPoint();
    dp.timeVal = tdata.getQuick(0, 0);
    dp.rankVal = rdata.getQuick(0, 0);

    dp.xcoord = xyzData.getQuick(0, 0);
    dp.ycoord = xyzData.getQuick(0, 1);
    dp.zcoord = xyzData.getQuick(0, 2);
    dp.len = lenData.getQuick(0, 0);

    dp.sxx = covData.getQuick(0, 0);
    dp.syy = covData.getQuick(0, 1);
    dp.szz = covData.getQuick(0, 2);
    dp.sxy = covData.getQuick(0, 3);
    dp.sxz = covData.getQuick(0, 4);
    dp.syz = covData.getQuick(0, 5);

    return dp;
  }

  /**
   * Write GpsData content to log.
   */
  public void output() {
    for (int i = 0; i < observations(); i++) {
      LOGGER.debug(new Double(tdata.getQuick(i, 0)).toString());
      LOGGER.debug(new Double(rdata.getQuick(i, 0)).toString());
      LOGGER.debug("\t{} {} {} {} ",
          xyzData.getQuick(i, 0), xyzData.getQuick(i, 1),
          xyzData.getQuick(i, 2), lenData.getQuick(i, 0));

      LOGGER.debug("\t\t{} {} {} ",
          covData.getQuick(i, 0),
          covData.getQuick(i, 1),
          covData.getQuick(i, 2));

      LOGGER.debug("\t\t{} {} {} ",
          covData.getQuick(i, 1),
          covData.getQuick(i, 3),
          covData.getQuick(i, 4));

      LOGGER.debug("\t\t{} {} {} ",
          covData.getQuick(i, 2),
          covData.getQuick(i, 4),
          covData.getQuick(i, 5));
    }
  }

}
