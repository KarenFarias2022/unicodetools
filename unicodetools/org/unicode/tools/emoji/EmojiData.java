package org.unicode.tools.emoji;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji.ModifierStatus;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class EmojiData {
    private static UnicodeSet SUPPRESS_SECONDARY = new UnicodeSet("[😀 😁 😂 😃 😄 😅 😆 😉 😊 😋 😎 😍 😘 😗 😙 😚 ☺ 🙂 🤗 😇 🤔 😐 😑 😶 🙄 😏 😣 😥 😮 🤐 😯 😪 😫 😴 😌 🤓 😛 😜 😝 ☹ 🙁 😒 😓 😔 😕 😖 🙃 😷 🤒 🤕 🤑 😲 😞 😟 😤 😢 😭 😦 😧 😨 😩 😬 😰 😱 😳 😵 😡 😠 👿 😈]").freeze();

    public enum DefaultPresentation {text, emoji}

    public static class EmojiDatum {
        public final DefaultPresentation style;
        public final ModifierStatus modifierStatus;
        public final Set<Emoji.CharSource> sources;

        /**
         * Sets must be immutable.
         * @param style
         * @param order
         * @param annotations
         * @param sources
         */
        private EmojiDatum(DefaultPresentation style, ModifierStatus modifierClass, Set<Emoji.CharSource> sources) {
            this.style = style;
            this.modifierStatus = modifierClass;
            this.sources = sources == null ? Collections.<Emoji.CharSource>emptySet() : sources;
        }

        @Override
        public String toString() {
            return "Emoji=Yes; " 
                    + "Emoji_Presentation=" + (style == DefaultPresentation.emoji ? "Yes" : "No") + "; "
                    + "Emoji_Modifier=" + (modifierStatus == ModifierStatus.modifier ? "Yes" : "No") + "; "
                    + "Emoji_Modifier_Base=" + (modifierStatus == ModifierStatus.modifier_base ? "Yes" : "No") + "; "
                    ;
        }
        @Override
        public boolean equals(Object obj) {
            EmojiDatum other = (EmojiDatum) obj;
            return obj != null 
                    && Objects.equals(style, other.style)
                    && Objects.equals(modifierStatus, other.modifierStatus)
                    && Objects.equals(sources, other.sources);
        }
        @Override
        public int hashCode() {
            return Objects.hash(style, modifierStatus, sources);
        }
    }

    private final UnicodeMap<EmojiDatum> data = new UnicodeMap<>();
    private final UnicodeSet singletonsWithDefectives = new UnicodeSet();
    private final UnicodeSet singletonsWithoutDefectives = new UnicodeSet();
    private final UnicodeSet charsWithData = new UnicodeSet();
    private final UnicodeSet allEmojiWithoutDefectives;
    private final UnicodeSet sortingCharacters;
    private final UnicodeSet allEmojiWithDefectives;

    private final Map<DefaultPresentation, UnicodeSet> defaultPresentationMap;
    private final Map<ModifierStatus, UnicodeSet> modifierClassMap;
    private final Map<Emoji.CharSource, UnicodeSet> charSourceMap;
    private final UnicodeSet modifierBases;
    private final VersionInfo version;
    private final UnicodeSet modifierSequences;
    private final UnicodeSet zwjSequencesNormal = new UnicodeSet();
    private final UnicodeSet zwjSequencesAll = new UnicodeSet();
    private final UnicodeSet afterZwj = new UnicodeSet();
    private final UnicodeSet flagSequences = new UnicodeSet();
    private final UnicodeSet keycapSequences = new UnicodeSet();
    private final UnicodeSet keycapSequenceAll = new UnicodeSet();
    private final UnicodeSet keycapBases = new UnicodeSet();
    private final UnicodeSet emojiDefectives = new UnicodeSet();
    private final UnicodeMap<String> toNormalizedVariant = new UnicodeMap<String>();
    
    private final UnicodeMap<String> names = new UnicodeMap<>();

    public static final Splitter semi = Splitter.onPattern("[;#]").trimResults();
    public static final Splitter semiOnly = Splitter.onPattern(";").trimResults();
    public static final Splitter hashOnly = Splitter.onPattern("#").trimResults();
    public static final Splitter comma = Splitter.on(",").trimResults();

    static final ConcurrentHashMap<VersionInfo, EmojiData> cache = new ConcurrentHashMap<>();

    public static EmojiData of(VersionInfo version) {
        EmojiData result = cache.get(version);
        if (result == null) {
            result = new EmojiData(version);
            cache.put(version, result);
        }
        return result;
    }

    enum EmojiProp {Emoji, Emoji_Presentation, Emoji_Modifier, Emoji_Modifier_Base}
    // 0023          ; Emoji                #   [1] (#️)      NUMBER SIGN
    // 231A..231B    ; Emoji_Presentation   #   [2] (⌚️..⌛️)  WATCH..HOURGLASS
    // 1F3FB..1F3FF  ; Emoji_Modifier
    // 261D          ; Emoji_Modifier_Base  #   [1] (☝️)      WHITE UP POINTING INDEX

    private EmojiData(VersionInfo version) {
        final UnicodeMap<General_Category_Values> gc = Emoji.BETA.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
        UnicodeSet NSM = gc.getSet(UcdPropertyValues.General_Category_Values.Nonspacing_Mark);
        UnicodeSet EM = gc.getSet(UcdPropertyValues.General_Category_Values.Enclosing_Mark);
        EnumMap<DefaultPresentation, UnicodeSet> _defaultPresentationMap = new EnumMap<>(DefaultPresentation.class);
        EnumMap<ModifierStatus, UnicodeSet> _modifierClassMap = new EnumMap<>(ModifierStatus.class);
        EnumMap<Emoji.CharSource, UnicodeSet> _charSourceMap = new EnumMap<>(Emoji.CharSource.class);

        String[] ADD_VARIANT_KEYCAPS = {
                Emoji.KEYCAP_MARK_STRING,
                // Emoji.TEXT_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING,
                Emoji.EMOJI_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING,
        };

        this.version = version;
        final String directory = Settings.DATA_DIR + "emoji/" + version.getVersionString(2, 4);
        if (version.compareTo(VersionInfo.getInstance(2)) >= 0) {
            UnicodeRelation<EmojiProp> emojiData = new UnicodeRelation<>();
            UnicodeMap<Set<Emoji.CharSource>> sourceData = new UnicodeMap<>();

            for (String line : FileUtilities.in(directory, "emoji-data.txt")) {
                //# Code ;  Default Style ; Ordering ;  Annotations ;   Sources #Version Char Name
                // U+263A ;    text ;  0 ; face, human, outlined, relaxed, smile, smiley, smiling ;    jw  # V1.1 (☺) white smiling face
                if (line.startsWith("#") || line.isEmpty()) continue;
                List<String> coreList = hashOnly.splitToList(line);
                List<String> list = semi.splitToList(coreList.get(0));
                final String f0 = list.get(0);
                final EmojiProp prop = EmojiProp.valueOf(list.get(1));
                Binary propValue = Binary.Yes;
                if (list.size() > 2) {
                    propValue = Binary.valueOf(list.get(2));
                }
                int codePoint, codePointEnd;
                int pos = f0.indexOf("..");
                if (pos < 0) {
                    codePoint = codePointEnd = Integer.parseInt(f0, 16);
                } else {
                    codePoint = Integer.parseInt(f0.substring(0,pos), 16);
                    codePointEnd = Integer.parseInt(f0.substring(pos+2), 16);
                }
                for (int cp = codePoint; cp <= codePointEnd; ++cp) {
                    if (propValue == Binary.No) {
                        emojiData.remove(cp, prop);
                    } else {
                        emojiData.add(cp, prop);
                    }
                }
            }
            // check consistency, fix "with defectives"
            for (Entry<String, Set<EmojiProp>> entry : emojiData.entrySet()) {
                final String cp = entry.getKey();
                final Set<EmojiProp> set = entry.getValue();
                if (!set.contains(EmojiProp.Emoji)) {
                    System.err.println("**\t" + cp + "\t" + set);
                }
                singletonsWithDefectives.add(cp);
                if (!Emoji.DEFECTIVE.contains(cp)) {
                    singletonsWithoutDefectives.add(cp);
                }
            }
            // HACK 1F441 200D 1F5E8
            zwjSequencesAll.add(new StringBuilder()
            .appendCodePoint(0x1F441)
            .appendCodePoint(0xFE0F)
            .appendCodePoint(0x200D)
            .appendCodePoint(0x1F5E8)
            .appendCodePoint(0xFE0F)
            .toString());
            
            for (String file : Arrays.asList("emoji-sequences.txt", "emoji-zwj-sequences.txt")) {
                boolean zwj = file.contains("zwj");
                for (String line : FileUtilities.in(directory, file)) {
                    int hashPos = line.indexOf('#');
                    if (hashPos >= 0) {
                        line = line.substring(0, hashPos);
                    }
                    if (line.isEmpty()) continue;
                    
                    List<String> list = semi.splitToList(line);
                    String source = Utility.fromHex(list.get(0));
                    String withoutEmojiVariant = source.replace(Emoji.EMOJI_VARIANT_STRING, "");
                    if (!withoutEmojiVariant.equals(source)) {
                        toNormalizedVariant.put(withoutEmojiVariant, source);
                    }
                    emojiDefectives.addAll(source);
                    int first = source.codePointAt(0);
                    if (zwj) {
                        zwjSequencesAll.add(source);
                        if (!source.contains("\u2764") || source.contains(Emoji.EMOJI_VARIANT_STRING)) {
                            zwjSequencesNormal.add(source);
                        }
                        zwjSequencesAll.add(source.replace(Emoji.EMOJI_VARIANT_STRING, "")); // get non-variant

                        boolean isAfterZwj = false;
                        for (int cp : CharSequences.codePoints(source)) {
                            if (isAfterZwj) {
                                afterZwj.add(cp);
                            }
                            isAfterZwj = cp == 0x200D;
                        }
                    } else {
                        if (Emoji.isRegionalIndicator(first)) {
                            flagSequences.add(source);
                        } else if (EM.containsSome(source) || NSM.containsSome(source)) {
                            final String firstString = source.substring(0,1);
                            keycapSequences.add(firstString + Emoji.EMOJI_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING);
                            for (String s : ADD_VARIANT_KEYCAPS) {
                                keycapSequenceAll.add(firstString + s);
                            }
                            keycapBases.add(firstString);
                        } else if (Emoji.DEFECTIVE.contains(first)) {
                            throw new IllegalArgumentException("Unexpected");
                        }
                    }
                    String name;
                    if (list.size() > 2) {
                        name = UCharacter.toTitleCase(list.get(2), BreakIterator.getSentenceInstance(ULocale.ENGLISH));
                    } else {
                        name = getFallbackName(source);
                    }
                    names.put(source, name);
                    String source2 = source.replace(Emoji.EMOJI_VARIANT_STRING, "");
                    if (!source2.equals(source)) {
                        names.put(source2, name);
                    }
                    if (!Emoji.DEFECTIVE.contains(first)) { // HACK
                        continue;
                    }
                    emojiData.add(source, EmojiProp.Emoji);
                    //                    if (Emoji.REGIONAL_INDICATORS.contains(first)) {
                    //                        emojiData.add(source, EmojiProp.Emoji_Presentation);
                    //                    }
                }
            }
            emojiDefectives.removeAll(emojiData.keySet()).freeze();
            zwjSequencesNormal.freeze();
            zwjSequencesAll.removeAll(zwjSequencesNormal).freeze();
            afterZwj.freeze();
            flagSequences.freeze();
            keycapSequences.freeze();
            keycapSequenceAll.removeAll(keycapSequences).freeze();
            keycapBases.freeze();

            for (String line : FileUtilities.in(EmojiData.class, "emojiSources.txt")) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                List<String> list = semi.splitToList(line);
                String source = Utility.fromHex(list.get(0));
                Set<Emoji.CharSource> sourcesIn = getSet(list.get(1));
                sourceData.put(source, sourcesIn);
            }
            singletonsWithDefectives.freeze();
            singletonsWithoutDefectives.freeze();
            emojiData.freeze();
            sourceData.freeze();
            for (Entry<String, Set<EmojiProp>> entry : emojiData.entrySet()) {
                final String key = entry.getKey();
                final Set<EmojiProp> set = entry.getValue();
                DefaultPresentation styleIn = set.contains(EmojiProp.Emoji_Presentation) ? DefaultPresentation.emoji : DefaultPresentation.text;
                ModifierStatus modClass = set.contains(EmojiProp.Emoji_Modifier) ? ModifierStatus.modifier
                        : set.contains(EmojiProp.Emoji_Modifier_Base) ? ModifierStatus.modifier_base 
                                : ModifierStatus.none;
                Set<Emoji.CharSource> sources = sourceData.get(key);
                data.put(key, new EmojiDatum(styleIn, modClass, sources));
                putUnicodeSetValue(_defaultPresentationMap, key, styleIn);
                putUnicodeSetValue(_modifierClassMap, key, modClass);
            }

        } else {
            for (String line : FileUtilities.in(directory, "emoji-data.txt")) {
                //# Code ;  Default Style ; Ordering ;  Annotations ;   Sources #Version Char Name
                // U+263A ;    text ;  0 ; face, human, outlined, relaxed, smile, smiley, smiling ;    jw  # V1.1 (☺) white smiling face
                if (line.startsWith("#") || line.isEmpty()) continue;
                List<String> list = semi.splitToList(line);
                // 00A9 ;   text ;  L1 ;    none ;  j   # V1.1 (©) COPYRIGHT SIGN
                // 2639 ;   text ;  L2 ;    secondary ; w   # V1.1 (☹) WHITE FROWNING FACE

                String codePoint = Utility.fromHex(list.get(0)); // .replace("U+","")
                DefaultPresentation styleIn = DefaultPresentation.valueOf(list.get(1));

                ModifierStatus modClass = Emoji.IS_BETA && SUPPRESS_SECONDARY.contains(codePoint) 
                        ? ModifierStatus.none 
                                : ModifierStatus.fromString(list.get(3));
                // remap, since we merged 'secondary' into 'primary'
                Set<Emoji.CharSource> sourcesIn = getSet(_charSourceMap, codePoint, list.get(4));
                data.put(codePoint, new EmojiDatum(styleIn, modClass, sourcesIn));
                putUnicodeSetValue(_defaultPresentationMap, codePoint, styleIn);
                putUnicodeSetValue(_modifierClassMap, codePoint, modClass);
            }
        }

        freezeUnicodeSets(_defaultPresentationMap.values());
        freezeUnicodeSets(_charSourceMap.values());
        modifierClassMap = Collections.unmodifiableMap(_modifierClassMap);
        modifierBases = new UnicodeSet()
        .addAll(modifierClassMap.get(ModifierStatus.modifier_base))
        //.addAll(modifierClassMap.get(ModifierStatus.secondary))
        .freeze();
        modifierSequences = new UnicodeSet();
        for (String base : modifierBases) {
            for (String mod : modifierClassMap.get(ModifierStatus.modifier)) {
                final String seq = base + mod;
                modifierSequences.add(seq);
                names.put(seq, UCharacter.toTitleFirst(ULocale.ENGLISH, getName(base, true)) 
                        + ", " + shortName(mod.codePointAt(0)).toLowerCase(Locale.ENGLISH));
            }
        }
        names.freeze();
        modifierSequences.freeze();

        defaultPresentationMap = Collections.unmodifiableMap(_defaultPresentationMap);
        charSourceMap = Collections.unmodifiableMap(_charSourceMap);
        data.freeze();
        charsWithData.addAll(data.keySet());
        charsWithData.freeze();
        //        flatChars.addAll(singletonsWithDefectives)
        //        .removeAll(charsWithData.strings())
        //        .addAll(Emoji.FIRST_REGIONAL,Emoji.LAST_REGIONAL)
        //        .addAll(new UnicodeSet("[0-9*#]"))
        ////        .add(Emoji.EMOJI_VARIANT)
        ////        .add(Emoji.TEXT_VARIANT)
        ////        .add(Emoji.JOINER)
        //        //.removeAll(new UnicodeSet("[[:L:][:M:][:^nt=none:]+_-]"))
        //        .freeze();

        allEmojiWithoutDefectives = new UnicodeSet(singletonsWithDefectives)
        .addAll(flagSequences)
        .addAll(modifierSequences)
        .addAll(keycapSequences)
        .addAll(zwjSequencesNormal)
        .removeAll(Emoji.DEFECTIVE)
        .freeze();
        
        allEmojiWithDefectives = new UnicodeSet(allEmojiWithoutDefectives)
        .addAll(zwjSequencesAll)
        .addAll(keycapSequenceAll)
        .freeze();

        sortingCharacters = new UnicodeSet(allEmojiWithDefectives)
        .removeAll(Emoji.DEFECTIVE)
        .freeze();
    }

    public UnicodeSet getSingletonsWithDefectives() {
        return singletonsWithDefectives;
    }

    public UnicodeSet getAllEmojiWithDefectives() {
        return allEmojiWithDefectives;
    }

    public UnicodeSet getAllEmojiWithoutDefectives() {
        return allEmojiWithoutDefectives;
    }

    public UnicodeSet getSingletonsWithoutDefectives() {
        return singletonsWithoutDefectives;
    }

    public UnicodeSet getChars() {
        return charsWithData;
    }

    public static void freezeUnicodeSets(Collection<UnicodeSet> collection) {
        for (UnicodeSet value : collection) {
            value.freeze();
        }
    }

    public EmojiDatum getData(String codePointString) {
        return data.get(codePointString);
    }

    public EmojiDatum getData(int codePoint) {
        return data.get(codePoint);
    }

    public Set<ModifierStatus> getModifierStatuses() {
        return modifierClassMap.keySet();
    }

    public UnicodeSet getModifierStatusSet(ModifierStatus source) {
        return CldrUtility.ifNull(modifierClassMap.get(source), UnicodeSet.EMPTY);
    }

    public UnicodeSet getModifierBases() {
        return modifierBases;
    }

    public UnicodeSet getModifierSequences() {
        return modifierSequences;
    }

    public UnicodeSet getModifiers() {
        return modifierClassMap.get(ModifierStatus.modifier);
    }

    public UnicodeSet getZwjSequencesNormal() {
        return zwjSequencesNormal;
    }

    /** Extra variant sequences */
    public UnicodeSet getZwjSequencesAll() {
        return zwjSequencesAll;
    }

    public UnicodeSet getAfterZwj() {
        return afterZwj;
    }

    public UnicodeSet getDefaultPresentationSet(DefaultPresentation defaultPresentation) {
        return CldrUtility.ifNull(defaultPresentationMap.get(defaultPresentation),UnicodeSet.EMPTY);
    }

    public UnicodeSet getCharSourceSet(Emoji.CharSource charSource) {
        return CldrUtility.ifNull(charSourceMap.get(charSource), UnicodeSet.EMPTY);
    }

    public Iterable<Entry<String, EmojiDatum>> entrySet() {
        return data.entrySet();
    }

    public UnicodeSet keySet() {
        return data.keySet();
    }

    private static Set<Emoji.CharSource> getSet(EnumMap<Emoji.CharSource, UnicodeSet> _defaultPresentationMap, String source, String string) {
        if (string.isEmpty()) {
            return Collections.emptySet();
        }
        EnumSet<Emoji.CharSource> result = EnumSet.noneOf(Emoji.CharSource.class);
        for (Emoji.CharSource cs : Emoji.CharSource.values()) {
            if (string.contains(cs.letter)) {
                result.add(cs);
                putUnicodeSetValue(_defaultPresentationMap, source, cs);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<Emoji.CharSource> getSet(String list) {
        if (list.isEmpty()) {
            return Collections.emptySet();
        }
        EnumSet<Emoji.CharSource> result = EnumSet.noneOf(Emoji.CharSource.class);
        for (Emoji.CharSource cs : Emoji.CharSource.values()) {
            if (list.contains(cs.letter)) {
                result.add(cs);
            }
        }
        return Collections.unmodifiableSet(result);
    }


    public static <T> void putUnicodeSetValue(
            Map<T, UnicodeSet> map,
            String key, T value) {
        UnicodeSet us = map.get(value);
        if (us == null) {
            map.put(value, us = new UnicodeSet());
        }
        us.add(key);
    }

    //    public final Comparator<String> EMOJI_COMPARATOR = new Comparator<String>() {
    //
    //        @Override
    //        public int compare(String o1, String o2) {
    //            int i1 = 0, i2 = 0;
    //            while (true) {
    //                if (i1 == o1.length()) {
    //                    return i2 == o2.length() ? 0 : -1;
    //                } else if (i2 == o2.length()) {
    //                    return 1;
    //                }
    //                int cp1 = o1.codePointAt(i1);
    //                int cp2 = o2.codePointAt(i2);
    //                if (cp1 != cp2) {
    //                    EmojiDatum d1 = getData(cp1);
    //                    EmojiDatum d2 = getData(cp2);
    //                    if (d1 == null) {
    //                        return d2 == null ? cp1 - cp2 : 1;
    //                    } else {
    //                        return d2 == null ? -1 : d1.order - d2.order;
    //                    }
    //                }
    //                i1 += Character.charCount(cp1);
    //                i2 += Character.charCount(cp2);
    //                continue;
    //            }
    //        }       
    //    };
    //


    private static void show(int cp, final UnicodeMap<String> names, EmojiData emojiData) {
        System.out.println(emojiData.version + "\t" + Utility.hex(cp) + ", " + emojiData.getData(cp) + "\t" + names.get(cp));
    }
    private static void show(String cp, UnicodeMap<Age_Values> ages, final UnicodeMap<String> names, EmojiData emojiData) {
        System.out.println(
                Emoji.getNewest(cp).getShortName()
                + ";\temojiVersion=" + emojiData.version.getVersionString(2, 2) 
                + ";\t" + Utility.hex(cp) 
                + ";\t" + cp
                + ";\t" + names.get(cp)
                + ";\t" + emojiData.getData(cp) 
                );
    }

    public UnicodeSet getSortingChars() {
        return sortingCharacters;
    }

    public static final EmojiData EMOJI_DATA = of(Emoji.VERSION_TO_GENERATE);
    public static final UnicodeSet MODIFIERS = EMOJI_DATA.getModifierStatusSet(ModifierStatus.modifier);

    public UnicodeSet getFlagSequences() {
        return flagSequences;
    }

    public UnicodeSet getKeycapSequences() {
        return keycapSequences;
    }

    /** Include variant VS sequences **/
    public UnicodeSet getKeycapSequencesAll() {
        return keycapSequenceAll;
    }

    public UnicodeSet getKeycapBases() {
        return keycapBases;
    }

    public boolean skipEmojiSequence(String string) {
        EmojiData emojiData = this;
        if (string.equals(" ") 
                || string.equals("\t") 
                || string.equals(Emoji.EMOJI_VARIANT_STRING) 
                || string.equals(Emoji.TEXT_VARIANT_STRING)
                || string.equals(Emoji.JOINER_STRING)) {
            return true;
        }
        if (!emojiData.getSortingChars().contains(string) 
                && !emojiData.getZwjSequencesNormal().contains(string)
                ) {
            return true;
        }
        return false;
    }

    private static final VersionInfo UCD9 = VersionInfo.getInstance(9,0);
    private static final VersionInfo Emoji3 = VersionInfo.getInstance(3,0);
    private static final VersionInfo Emoji2 = VersionInfo.getInstance(2,0);

    public static EmojiData forUcd(VersionInfo versionInfo) {
        return EmojiData.of(versionInfo.equals(UCD9) ? Emoji3 : Emoji2);
    }

    static final UnicodeSet             JCARRIERS     = new UnicodeSet()
    .addAll(Emoji.BETA.load(UcdProperty.Emoji_DCM).keySet())
    .addAll(Emoji.BETA.load(UcdProperty.Emoji_KDDI).keySet())
    .addAll(Emoji.BETA.load(UcdProperty.Emoji_SB).keySet())
    .removeAll(new UnicodeSet("[:whitespace:]"))
    .freeze();

    public enum VariantHandling {sequencesOnly, all}

    /**
     * Add EVS to sequences where needed (and remove where not)
     * @param source
     * @param variantHandling TODO
     * @return
     */
    public String addEmojiVariants(String source, char variant, VariantHandling variantHandling) {
        if (variantHandling == VariantHandling.sequencesOnly) {
            if (!UTF16.hasMoreCodePointsThan(source, 1)) {
                return source;
            }
        } 
        StringBuilder result = new StringBuilder();
        int[] sequences = CharSequences.codePoints(source);
        for (int i = 0; i < sequences.length; ++i) {
            int cp = sequences[i];
            // remove VS so we start with a clean slate
            if (Emoji.EMOJI_VARIANTS.contains(cp)) {
                continue;
            }
            result.appendCodePoint(cp);
            // TODO fix so that this works with string of characters containing emoji and others.
            if (!defaultPresentationMap.get(DefaultPresentation.emoji).contains(cp)
                    && (!emojiDefectives.contains(cp) || cp == 0x2695)) {
                if (i == sequences.length - 1 
                        || !MODIFIERS.contains(sequences[i+1])) {
                    result.appendCodePoint(variant);
                }
            }
        }
        return result.toString();
    }

    public UnicodeMap<String> getRawNames() {
        return names;
    }
    
    public String getName(String s, boolean toLower) {
        String name = names.get(s);
        if (name != null) {
            return (toLower ? name.toLowerCase(Locale.ENGLISH) : name);
        }
        name = Emoji.NAME.get(s);
        if (name != null) {
            return (toLower ? name.toLowerCase(Locale.ENGLISH) : name);
        }
        name = CandidateData.getInstance().getName(s);
        if (name != null) {
            return (toLower ? name.toLowerCase(Locale.ENGLISH) : name);
        }
        String s2 = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
        if (!s2.equals(s)) {
            return getName(s2, toLower);
        }
        throw new IllegalArgumentException("no name for " + s + " " + Utility.hex(s));
    }
    
    /**
     * Get all characters that are in emoji sequences, but are not singleton emoji.
     * @return
     */
    public UnicodeSet getEmojiDefectives() {
        return emojiDefectives;
    }
    
    public String getFallbackName(String s) {
        final int firstCodePoint = s.codePointAt(0);
        String name = Emoji.NAME.get(firstCodePoint);
        if (s.indexOf(Emoji.ENCLOSING_KEYCAP) >= 0) {
            return "Keycap " + name;
        }
        final int firstCount = Character.charCount(firstCodePoint);
        main:
        if (s.length() > firstCount) {
            int cp2 = s.codePointAt(firstCount);
            final EmojiDatum edata = getData(cp2);
            if (edata != null && ModifierStatus.modifier == edata.modifierStatus) {
                name += ", " + shortName(cp2);
            } else {
                StringBuffer nameBuffer = new StringBuffer();
                boolean sep = false;
                if (s.indexOf(Emoji.JOINER) >= 0) {
                    String title = "";
                    if (s.indexOf(0x1F48B) >= 0) { // KISS MARK
                        title = "Kiss: ";
                    } else if (s.indexOf(0x2764) >= 0) { // HEART
                        title = "Couple with heart: ";
                    } else if (s.indexOf(0x2640) >= 0) {
                        name = nameBuffer.append("FEMALE: ").append(Emoji.NAME.get(s.codePointAt(0))).toString();  
                        break main;
                    } else if (s.indexOf(0x2642) >= 0) {
                        name = nameBuffer.append("MALE: ").append(Emoji.NAME.get(s.codePointAt(0))).toString();  
                        break main;
                    } else if (Emoji.PROFESSION_OBJECT.containsSome(s)) {
                        title = "Role: ";
                    } else if (s.indexOf(0x1F441) < 0) { // !EYE
                        title = "Family: ";
                    }
                    nameBuffer.append(title);
                }
                for (int cp : CharSequences.codePoints(s)) {
                    if (cp == Emoji.JOINER || cp == Emoji.EMOJI_VARIANT || cp == 0x2764 || cp == 0x1F48B) { // heart, kiss
                        continue;
                    }
                    if (sep) {
                        nameBuffer.append(", ");
                    } else {
                        sep = true;
                    }
                    nameBuffer.append(Emoji.NAME.get(cp));
    
                    //                    nameBuffer.append(cp == Emoji.JOINER ? "zwj" 
                    //                            : cp == Emoji.EMOJI_VARIANT ? "emoji-vs" 
                    //                                    : NAME.get(cp));
                }
                name = nameBuffer.toString();
            }
        }
        return name == null ? "UNNAMED" : name;
    }

    static String shortName(int cp2) {
        return Emoji.NAME.get(cp2).substring("emoji modifier fitzpatrick ".length());
    }
    
    public String normalizeVariant(String emojiSequence) {
        String result = toNormalizedVariant.get(emojiSequence);
        if (result != null) {
            return result;
        }
        if (emojiSequence.contains(Emoji.EMOJI_VARIANT_STRING)) {
            String trial = emojiSequence.replace(Emoji.EMOJI_VARIANT_STRING, "");
            result = toNormalizedVariant.get(trial);
            if (result != null) {
                return result;
            } else {
                return trial;
            }
        }
        return emojiSequence;
    }
    
    public static void main(String[] args) {
        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
        System.out.println("Version " + GenerateEnums.ENUM_VERSION);
        final IndexUnicodeProperties beta = IndexUnicodeProperties.make(Age_Values.V9_0);
        final UnicodeMap<String> betaNames = beta.load(UcdProperty.Name);
        final UnicodeMap<String> names = latest.load(UcdProperty.Name);
        final UnicodeMap<Age_Values> ages = beta.loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class);
        EmojiData emojiData3 = new EmojiData(VersionInfo.getInstance(3));
        EmojiData emojiData2 = new EmojiData(VersionInfo.getInstance(2));

        show(0x1f946, names, emojiData3);
        show(0x1f93b, names, emojiData3);

        UnicodeSet overlap = new UnicodeSet(emojiData3.getModifierBases()).retainAll(emojiData3.getDefaultPresentationSet(DefaultPresentation.text));
        System.out.println("ModifierBase + TextPresentation: " + overlap.size() + "\t" + overlap.toPattern(false));
        for (String s : overlap) {
            System.out.println(Utility.hex(s) + "\t" + s + "\t" + ages.get(s) + "\t" +  names.get(s));
        }

        System.out.println("v2 SingletonsWithDefectives " + emojiData2.getSingletonsWithDefectives().size() 
                + "\t" + emojiData2.getSingletonsWithDefectives());

        System.out.println("SingletonsWithDefectives " + emojiData3.getSingletonsWithDefectives().size() 
                + "\t" + emojiData3.getSingletonsWithDefectives());
        System.out.println("Defectives " + -(emojiData3.getSingletonsWithDefectives().size() - emojiData3.getSingletonsWithoutDefectives().size()));
        System.out.println("Flag Sequences " + emojiData3.getFlagSequences().size());
        System.out.println("ModiferSequences " + emojiData3.getModifierSequences().size());
        System.out.println("Keycap Sequences " + emojiData3.getKeycapSequences().size() + "\t" + emojiData3.getKeycapSequences().toPattern(false));
        System.out.println("Zwj Sequences " + emojiData3.getZwjSequencesNormal().size() + "\t" + emojiData3.getZwjSequencesNormal().toPattern(false));

        System.out.println("modifier" + ", " + emojiData3.getModifierStatusSet(ModifierStatus.modifier).toPattern(false));
        System.out.println(Emoji.CharSource.WDings  + ", " + emojiData3.getCharSourceSet(Emoji.CharSource.WDings).toPattern(false));
        System.out.println(DefaultPresentation.emoji + ", " + emojiData3.getDefaultPresentationSet(DefaultPresentation.emoji).toPattern(false));
        show(0x1F3CB, names, emojiData3);
        show(0x1F3CB, names, emojiData2);
        UnicodeSet keys = new UnicodeSet(emojiData3.keySet()).addAll(emojiData2.keySet());
        System.out.println("Diffs");
        for (String key : keys) {
            EmojiDatum datum = emojiData2.data.get(key);
            EmojiDatum other = emojiData3.data.get(key);
            if (!Objects.equals(datum, other)) {
                //System.out.println("\n" + key + "\t" + Utility.hex(key) + "\t" + names.get(key));
                show(key, ages, betaNames, emojiData3);
                //show(key, ages, betaNames, emojiData2);
            }
        }
        System.out.println("Keycap0 " + emojiData3.getSortingChars().contains("0" + Emoji.KEYCAP_MARK_STRING));
        System.out.println("KeycapE " + emojiData3.getSortingChars().contains("0" + Emoji.EMOJI_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING));
        System.out.println("KeycapT " + emojiData3.getSortingChars().contains("0" + Emoji.TEXT_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING));
    }
}