package org.unicode.tools.emoji;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.text.utility.Utility;
import org.unicode.tools.Tabber;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.UnicodeSet;

public class GenerateEmojiKeyboard {
    enum Target {csv, propFile}

    public static void main(String[] args) throws Exception {
        GenerateEmojiKeyboard.showLines(EmojiOrder.STD_ORDER, Target.propFile, Emoji.DATA_DIR);
        // GenerateEmojiKeyboard.showLines(EmojiOrder.STD_ORDER, Target.csv, Emoji.TR51_INTERNAL_DIR + "keyboard");
        //        boolean foo2 = EmojiData.EMOJI_DATA.getChars().contains(EmojiData.SAMPLE_WITHOUT_TRAILING_EVS);
        //        Set<String> foo = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, 
        //                EmojiData.EMOJI_DATA.getChars());
        //
        //        showDiff(EmojiData.EMOJI_DATA.getChars(), EmojiOrder.STD_ORDER.emojiData.getSortingChars());
    }

    private static void showDiff(UnicodeSet chars, UnicodeSet sortingChars) {
        System.out.println(new UnicodeSet(chars).removeAll(sortingChars));
        System.out.println(new UnicodeSet(sortingChars).removeAll(chars));
    }

    static class Totals {
        int total = 0;
        int totalNoMod = 0;
        int totalNoModNoSign = 0;

        private void add(String cp) {
            Totals totals = this;
            ++totals.total;
            if (!EmojiData.MODIFIERS.containsSome(cp)) {
                ++totals.totalNoMod;
                if (!Emoji.GENDER_MARKERS.containsSome(cp)) {
                    ++totals.totalNoModNoSign;
                }
            }
        }

        private void show(TempPrintWriter out, EmojiOrder.MajorGroup lastMajorGroup) throws IOException {
            Totals totals = this;
            out.println("# subtotal:\t\t" + totals.total);
            out.println("# subtotal:\t\t" + totals.totalNoMod + "\tw/o modifiers");
            System.out.println("\t" + lastMajorGroup + "\t" + totals.total + "\t" + totals.totalNoMod + "\t" + totals.totalNoModNoSign);
            totals.total = 0;
            totals.totalNoMod = 0;
            totals.totalNoModNoSign = 0;
        }
    }

