package gov.usgs.volcanoes.vdx.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents data about selection option.
 * 
 * @author Loren Antolik, Bill Tollett
 */
public class SelectOption {
  private int soid;
  private int idx;
  private String code;
  private String name;

  /**
   * Constructor.
   * 
   * @param soid select option id
   * @param idx select option index
   * @param code select option code
   * @param name select option name
   */
  public SelectOption(int soid, int idx, String code, String name) {
    this.soid = soid;
    this.idx = idx;
    this.code = code;
    this.name = name;
  }

  /**
   * Constructor.
   * 
   * @param idx select option index
   * @param code select option code
   * @param name select option name
   */
  public SelectOption(int idx, String code, String name) {
    this.soid = 0;
    this.idx = idx;
    this.code = code;
    this.name = name;
  }

  /**
   * Constructor.
   * 
   * @param ch option
   */
  public SelectOption(String ch) {
    String[] parts = ch.split(":");
    soid = Integer.parseInt(parts[0]);
    idx = Integer.parseInt(parts[1]);
    code = parts[2];
    name = parts[3];
  }

  /**
   * Getter for select option id.
   * 
   * @return id
   */
  public int getId() {
    return soid;
  }

  /**
   * Getter for select option idx.
   * 
   * @return index
   */
  public int getIndex() {
    return idx;
  }

  /**
   * Getter for select option code.
   * 
   * @return code
   */
  public String getCode() {
    return code;
  }

  /**
   * Getter for select option name.
   * 
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * Conversion utility.
   * 
   * @param ss list of select options to be added to map
   * @return map of select options, keyed by select option id
   */
  public static Map<Integer, SelectOption> fromStringsToMap(List<String> ss) {
    Map<Integer, SelectOption> map = new LinkedHashMap<Integer, SelectOption>();
    for (String s : ss) {
      SelectOption so = new SelectOption(s);
      map.put(so.getId(), so);
    }
    return map;
  }

  /**
   * Conversion of objects to string.
   * 
   * @return string representation
   */
  public String toString() {
    return String.format("%d:%d:%s:%s", getId(), getIndex(), getCode(), getName());
  }
}
