package au.com.grieve.multibridge.util;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple Template Class
 */
public class SimpleTemplate {

    private Map<String, String> placeHolders;

    public SimpleTemplate(Map<String,String> placeHolders) {
        this.placeHolders = placeHolders;
    }

    /**
     * Given a string, return it with the placeholders replaced
     */
    public String replace(String input) {
        Pattern p = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

        for(int maxTries=20;maxTries > 0;maxTries--) {
            Matcher m = p.matcher(input);
            StringBuffer sb = new StringBuffer(input.length());

            boolean found = false;
            while (m.find()) {
                String tag = m.group(1).toUpperCase();
                if (!placeHolders.containsKey(tag)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                } else {
                    found = true;
                    m.appendReplacement(sb, Matcher.quoteReplacement(placeHolders.get(tag)));
                }
            }

            if (!found) {
                return input;
            }

            m.appendTail(sb);
            input = sb.toString();
        }
        throw new RuntimeException("Too many recursions in Placeholder");
    }

    /**
     * Given an inFile and outFile, replace all placeholders in inFile and save to Outfile
     */
    public void replace(Path inFile, Path outFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile.toFile()));
        BufferedReader reader = new BufferedReader(new FileReader(inFile.toFile()));

        for (String line; ((line = reader.readLine()) != null); ) {
            writer.write(replace(line));
            writer.newLine();
        }
        writer.close();
        reader.close();
    }
}
