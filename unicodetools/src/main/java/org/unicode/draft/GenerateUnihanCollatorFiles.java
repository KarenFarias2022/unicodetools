package org.unicode.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;

public class GenerateUnihanCollatorFiles {

    static final String OUTPUT_DIRECTORY = CldrUtility.GEN_DIRECTORY + "/han";
    static final String OUTPUT_DIRECTORY_REPLACE = CldrUtility.GEN_DIRECTORY + "/han/replace";

    static final Pattern START_AUTOGEN = Pattern.compile(".*#\\s*START\\s*AUTOGENERATED\\s*([^(]*).*");
    static final Pattern END_AUTOGEN = Pattern.compile(".*#\\s*END\\s*AUTOGENERATED\\s*([^(]*).*");

    // new format
    // # START AUTOGENERATED PINYIN LONG (sort by pinyin then kTotalStrokes then kRSUnicode)


    public static void main(String[] args) throws IOException {
        composeFile(CldrUtility.COMMON_DIRECTORY + "/collation/", "zh.xml", true);
        composeFile(CldrUtility.COMMON_DIRECTORY + "/transforms/", "Han-Latin.xml", false);
    }

    private static void composeFile(String inputDirectory, String fileName, boolean fixChoice) throws IOException {
        final Matcher start_autogen = START_AUTOGEN.matcher("");
        final Matcher end_autogen = END_AUTOGEN.matcher("");
        int count = 0;
        try (final PrintWriter newFile = FileUtilities.openUTF8Writer(OUTPUT_DIRECTORY_REPLACE, fileName);
                final BufferedReader oldFile = FileUtilities.openUTF8Reader(inputDirectory, fileName)) {
            while (true) {

                // copy up to the first autogen comment, including the comment line
                String matchingLine = CldrUtility.copyUpTo(oldFile, start_autogen, newFile, true);
                newFile.flush();
                if (matchingLine == null) {
                    if (count == 0) {
                        throw new IllegalArgumentException("No START comments for autogeneration: " + matchingLine);
                    }
                    break;
                }
                ++count;
                final String choice = start_autogen.group(1).trim();
                final String replacementFile = fixChoice 
                        ? choice.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "_").replace("_long", "").replace("stroke", "strokeT") + ".txt" 
                        : choice;

                // copy the file to be inserted
                try (final BufferedReader insertFile = FileUtilities.openUTF8Reader(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, replacementFile)) {
                    CldrUtility.copyUpTo(insertFile, (Matcher)null, newFile, true); // copy to end
                    newFile.flush();
                }
                //insertFile.close();

                // skip to the end of the matching autogen comment
                matchingLine = CldrUtility.copyUpTo(oldFile, end_autogen, null, true);

                // check for matching comment
                if (matchingLine == null || !choice.equals(end_autogen.group(1).trim())) {
                    throw new IllegalArgumentException("Mismatched comments for autogeneration: " + choice + ", " + matchingLine);
                }
                newFile.println(matchingLine); // copy comment line
                newFile.flush();
            }
        }
        System.out.println(count + " segments replaced");
    }
}

