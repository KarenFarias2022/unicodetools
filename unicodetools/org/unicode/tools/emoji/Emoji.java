package org.unicode.tools.emoji;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class Emoji {

	/**
     * Set the following to true iff the beta is available. The main function is to add pointers between the release and beta charts.
     */
    static final boolean BETA_IS_OPEN = true;

    /**
     * Constants for versions
     */
    public static final VersionInfo VERSION5 = VersionInfo.getInstance(5);
    public static final VersionInfo VERSION4 = VersionInfo.getInstance(4);
    public static final VersionInfo VERSION3 = VersionInfo.getInstance(3);
    public static final VersionInfo VERSION2 = VersionInfo.getInstance(2);
    public static final VersionInfo VERSION1 = VersionInfo.getInstance(1);

    public static final VersionInfo UCD10 = VersionInfo.getInstance(10);
    public static final VersionInfo UCD9 = VersionInfo.getInstance(9);
    public static final VersionInfo UCD8 = VersionInfo.getInstance(8);

    /**
     * Change each following once we release. That is, VERSION_LAST_RELEASED* becomes VERSION_BETA*, and both the latter increment.
     * Also add to EMOJI_TO_UNICODE_VERSION
     */
    public static final VersionInfo VERSION_LAST_RELEASED2 = VERSION3;
    public static final VersionInfo VERSION_LAST_RELEASED = VERSION4;
    public static final VersionInfo VERSION_BETA = VERSION5;
    
    public static Map<VersionInfo, VersionInfo> EMOJI_TO_UNICODE_VERSION = ImmutableMap.of(
            VERSION5, UCD10,
            VERSION4, UCD9,
            VERSION3, UCD9,
            VERSION2, UCD8,
            VERSION1, UCD8
            );
    
    public static Map<VersionInfo, String> EMOJI_TO_DATE = ImmutableMap.of(
            VERSION5, "2017H1",
            VERSION4, "2016-11-22",
            VERSION3, "2016-06-03",
            VERSION2, "2015-11-12",
            VERSION1, "2015-06-09"
            );

    public static final VersionInfo VERSION_LAST_RELEASED_UNICODE = EMOJI_TO_UNICODE_VERSION.get(VERSION_LAST_RELEASED);
    public static final VersionInfo VERSION_BETA_UNICODE = EMOJI_TO_UNICODE_VERSION.get(VERSION_BETA);
    
    private static final String BETA_PLAIN = " — Beta";
    private static final String BETA_COLORED = "<span style='color:red'><i> — Beta</i></span>";
    
    /**
     * Change the following according to whether we are generating the beta version of files, or the new version.
     * We support generating the last version in order to make improvements to the charts.
     */
    public static final boolean IS_BETA = CldrUtility.getProperty("emoji-beta", true); // default to true

    //public static final VersionInfo VERSION_FORMAT1 = VersionInfo.getInstance(1);

    /**
     * Computed
     */
    
    public static final String            BETA_TITLE_AFFIX          = Emoji.IS_BETA ? BETA_PLAIN : "";
    public static final String            BETA_HEADER_AFFIX          = Emoji.IS_BETA ? BETA_COLORED : "";

    public static final String VERSION_LAST_RELEASED_STRING = VERSION_LAST_RELEASED.getVersionString(2, 4);
    public static final String VERSION_BETA_STRING = VERSION_BETA.getVersionString(2, 4) + BETA_COLORED;


    public static final VersionInfo VERSION_TO_GENERATE = IS_BETA ? VERSION_BETA : VERSION_LAST_RELEASED;
    public static final VersionInfo VERSION_TO_GENERATE_PREVIOUS = IS_BETA ? VERSION_LAST_RELEASED : VERSION_LAST_RELEASED2;
    
    public static final String VERSION_STRING = VERSION_TO_GENERATE.getVersionString(2, 4);

    public static final VersionInfo VERSION_TO_GENERATE_UNICODE = IS_BETA ? VERSION_BETA_UNICODE : VERSION_LAST_RELEASED_UNICODE;

    public static final String TR51_SVN_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr51/";
    //public static final String TR51_PREFIX = IS_BETA ? "internal-beta/" : "internal/";

    public static final String TR51_INTERNAL_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr51/"
            + (IS_BETA ? "internal-beta/" : "internal/");
    public static final String CHARTS_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "emoji/" 
            + (IS_BETA ? "charts-beta/" : "charts/");
    public static final String RELEASE_CHARTS_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "emoji/charts/";
    public static final String DATA_DIR = Settings.UNICODE_DRAFT_PUBLIC + "emoji/" + VERSION_STRING + "/";

    static final String IMAGES_OUTPUT_DIR = TR51_SVN_DIR + "images/";

    public enum ModifierStatus {
        none, modifier, modifier_base;
    }
    
    // HACK
    static final UnicodeSet GENDER_BASE = new UnicodeSet("[👯💂👳👱⛹🏃🏄🏊-🏌👮👷💁💆💇🕵🙅-🙇🙋🙍🙎🚣 🚴-🚶🤹 \\U0001F926\\U0001F937\\U0001F938\\U0001F93C-\\U0001F93E]")
    .freeze();
    static final UnicodeSet PROFESSION_OBJECT = new UnicodeSet("[⚕🌾🍳🎓🎤🏫🏭💻💼🔧🔬]")
    .freeze();

    public enum Source {
        // also used for accessing pngs; order is important
        // if a source is in developer release, add ᵈ to the name, eg "Googᵈ"
        charOverride,
        color, 
        apple("Appl"), google("Goog"), 
        emojipedia, emojixpress, // put these here, so they are picked up for candidates first
        twitter("Twtr."), emojione("One"),
        fb("FB", "Facebook"), fbm("FBM", "Messenger (Facebook)"), samsung("Sams."), 
        windows("Wind."),
        ref, emojination, adobe, sample, proposed, 
        // gifs; don't change order!
        gmail("GMail"), sb("SB", "SoftBank"), dcm("DCM", "DoCoMo"), kddi("KDDI", "KDDI");
        
        static final Set<Source> OLD_SOURCES = ImmutableSet.copyOf(
                EnumSet.of(gmail, sb, dcm, kddi)); // do this to get same order as Source
        static final Set<Source> VENDOR_SOURCES = ImmutableSet.copyOf(
                EnumSet.of(apple, google, twitter, emojione, samsung, fb, windows)); // do this to get same order as Source
        static final Set<Emoji.Source> platformsToIncludeNormal = ImmutableSet.copyOf(EnumSet.of(
                Source.apple, Source.google, Source.windows, Source.twitter, Source.emojione, Source.samsung, 
                Source.fb, Source.fbm,
                Source.gmail, Source.dcm, Source.kddi, Source.sb
                ));

        private final String shortName;
        private final String longName;
        
        private Source() {
            this(null, null);
        }
        private Source(String shortName) {
            this(shortName, null);
        }
        private Source(String shortName, String longName) {
            this.shortName = shortName != null ? shortName : UCharacter.toTitleCase(name(), null);
            this.longName = longName != null ? longName : UCharacter.toTitleCase(name(), null);
        }
        
        boolean isGif() {
            return compareTo(Source.gmail) >= 0;
        }

        String getClassAttribute(String chars) {
            if (isGif()) {
                return "imgs";
            }
            String className = "imga";
            if (this == Source.ref && Emoji.getFlagCode(chars) != null) {
                className = "imgf";
            }
            return className;
        }

        public String getPrefix() {
            return this == google ? "android" : name();
        }

        public String shortName() {
            return shortName;
        }
        
        @Override
        public String toString() {
            return longName;
        }
    }


    enum CharSource {
        ZDings("ᶻ", "z"),
        ARIB("ª", "a"),
        JCarrier("ʲ", "j"),
        WDings("ʷ", "w"),
        Other("ˣ", "x");
        final String superscript;
        final String letter;
    
        private CharSource(String shortString, String letter) {
            this.superscript = shortString;
            this.letter = letter;
        }
    }


    public enum Qualified {all, first, none}

	public static final int TAG_TERM_CHAR = 0xE007F;
    public static final String TAG_TERM = UTF16.valueOf(TAG_TERM_CHAR);

    public static final char KEYCAP_MARK = '\u20E3';
    public static final String KEYCAP_MARK_STRING = String.valueOf(KEYCAP_MARK);
//    private static final UnicodeSet Unicode8Emoji = new UnicodeSet("[\\x{1F3FB}\\x{1F3FC}\\x{1F3FD}\\x{1F3FE}\\x{1F3FF}\\x{1F4FF}\\x{1F54B}\\x{1F54C}\\x{1F54D}"
//            +"\\x{1F54E}\\x{1F6D0}\\x{1F32D}\\x{1F32E}\\x{1F32F}\\x{1F37E}\\x{1F37F}\\x{1F983}\\x{1F984}\\x{1F9C0}"
//            +"\\x{1F3CF}\\x{1F3D0}\\x{1F3D1}\\x{1F3D2}\\x{1F3D3}\\x{1F3F8}\\x{1F3F9}\\x{1F3FA}\\x{1F643}"
//            +"\\x{1F644}\\x{1F910}\\x{1F911}\\x{1F912}\\x{1F913}\\x{1F914}\\x{1F915}\\x{1F916}\\x{1F917}"
//            +"\\x{1F918}\\x{1F980}\\x{1F981}\\x{1F982}]").freeze();
    //            new UnicodeSet(
//            "[🕉 ✡ ☸ ☯ ✝ ☦ ⛩ ☪ ⚛ 0-9©®‼⁉℗™ℹ↔-↙↩↪⌚⌛⌨⎈⏏⏩-⏺Ⓜ▪▫▶◀●◪◻-◾☀-☄☎-☒☔☕☘-☠☢-☤☦🕉☦ ☪ ☬ ☸ ✝ 🕉☪-☬☮☯☹-☾♈-♓♠-♯♲"
//                    + "♻♾♿⚐-⚜⚠⚡⚪⚫⚰⚱⚽-⚿⛄-⛈⛍-⛙⛛-⛡⛨-⛪⛰-⛵⛷-⛺⛼-✒✔-✘✝✨✳✴❄❇❌❎❓-❕❗❢-❧➕-➗"
//                    + "➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕⸙〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆏🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌬🌰-🍽🎀-🏎"
//                    + "🏔-🏷🐀-📾🔀-🔿🕊🕐-🕱🕳-🕹🖁-🖣🖥-🖩🖮-🗳🗺-🙂🙅-🙏🚀-🛏🛠-🛬🛰-🛳"
//                    + "{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]")
//    .addAll(Unicode8Emoji)
//    .removeAll(new UnicodeSet("[☫☬🎕⚘⸙⎈]"))
//    .removeAll(new UnicodeSet("[℗⏴-⏷●◪☙☤☼-☾♩-♯♾⚐⚑⚕⚚ ⚿⛆⛍⛐⛒⛕-⛙⛛⛜⛞-⛡⛨⛼⛾-✀✆✇✑ ❢❦❧🌢🌣🎔🎘🎜🎝🏱🏲🏶📾🔾🔿🕨-🕮🕱🖁-🖆 🖈🖉🖎🖏🖒-🖔🖗-🖣🖦🖧🖩🖮-🖰🖳-🖻🖽-🗁 🗅-🗐🗔-🗛🗟🗠🗤-🗮🗰-🗲🛆-🛈🛦-🛨🛪 🛱🛲]"))
//    .removeAll(new UnicodeSet("[🛉 🛊 🖑🗢☏☐☒☚-☜☞☟♲⛇✁✃✄✎✐✕✗✘  ♤  ♡  ♢ ♧❥🆏 ☻ ⛝ 0  1  2  3  4 5  6  7  8  9]"))
//    .add("🗨")
//    // .freeze() will freeze later
//    ;
//    static {
//        if (IS_BETA) {
//            EMOJI_CHARS.addAll("[🕺 🖤 🛑 🛒 🛴 🛵 🛶 🤙 🤚 🤛 🤜 🤝 🤞 🤠 🤡 🤢 🤣 🤤 🤥 🤦 🤧 🤰 🤳 🤴 🤵 🤶 🤷 🤸 🤹 🤺 🤻 🤼 🤽 🤾 🥀 🥁 🥂 🥃 🥄 🥅 🥆 🥇 🥈 🥉 🥊 🥋 🥐 🥑 🥒 🥓 🥔 🥕 🥖 🥗 🥘 🥙 🥚 🥛 🥜 🥝 🥞 🦅 🦆 🦇 🦈 🦉 🦊 🦋 🦌 🦍 🦎 🦏 🦐 🦑]");
//        }
//    }
    public static final UnicodeSet COMMON_ADDITIONS = new UnicodeSet("[➿🌍🌎🌐🌒🌖-🌘🌚🌜-🌞🌲🌳🍋🍐🍼🏇🏉🏤🐀-🐋🐏🐐🐓🐕🐖🐪👥👬👭💭💶💷📬📭📯📵🔀-🔂🔄-🔉🔕🔬🔭🕜-🕧😀😇😈😎😐😑😕😗😙😛😟😦😧😬😮😯😴😶🚁🚂🚆🚈🚊🚋🚍🚎🚐🚔🚖🚘🚛-🚡🚣🚦🚮-🚱🚳-🚵🚷🚸🚿🛁-🛅]").freeze();
    static final UnicodeSet ASCII_LETTER_HYPHEN = new UnicodeSet('-', '-', 'A', 'Z', 'a', 'z', '’', '’').freeze();
    static final UnicodeSet LATIN1_LETTER = new UnicodeSet("[[:L:]&[\\x{0}-\\x{FF}}]]").freeze();
    static final UnicodeSet KEYWORD_CHARS = new UnicodeSet(Emoji.ASCII_LETTER_HYPHEN)
    .add('0','9')
    .addAll(" +:.&")
    .addAll(LATIN1_LETTER)
    .freeze();
    static final UnicodeSet KEYCAPS = new UnicodeSet("[{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]").freeze();

    //public static final UnicodeSet SKIP_ANDROID = new UnicodeSet("[♨ ⚠ ▶ ◀ ✉ ✏ ✒ ✂ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ▪ ▫ ◻ ◼ ‼ ⁉ 〰 © ® 🅰 🅱 ℹ Ⓜ 🅾 🅿 ™ 🈂 🈷 ㊗ ㊙]").freeze();

    static public String buildFileName(String chars, String separator) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (int cp : With.codePointArray(chars)) {
            if (cp == Emoji.EMOJI_VARIANT) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                result.append(separator);
            }
            result.append(Utility.hex(cp).toLowerCase(Locale.ENGLISH));
        }
        return result.toString();
    }

    static Pattern DASH_OR_UNDERBAR = Pattern.compile("[-_]");

    static public String parseFileName(boolean hasPrefix, String chars) {
        StringBuilder result = new StringBuilder();
        int dotPos = chars.lastIndexOf('.');
        if (dotPos >= 0) {
            chars = chars.substring(0,dotPos);
        }
        String[] parts = DASH_OR_UNDERBAR.split(chars); //chars.split(separator);
        boolean first = true;
        for (String part : parts) {
            if (part.startsWith("x")) {
                continue;
            }
            if (hasPrefix && first) {
                first = false;
                continue;
            }
            result.appendCodePoint(Integer.parseInt(part,16));
        }
        return  result.toString();
    }

    public static String getHexFromFlagCode(String isoCountries) {
        String cc = new StringBuilder()
        .appendCodePoint(isoCountries.charAt(0) + Emoji.FIRST_REGIONAL - 'A') 
        .appendCodePoint(isoCountries.charAt(1) + Emoji.FIRST_REGIONAL - 'A')
        .toString();
        return cc;
    }

    static String getEmojiFromRegionCode(String chars) {
        return new StringBuilder()
        .appendCodePoint(chars.codePointAt(0) + FIRST_REGIONAL - 'A')
        .appendCodePoint(chars.codePointAt(1) + FIRST_REGIONAL - 'A')
        .toString();
    }

    static String getRegionCodeFromEmoji(String chars) {
        int first = chars.codePointAt(0);
        return new StringBuilder()
        .appendCodePoint(first - FIRST_REGIONAL + 'A')
        .appendCodePoint(chars.codePointAt(Character.charCount(first)) - FIRST_REGIONAL + 'A')
        .toString();
    }

    public static final UnicodeSet FACES = new UnicodeSet("[☺ ☹ 🙁 🙂 😀-😆 😉-😷 😇 😈 👿 🙃 🙄 🤐-🤕 🤗]").freeze();
    
    public static final char JOINER = '\u200D';
    public static final String JOINER_STR = "\u200D";

    public static final char EMOJI_VARIANT = '\uFE0F';
    public static final char TEXT_VARIANT = '\uFE0E';
    
    public static final UnicodeSet EMOJI_VARIANTS = new UnicodeSet().add(EMOJI_VARIANT).add(TEXT_VARIANT).freeze();
    
    public static final UnicodeSet EMOJI_VARIANTS_JOINER = new UnicodeSet(EMOJI_VARIANTS)
    .add(JOINER)
    .freeze();

    //public static final String PERSON = "\u263F";
    public static final String FEMALE = "\u2640";
    public static final String MALE = "\u2642";
    
    public static final int BOY = 0x1F466;
    public static final int WOMAN = 0x1F469;
	public static final int ADULT_CP = 0x1f9d1;
	public static final String ADULT = UTF16.valueOf(ADULT_CP);
	public static final String WOMAN_STR = UTF16.valueOf(WOMAN);

    public static final UnicodeSet FAMILY_MARKERS = new UnicodeSet().add(BOY, WOMAN).freeze();
    public static final UnicodeSet ACTIVITY_MARKER = new UnicodeSet("[🤱 🧖 🧗 🧘🤰 💆 💇 🚶 🏃 💃 🕺 👯 🕴 🗣 👤 👥 🏌 🏄 🚣 🏊 ⛹ 🏋 🚴 🚵 🤸 🤼-🤾 🤹]").freeze();
    public static final UnicodeSet GENDER_MARKERS = new UnicodeSet().add(FEMALE).add(MALE)