    public static void showLines(EmojiOrder emojiOrder, Target target, String directory) throws IOException {
        Set<String> retain = ImmutableSet.copyOf(
                new UnicodeSet(emojiOrder.emojiData
                        .getSortingChars())
                .removeAll(EmojiData.MODIFIERS)
                .addAllTo(new HashSet<String>()));
        
        UnicodeSet charactersNotShown = new UnicodeSet().addAll(retain);
        EmojiOrder.MajorGroup lastMajorGroup = null;
        TempPrintWriter out = null;
        Totals totals = new Totals();
        
        int maxField1 = 0;
        int maxField2 = 10;
        for (String cp : retain) {
            String hcp1 = Utility.hex(cp, " ");
            if (hcp1.length() > maxField1) {
                maxField1 = hcp1.length();
            }
        }
        Tabber tabber = new Tabber.MonoTabber()
        .add(maxField1+1, Tabber.LEFT)
        .add("non-fully-qualified".length()+3, Tabber.LEFT);
        ;

        for (Entry<String, Set<String>> labelToSet : emojiOrder.orderingToCharacters.keyValuesSet()) {
            final String label = labelToSet.getKey();
            final Set<String> list = labelToSet.getValue();
            if (list.contains("👮")) {
                int debug = 0;
            }
            EmojiOrder.MajorGroup majorGroup = emojiOrder.getMajorGroup(list); // majorGroupings.get(list.iterator().next());
            if (lastMajorGroup != majorGroup) {
                if (out != null) {
                    totals.show(out, lastMajorGroup);
                    if (target == Target.csv){ 
                        out.println("\n#EOF");
                        out.close();
                        out = null;
                    }
                }
                if (out == null) {
                    String filename = target == Target.csv ? majorGroup.toString().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z]+", "_") : "emoji-test";
                    final String suffix = target == Target.csv ? ".csv" : ".txt";
                    out = new TempPrintWriter(directory, filename + suffix);
                    if (target == Target.csv) {
                        out.println("# " + filename);
                        out.println("\n# Format\n"
                                + "#   Hex code points, characters, name");
                    } else {
                        out.println(Utility.getBaseDataHeader(filename, 51, "Emoji Keyboard/Display Test Data", Emoji.VERSION_STRING));
                        out.println("# This file provides data for testing which emoji forms should be in keyboards and which should also be displayed/processed.\n"
                                + "# Format\n"
                                + "#   Code points; status # emoji name\n"
                                + "#     Status\n"
                                + "#       keyboard — see “Emoji Implementation Notes” in UTR#51\n"
                                + "#       process — see “Emoji Implementation Notes” in UTR#51"
                                );
                        out.println("# Notes:\n"
                                + "#   • This currently omits the 12 keycap bases, the 5 modifier characters, and 26 singleton Regional Indicator characters\n"
                                + "#   • The file is in CLDR order, not codepoint order. This is recommended (but not required!) for keyboard palettes.\n"
                                + "#   • The groups and subgroups are purely illustrative. See the Emoji Order chart for more information."
                                );
                    }
                }
                if (target == Target.propFile) {
                    out.println("\n# group: " + majorGroup);
                }
                lastMajorGroup = majorGroup;
            }
            LinkedHashSet<String> filtered = new LinkedHashSet<>(list);
            if (retain != null) {
                filtered.retainAll(retain);
            }
            if (filtered.isEmpty()) {
                continue;
            }
            out.println("\n# subgroup: " + label); //  + "; size: " + filtered.size() + "; list: [" + CollectionUtilities.join(filtered, " ") + "]\n");
            
            for (String cp_raw : filtered) {
                String cp = emojiOrder.emojiData.addEmojiVariants(cp_raw);
                
                final String withoutRaw = cp.replace(Emoji.EMOJI_VARIANT_STRING, "");
                // String withoutVs = cp.contains(Emoji.JOINER_STRING) ? withoutRaw : cp;
                charactersNotShown.remove(withoutRaw);

                charactersNotShown.remove(cp);
                switch(target) {
                case csv: 
                    out.println("U+" + Utility.hex(cp,"U+") 
                            + "," + cp 
                            + "," + EmojiData.EMOJI_DATA.getName(cp, false));
                    break;
                case propFile:
                    out.println(tabber.process(Utility.hex(cp) + "\t; " 
                            + "fully-qualified"
                            + "\t# " + cp + " " + EmojiData.EMOJI_DATA.getName(cp, false)));
                    showWithoutVS(out, tabber, cp, charactersNotShown);
                    break;
                }
                totals.add(cp);
            }

            //          allCharacters.add(filtered);
            //          if (!allCharacters.equals(new UnicodeSet().addAll(retain))) {
            //              out.println(
            //                      retain.size() 
            //                      + "\t" + allCharacters.size() 
            //                      + "\t" + new UnicodeSet().addAll(retain).removeAll(allCharacters)
            //                      + "\t" + new UnicodeSet().addAll(allCharacters).removeAll(retain)
            //                      );
            //          }
        }
        if (out != null) {
            totals.show(out, lastMajorGroup);
            out.println("\n#EOF");
            out.close();
        }
        if (charactersNotShown.size() != 0) {
            throw new IllegalArgumentException("Missing characters: " + charactersNotShown.size() + "\t" + charactersNotShown.toPattern(false));
        }
    }

    static Splitter vsSplitter = Splitter.on(Emoji.EMOJI_VARIANT);
    
    /** Show all of the combinations with VS, except for all VS characters.
     */
    private static void showWithoutVS(TempPrintWriter out, Tabber tabber, String cp, UnicodeSet charactersNotShown) throws IOException {
        if (!cp.contains(Emoji.JOINER_STRING)) {
            return;
        }
        int pos = cp.indexOf(Emoji.EMOJI_VARIANT);
        if (pos < 0) {
            return;
        }
        String name = EmojiData.EMOJI_DATA.getName(cp, false);
        
        final List<String> parts = vsSplitter.splitToList(cp);
        final int size = parts.size();
        if (size > 2) {
            int debug = 0;
        }
        int count = (1 << (size-1)) - 1; // 3 parts => 100 => 11
        for (int bitmap = 0; bitmap < count; ++bitmap) {
            String temp = parts.get(0);
            for (int rest = 0; rest < size - 1; ++rest) {
                if ((bitmap & (1<<rest)) != 0) {
                    temp += Emoji.EMOJI_VARIANT_STRING;
                }
                temp += parts.get(rest+1);
            }
            out.println(tabber.process(Utility.hex(temp) + "\t; " 
                    + "non-fully-qualified"
                    + "\t# " + temp + " " + name));
            charactersNotShown.remove(temp);
        }
    }
}
