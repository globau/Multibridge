package au.com.grieve.multibridge.util;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A simple Template Class */
public class SimpleTemplate {

  private Map<String, String> placeHolders;

  public SimpleTemplate(Map<String, String> placeHolders) {
    this.placeHolders = placeHolders;
  }

  /**
   * Given a string, return it with the placeholders replaced Throws IOException if the string
   * references a variable not provided in placeHolders
   */
  public String replace(String input) throws IOException {
    Pattern p = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

    for (int maxTries = 20; maxTries > 0; maxTries--) {
      Matcher m = p.matcher(input);
      StringBuffer sb = new StringBuffer(input.length());

      boolean found = false;
      while (m.find()) {
        String tag = m.group(1).toUpperCase();
        if (!placeHolders.containsKey(tag)) {
          throw new MissingVariable(tag);
        }
        found = true;
        m.appendReplacement(sb, Matcher.quoteReplacement(placeHolders.get(tag)));
      }

      if (!found) {
        return input;
      }

      m.appendTail(sb);
      input = sb.toString();
    }
    throw new RuntimeException("Too many recursions in Placeholder");
  }

  /** Given an inFile and outFile, replace all placeholders in inFile and save to Outfile */
  public void replace(Path inFile, Path outFile) throws IOException {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(outFile.toFile()));
      BufferedReader reader = new BufferedReader(new FileReader(inFile.toFile()));

      for (String line; ((line = reader.readLine()) != null); ) {
        writer.write(replace(line));
        writer.newLine();
      }
    } catch (MissingVariable e) {
      System.err.println(inFile + ": " + e.getMessage());
    } catch (IOException e) {
      throw new IOException(inFile + ": " + e.getMessage());
    }
  }

  private static class MissingVariable extends IOException {
    MissingVariable(String variable) {
      super("Missing value for template variable: " + variable);
    }
  }
}