//    		.add(PERSON)
    		.freeze();
    public static final UnicodeSet ROLE_MARKER = new UnicodeSet("[\\U0001F9D1 \\U0001F468 \\U0001F469 \\U0001F9D9-\\U0001F9DF 👱 👮 👳 👷 💂 🕵]").freeze();
    
    static final int FIRST_REGIONAL = 0x1F1E6;
    static final int LAST_REGIONAL = 0x1F1FF;

    public static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(FIRST_REGIONAL,LAST_REGIONAL).freeze();
    public static final UnicodeSet DEFECTIVE = new UnicodeSet("[0123456789*#]").addAll(REGIONAL_INDICATORS).freeze();

    //    static final UnicodeSet EXCLUDE = new UnicodeSet(
    //    "[🂠-🂮 🂱-🂿 🃁-🃎 🃑-🃵 🀀-🀃 🀅-🀫 〠🕲⍾☸🀰-🂓 🙬 🙭 🙮 🙯🗴🗵🗶🗷🗸🗹★☆⛫\uFFFC⛤-⛧ ⌤⌥⌦⌧⌫⌬⎆⎇⎋⎗⎘⎙⎚⏣⚝⛌⛚⛬⛭⛮⛯⛶⛻✓🆊\\U0001F544-\\U0001F549" +
    //    "☖  ☗  ⛉  ⛊  ⚀  ⚁  ⚂  ⚃  ⚄  ⚅ ♔  ♕  ♖  ♗  ♘  ♙  ♚  ♛  ♜  ♝  ♞  ♟  ⛀  ⛁  ⛂ ⛃" +
    //    "]").freeze();
    //    // 🖫🕾🕿🕻🕼🕽🕾🕿🖀🖪🖬🖭

    

