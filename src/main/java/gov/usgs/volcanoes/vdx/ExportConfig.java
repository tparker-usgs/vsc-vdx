package gov.usgs.volcanoes.vdx;

import gov.usgs.volcanoes.core.configfile.ConfigFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of export configuration options.
 *
 * @author Scott Hunter
 */
public class ExportConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportConfig.class);
  private int exportable;            // -1 = undef, 1 = yes, 0 = no
  private int numCommentLines;        // # comment lines; -1 = undef
  private TreeMap<Integer, String> commentLine;  // map index -> comment line
  private int[] width;            // display widths (int, dec)
  private boolean closed;
  private String source;

  /**
   * Constructor.
   *
   * @param source name of source (blank for top-level)
   * @param config ConfigFile to extract export options from
   */
  public ExportConfig(String source, ConfigFile config) {
    closed = false;
    this.source = source;

    // is exporting allowed?
    String str = config.getString("exportEnabled");
    if (str != null && (str.equals("true") || str.equals("false"))) {
      exportable = (str.equals("true") ? 1 : 0);
    } else {
      exportable = -1;
    }
    // number of comment lines
    numCommentLines = -1;
    str = config.getString("exportCommentLines");
    commentLine = new TreeMap<Integer, String>();
    if (str != null) {
      try {
        int numLines = Integer.parseInt(str);
        if (numLines < 1) {
          throw new Exception("exportCommentLines out of range: " + numLines);
        }
        numCommentLines = numLines;
      } catch (Exception e) {
        LOGGER.warn("Error parsing {} exportCommentLines: {}", source, str);
      }
    }

    // comment lines
    List<String> comments = config.getList("exportCommentLine");
    if (comments != null) {
      for (Iterator it = comments.iterator(); it.hasNext(); ) {
        String commName = (String) it.next();
        ConfigFile sub = config.getSubConfig(commName);
        String index = sub.getString("index");
        String text = sub.getString("value");
        try {
          int i = Integer.parseInt(index);
          if (numCommentLines != -1 && (i < 1 || i > numCommentLines)) {
            throw new Exception("Index out of range (1.." + numCommentLines + "):" + index);
          }
          if (text == null) {
            // An index w/o text is considered a blank line
            commentLine.put(i, "");
          } else {
            commentLine.put(i, text);
          }
        } catch (Exception e) {
          LOGGER.warn("Error parsing index: ", e);
        }
      }
    }

    // size of fixed-width fields
    width = null;
    str = config.getString("exportDataWidth");
    if (str != null) {
      try {
        String[] widthStr = str.split("[.]");
        if (widthStr.length != 2) {
          throw new Exception("requires 2 values (not " + widthStr.length + "): '" + str + "'");
        }
        width = new int[2];
        String edwName = "first";
        int i;
        for (i = 0; i < 2; i++) {
          width[i] = Integer.parseInt(widthStr[i]);
          if (width[i] < 0 || width[i] > 23) {
            width = null;
            throw new Exception(
                edwName + " export data width value (" + widthStr[i] + ") > 23; ignored");
          }
        }
        i = width[0] + width[1];
        if (i > 23) {
          width = null;
          throw new Exception(edwName + " export data width sum (" + i + ") > 23; ignored");
        }
      } catch (Exception e) {
        LOGGER.warn("Error parsing exportDataWidth; ignoring: ", e);
      }
    }
  }

  /**
   * Constructor.
   *
   * @param srcText List of Strings defining config.  Required format: <ol> <li>exportable</li>
   * <li>width[0]/li> <li>width[1]</li> <li>Remaining <i>numCommentLines</i> items are comment
   * lines</li> </ol>
   */
  public ExportConfig(List<String> srcText) {
    closed = false;
    Iterator<String> it = srcText.iterator();
    exportable = Integer.parseInt(it.next());
    int[] fw = new int[2];
    fw[0] = Integer.parseInt(it.next());
    fw[1] = Integer.parseInt(it.next());
    if (fw[0] != -1 && fw[1] != -1) {
      width = fw;
    }
    numCommentLines = srcText.size() - 3;
    commentLine = new TreeMap<Integer, String>();
    for (int i = 0; i < numCommentLines; i++) {
      commentLine.put(i + 1, it.next());
    }
  }

  /**
   * Override current options from provided options.
   *
   * @param over source of overrides
   */
  public void override(ExportConfig over) {
    if (exportable == -1) {
      exportable = over.exportable;
    } else if (exportable == 1 && over.exportable == 0) {
      exportable = 0;
    }
    if (over.width != null) {
      width = over.width;
    }
    if (over.numCommentLines != -1) {
      numCommentLines = over.numCommentLines;
    }
    for (int i = 1; i <= numCommentLines; i++) {
      String newline = over.commentLine.get(i);
      if (newline != null) {
        commentLine.put(i, newline);
      }
    }
  }

  /**
   * Underride current options from provided options.
   *
   * @param under source of underrides
   */
  public void underride(ExportConfig under) {
    if (exportable == -1) {
      exportable = under.exportable;
    } else if (exportable == 1 && under.exportable == 0) {
      exportable = 0;
    }
    if (width == null) {
      width = under.width;
    }
    if (numCommentLines == -1) {
      numCommentLines = under.numCommentLines;
    }
    for (int i = 1; i <= numCommentLines; i++) {
      if (commentLine.get(i) == null) {
        commentLine.put(i, under.commentLine.get(i));
      }
    }
  }

  /**
   * Return "this source is exportable".
   *
   * @return "this source is exportable"
   */
  public boolean isExportable() {
    return !(exportable == 0);
  }

  /**
   * Return the comment lines for this source.
   *
   * @return array of comment lines
   */
  public String[] getComments() {
    if (numCommentLines < 1) {
      return null;
    }
    String[] comments = new String[numCommentLines];
    for (int i = 0; i < numCommentLines; i++) {
      comments[i] = commentLine.get(i + 1);
      if (comments[i] == null) {
        comments[i] = "";
      }
    }
    return comments;
  }

  /**
   * Return the size of fixed width fields.
   *
   * @return array of fixed width sizes
   */
  public int[] getFixedWidth() {
    int[] fw = width;
    if (fw == null) {
      fw = new int[2];
      fw[0] = 17;
      fw[1] = 6;
    }
    return fw;
  }

  /**
   * Yield list of string representations of this.
   *
   * @return list of strings
   */
  public List<String> toStringList() {
    ArrayList<String> rep = new ArrayList<String>(numCommentLines + 3);
    rep.add("" + exportable);
    if (width != null) {
      rep.add("" + width[0]);
      rep.add("" + width[1]);
    } else {
      rep.add("-1");
      rep.add("-1");
    }
    String[] comments = getComments();
    if (comments != null) {
      for (String s : comments) {
        rep.add(s);
      }
    }
    return rep;
  }

  /**
   * Add components of this to params.
   *
   * @param params mapping to add toStringList
   */
  public void parameterize(Map<String, String> params) {
    params.put("exportable", "" + exportable);
    if (width == null) {
      params.put("width.0", "-1");
      params.put("width.1", "-1");
    } else {
      params.put("width.0", "" + width[0]);
      params.put("width.1", "" + width[1]);
    }
    params.put("numCommentLines", "" + numCommentLines);
    String[] comments = getComments();
    if (comments != null) {
      int i = 1;
      for (String s : comments) {
        params.put("cmt." + i, s);
        i++;
      }
    }
  }

  /**
   * Test for closure.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Close this.
   */
  public void setClosed() {
    closed = true;
  }

}