//    static final UnicodeSet EMOJI_CHARS_WITHOUT_FLAGS = new UnicodeSet(EMOJI_CHARS).freeze();
//    static {
//        CLDRConfig config = CLDRConfig.getInstance();
//        //StandardCodes sc = config.getStandardCodes();
//        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
//        Set<String> container = new TreeSet<>();
//        Set<String> contained = new TreeSet<>();
//        for (Entry<String, String> territoryToContained : sdi.getTerritoryToContained().entrySet()) {
//            container.add(territoryToContained.getKey());
//            contained.add(territoryToContained.getValue());
//        }
//        contained.removeAll(container);
//        contained.add("EU"); // special case
//        Map<String, R2<List<String>, String>> aliasInfo = sdi.getLocaleAliasInfo().get("territory");
//        contained.removeAll(aliasInfo.keySet());
//        for (String s: contained) {
//            //System.out.println(s + "\t" + config.getEnglish().getName("territory", s));
//            FLAGS.add(getHexFromFlagCode(s));
//        }
//        FLAGS.freeze();
//        EMOJI_CHARS.addAll(FLAGS).freeze();
//    }

    public static boolean isRegionalIndicator(int firstCodepoint) {
        return FIRST_REGIONAL <= firstCodepoint && firstCodepoint <= Emoji.LAST_REGIONAL;
    }

    public static final char ENCLOSING_KEYCAP = '\u20E3';
    static final Comparator<String> CODEPOINT_LENGTH = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.codePointCount(0, o1.length()) - o2.codePointCount(0, o2.length());
        }
    };


    public static final UnicodeSet ASCII_LETTERS = new UnicodeSet("[A-Za-z]").freeze();
    public static final String EMOJI_VARIANT_STRING = String.valueOf(EMOJI_VARIANT);
    public static final String TEXT_VARIANT_STRING = String.valueOf(TEXT_VARIANT);
    public static final String JOINER_STRING = String.valueOf(JOINER);

    public static String getLabelFromLine(Output<Set<String>> newLabel, String line) {
        line = line.replace(EMOJI_VARIANT_STRING, "").replace(TEXT_VARIANT_STRING, "").trim();
        int tabPos = line.indexOf('\t');
        //        if (tabPos < 0 && Emoji.EMOJI_CHARS.contains(getEmojiSequence(line, 0))) {
        //            tabPos = line.length();
        //            
        //        }
        if (tabPos >= 0) {
            newLabel.value.clear();
            String[] temp = line.substring(0,tabPos).trim().split(",\\s*");
            for (String part : temp) {
                if (KEYWORD_CHARS.containsAll(part)) {
                    newLabel.value.add(part);
                } else {
                    throw new IllegalArgumentException("Bad line format: " + line);
                }
            }
            line = line.substring(tabPos + 1);
        }
        return line;
    }
    //    private static final Transform<String,String> WINDOWS_URL = new Transform<String,String>() {
    //        public String transform(String s) {
    //            String base = "images /windows/windows_";
    //            String separator = "_";
    //            return base + Emoji.buildFileName(s, separator) + ".png";
    //        }
    //
    //    };

    public static String getEmojiSequence(String line, int i) {
        // take the first character.
        int firstCodepoint = line.codePointAt(i);
        int firstLen = Character.charCount(firstCodepoint);
        if (i + firstLen == line.length()) {
            return line.substring(i, i+firstLen);
        }
        int secondCodepoint = line.codePointAt(i+firstLen);
        int secondLen = Character.charCount(secondCodepoint);
        if (secondCodepoint == ENCLOSING_KEYCAP
                || (isRegionalIndicator(firstCodepoint) && isRegionalIndicator(secondCodepoint))) {
            return line.substring(i, i+firstLen+secondLen);
        }
        if (i+firstLen+secondLen == line.length()) {
            return line.substring(i, i+firstLen);
        }
        if (secondCodepoint == Emoji.JOINER) {
            return line.substring(i, i+firstLen+secondLen) + getEmojiSequence(line, i+firstLen+secondLen);
        }
        return line.substring(i, i+firstLen);
    }

    static final UnicodeSet U80 = new UnicodeSet("[🌭🌮🌯🍾🍿🏏🏐🏑🏒🏓🏸🏹🏺🏻🏼🏽🏾🏿📿🕋🕌🕍🕎🙃🙄🛐🤀🤐🤑🤒🤓🤔🤕🤖🤗🤘🦀🦁🦂🦃🦄🧀]").freeze();
    static final UnicodeSet U90 = new UnicodeSet("[\\x{1F57A} \\x{1F5A4} \\x{1F6D1} \\x{1F6F4} \\x{1F6F5} \\x{1F919} \\x{1F91A} \\x{1F91B} \\x{1F91C} \\x{1F91D} \\x{1F91E} \\x{1F920} \\x{1F921} \\x{1F922} \\x{1F923} \\x{1F924} \\x{1F925} \\x{1F926} \\x{1F930} \\x{1F933} \\x{1F934} \\x{1F935} \\x{1F936} \\x{1F937} \\x{1F940} \\x{1F942} \\x{1F950} \\x{1F951} \\x{1F952} \\x{1F953} \\x{1F954} \\x{1F955} \\x{1F985} \\x{1F986} \\x{1F987} \\x{1F988} \\x{1F989} \\x{1F98A}]").freeze();
    public static final Transliterator UNESCAPE = Transliterator.getInstance("hex-any/Perl");

    static String getImageFilenameFromChars(Emoji.Source type, String chars) {
        chars = chars.replace(Emoji.EMOJI_VARIANT_STRING,"");
//        if (type == Emoji.Source.android && Emoji.SKIP_ANDROID.contains(chars)) { // hack to exclude certain android
//            return null;
//        }
        if (type == Source.charOverride) { 
            Source overrideSource = BEST_OVERRIDE.get(chars);
            if (overrideSource != null) {
                type = overrideSource;
            } else if (ZwjType.getType(chars) != ZwjType.family) {
                overrideSource = BEST_OVERRIDE.get(UTF16.valueOf(chars.codePointAt(0)));
                if (overrideSource != null) {
                    type = overrideSource;
                }
            }
        }

        String core = buildFileName(chars, "_");
        String suffix = ".png";
        if (type != null && type.isGif()) {
            suffix = ".gif";
        }
        return type.getPrefix() + "/" + type.getPrefix() + "_" + core + suffix;
    }

    static String getFlagCode(String chars) {
        int firstCodepoint = chars.codePointAt(0);
        if (!isRegionalIndicator(firstCodepoint)) {
            return null;
        }
        int firstLen = Character.charCount(firstCodepoint);
        int secondCodepoint = firstLen >= chars.length() ? 0 : chars.codePointAt(firstLen);
        if (!isRegionalIndicator(secondCodepoint)) {
            return null;
        }
        secondCodepoint = chars.codePointAt(2);
        String cc = (char) (firstCodepoint - FIRST_REGIONAL + 'A')
                + ""
                + (char) (secondCodepoint - FIRST_REGIONAL + 'A');
        // String remapped = REMAP_FLAGS.get(cc);
        // if (remapped != null) {
        // cc = remapped;
        // }
        // if (REPLACEMENT_CHARACTER.equals(cc)) {
        // return null;
        // }
        return cc;
    }

    static public File getImageFile(Source type, String chars) {
        chars = chars.replace(Emoji.EMOJI_VARIANT_STRING,"");
        String filename = getImageFilenameFromChars(type, chars);
        if (filename != null) {
            File file = new File(IMAGES_OUTPUT_DIR, filename);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }
    
    static final UnicodeMap<Emoji.Source> BEST_OVERRIDE = new UnicodeMap<>();
    static {
        // BEST_OVERRIDE.putAll(new UnicodeSet("[⛹🏃🏄🏊-🏌👨👩👮👯👱👳👷💁💂💆💇🕵🙅-🙇🙋🙍🙎🚣🚴-🚶🤦🤷-🤹🤼-🤾]"), Emoji.Source.google);
        BEST_OVERRIDE.freeze();
    }

    public static File getBestFile(String s, Source... doFirst) {
        for (Source source : Emoji.orderedEnum(doFirst)) {
            if (source == Source.charOverride) { 
                Source overrideSource = BEST_OVERRIDE.get(s);
                if (overrideSource != null) {
                    source = overrideSource;
                } else if (ZwjType.getType(s) != ZwjType.family) {
                    overrideSource = BEST_OVERRIDE.get(s);
                    if (overrideSource != null) {
                        source = overrideSource;
                    }
                }
            }
            File file = getImageFile(source, s);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public static Iterable<Source> orderedEnum(Source... doFirst) {
        if (doFirst.length == 0) {
            return Arrays.asList(Source.values());
        }
        LinkedHashSet<Source> ordered = new LinkedHashSet<>(Arrays.asList(doFirst));
        ordered.addAll(Arrays.asList(Source.values()));
        return ordered;
    }

    public static final IndexUnicodeProperties    LATEST  = IndexUnicodeProperties.make(VERSION_TO_GENERATE_UNICODE);
    public static final IndexUnicodeProperties    BETA  = IndexUnicodeProperties.make(VERSION_BETA_UNICODE);

    static final UnicodeMap<Age_Values>        VERSION_ENUM            = BETA.loadEnum(UcdProperty.Age, Age_Values.class);

    // Certain resources we always load from latest.
    
    static final UnicodeMap<String>        NAME                        = BETA.load(UcdProperty.Name);

    static final LocaleDisplayNames        LOCALE_DISPLAY              = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    static final transient Collection<Age_Values> output = new TreeSet(Collections.reverseOrder());

    static Age_Values getNewest(String s) {
        synchronized (Emoji.output) {
            Emoji.output.clear();
            Emoji.getValues(s, VERSION_ENUM, Emoji.output);
            return Emoji.output.iterator().next();
        }
    }

    // should be method on UnicodeMap
    static final <T, C extends Collection<T>> C getValues(String source, UnicodeMap<T> data, C output) {
        for (int cp : CharSequences.codePoints(source)) {
            T datum = data.get(cp);
            if (datum != null) {
                output.add(datum);
            }
        }
        return output;
    }

    static final String INTERNAL_OUTPUT_DIR = Settings.OTHER_WORKSPACE_DIRECTORY + "Generated/emoji/";
    public static final String HEALTHCARE = "⚕";
    public static final String UN = "🇺🇳";

    public static String toUHex(String s) {
        return "U+" + Utility.hex(s, " U+");
    }
    
    public static String getFlagRegionName(String s) {
        String result = Emoji.getFlagCode(s);
        if (result != null) {
            result = Emoji.LOCALE_DISPLAY.regionDisplayName(result);
            if (result.endsWith(" SAR China")) {
                result = result.substring(0, result.length() - " SAR China".length());
            } else if (result.contains("(")) {
                result = result.substring(0, result.indexOf('(')) + result.substring(result.lastIndexOf(')') + 1);
            }
            result = result.replaceAll("\\s\\s+", " ").trim();
        }
        return result;
    }
    
//    public static void main(String[] args) {
//        if (!EMOJI_CHARS.containsAll(Unicode8Emoji)) {
//            throw new IllegalArgumentException();
//        }
//        if (!EMOJI_CHARS.contains("🗨")) {
//            throw new IllegalArgumentException();
//        }
//        System.out.println(Source.fbm + " " + Source.fbm.shortName());
//        System.out.println("Singletons:\n" + EMOJI_SINGLETONS.toPattern(false));
//        System.out.println("Without flags:\n" + EMOJI_CHARS_WITHOUT_FLAGS.toPattern(false));
//        System.out.println("Flags:\n" + FLAGS.toPattern(false));
//        System.out.println("With flags:\n" + EMOJI_CHARS.toPattern(false));
//        System.out.println("FLAT:\n" + EMOJI_CHARS_FLAT.toPattern(false));
//        System.out.println("FLAT:\n" + EMOJI_CHARS_FLAT.toPattern(true));
//    }

    public static String show(String key) {
        StringBuilder b = new StringBuilder();
        for (int cp : CharSequences.codePoints(key)) {
            if (b.length() != 0) {
                b.append(' ');
            }
            b.append("U+" + Utility.hex(cp) + " " + UTF16.valueOf(cp));
        }
        return b.toString();
    }

}
