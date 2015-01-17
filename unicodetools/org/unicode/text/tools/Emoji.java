package org.unicode.text.tools;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.With;
import org.unicode.text.utility.Utility;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class Emoji {

    public static final UnicodeSet EMOJI_CHARS = new UnicodeSet(
            "[🕉 ✡ ☸ ☯ ✝ ☦ ⛩ ☪ ⚛ 0-9©®‼⁉℗™ℹ↔-↙↩↪⌚⌛⌨⎈⏏⏩-⏺Ⓜ▪▫▶◀●◪◻-◾☀-☄☎-☒☔☕☘-☠☢-☤☦🕉☦ ☪ ☬ ☸ ✝ 🕉☪-☬☮☯☹-☾♈-♓♠-♯♲"
                    + "♻♾♿⚐-⚜⚠⚡⚪⚫⚰⚱⚽-⚿⛄-⛈⛍-⛙⛛-⛡⛨-⛪⛰-⛵⛷-⛺⛼-✒✔-✘✝✨✳✴❄❇❌❎❓-❕❗❢-❧➕-➗"
                    + "➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕⸙〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆏🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌬🌰-🍽🎀-🏎"
                    + "🏔-🏷🐀-📾🔀-🔿🕊🕐-🕱🕳-🕹🖁-🖣🖥-🖩🖮-🗳🗺-🙂🙅-🙏🚀-🛏🛠-🛬🛰-🛳"
                    + "{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]")
    .removeAll(new UnicodeSet("[☫☬🎕⚘]"))
    .removeAll(new UnicodeSet("[℗⏴-⏷●◪☙☤☼-☾♩-♯♾⚐⚑⚕⚚ ⚿⛆⛍⛐⛒⛕-⛙⛛⛜⛞-⛡⛨⛼⛾-✀✆✇✑ ❢❦❧🌢🌣🎔🎘🎜🎝🏱🏲🏶📾🔾🔿🕨-🕮🕱🖁-🖆 🖈🖉🖎🖏🖒-🖔🖗-🖣🖦🖧🖩🖮-🖰🖳-🖻🖽-🗁 🗅-🗐🗔-🗛🗟🗠🗤-🗮🗰-🗲🛆-🛈🛦-🛨🛪 🛱🛲]"))
    .removeAll(new UnicodeSet("[🛉 🛊 🖑🗢☏☐☒☚-☜☞☟♲⛇✁✃✄✎✐✕✗✘  ♤  ♡  ♢ ♧❥🆏 ☻ ⛝ 0  1  2  3  4 5  6  7  8  9]"))
    ;
    static final UnicodeSet ASCII_LETTER_HYPHEN = new UnicodeSet('-', '-', 'A', 'Z', 'a', 'z', '’', '’').freeze();
    static final UnicodeSet LATIN1_LETTER = new UnicodeSet("[[:L:]&[\\x{0}-\\x{FF}}]]").freeze();
    static final UnicodeSet KEYWORD_CHARS = new UnicodeSet(Emoji.ASCII_LETTER_HYPHEN)
    .add('0','9')
    .addAll(" +:.")
    .addAll(LATIN1_LETTER)
    .freeze();

    static final UnicodeSet GITHUB_APPLE_CHARS = new UnicodeSet(
            "[‼⁉™ℹ↔-↙↩↪⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✂✅✈-✌✏✒✔✖✨✳✴❄❇❌❎❓-❕❗❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌟🌰-🌵🌷-🍼🎀-🎓🎠-🏄🏆-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔇🔉-🔽🕐-🕧🗻-🙀🙅-🙏🚀-🚊🚌-🛅{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]")
    .freeze();

    static public String buildFileName(String chars, String separator) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (int cp : With.codePointArray(chars)) {
            if (first) {
                first = false;
            } else {
                result.append(separator);
            }
            result.append(Utility.hex(cp).toLowerCase(Locale.ENGLISH));
        }
        return  result.toString();
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

    static final int FIRST_REGIONAL = 0x1F1E6;
    static final int LAST_REGIONAL = 0x1F1FF;

    //    static final UnicodeSet EXCLUDE = new UnicodeSet(
    //    "[🂠-🂮 🂱-🂿 🃁-🃎 🃑-🃵 🀀-🀃 🀅-🀫 〠🕲⍾☸🀰-🂓 🙬 🙭 🙮 🙯🗴🗵🗶🗷🗸🗹★☆⛫\uFFFC⛤-⛧ ⌤⌥⌦⌧⌫⌬⎆⎇⎋⎗⎘⎙⎚⏣⚝⛌⛚⛬⛭⛮⛯⛶⛻✓🆊\\U0001F544-\\U0001F549" +
    //    "☖  ☗  ⛉  ⛊  ⚀  ⚁  ⚂  ⚃  ⚄  ⚅ ♔  ♕  ♖  ♗  ♘  ♙  ♚  ♛  ♜  ♝  ♞  ♟  ⛀  ⛁  ⛂ ⛃" +
    //    "]").freeze();
    //    // 🖫🕾🕿🕻🕼🕽🕾🕿🖀🖪🖬🖭

    static {
        System.out.println(EMOJI_CHARS.toPattern(false));
    }
    static final UnicodeSet FLAGS = new UnicodeSet();
    static {
        CLDRConfig config = CLDRConfig.getInstance();
        StandardCodes sc = config.getStandardCodes();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        Set<String> container = new TreeSet();
        Set<String> contained = new TreeSet();
        for (Entry<String, String> territoryToContained : sdi.getTerritoryToContained().entrySet()) {
            container.add(territoryToContained.getKey());
            contained.add(territoryToContained.getValue());
        }
        contained.removeAll(container);
        contained.add("EU"); // special case
        Map<String, R2<List<String>, String>> aliasInfo = sdi.getLocaleAliasInfo().get("territory");
        contained.removeAll(aliasInfo.keySet());
        for (String s: contained) {
            //System.out.println(s + "\t" + config.getEnglish().getName("territory", s));
            FLAGS.add(getHexFromFlagCode(s));
        }
        FLAGS.freeze();
        EMOJI_CHARS.addAll(FLAGS).freeze();
    }

    static final Transform<String,String> APPLE_URL = new Transform<String,String>() {
        public String transform(String s) {
            StringBuilder result = 
                    new StringBuilder(
                            Emoji.APPLE_LOCAL.containsAll(s) ? "images/apple-extras/apple-" 
                                    : "http://emojistatic.github.io/images/64/");
            boolean first = true;
            for (int cp : With.codePointArray(s)) {
                if (first) {
                    first = false;
                } else {
                    result.append("-");
                }
                result.append(com.ibm.icu.impl.Utility.hex(cp).toLowerCase(Locale.ENGLISH));
            }
            return  result.append(".png").toString();
        }
    };

    static final Transform<String,String> TWITTER_URL = new Transform<String,String>() {
        public String transform(String s) {
            StringBuilder result = new StringBuilder("https://abs.twimg.com/emoji/v1/72x72/");
            boolean first = true;
            for (int cp : With.codePointArray(s)) {
                if (first) {
                    first = false;
                } else {
                    result.append("-");
                }
                result.append(Integer.toHexString(cp));
            }
            return  result.append(".png").toString();
        }
    };
    static final UnicodeSet APPLE_LOCAL = new UnicodeSet("[🌠 🔈 🚋{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]").freeze();

    public static boolean isRegionalIndicator(int firstCodepoint) {
        return FIRST_REGIONAL <= firstCodepoint && firstCodepoint <= Emoji.LAST_REGIONAL;
    }

    static final char ENCLOSING_KEYCAP = '\u20E3';
    static final Comparator<String> CODEPOINT_LENGTH = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.codePointCount(0, o1.length()) - o2.codePointCount(0, o2.length());
        }
    };

    public static final String OUTPUT_DIR = "/Users/markdavis/workspace/unicode-draft/Public/emoji/1.0/";

    static final String TR51_OUTPUT_DIR = "/Users/markdavis/workspace/unicode-draft/reports/tr51/";
    static final String IMAGES_OUTPUT_DIR = TR51_OUTPUT_DIR + "images/";

    public static void main(String[] args) {

    }

    //    private static final UnicodeSet FITZ_OPTIONAL = new UnicodeSet("[\\u261D \\u261F \\u2639-\\u263B \\u270A-\\u270D \\U0001F3C2-\\U0001F3C4 \\U0001F3C7 \\U0001F3CA \\U0001F440-\\U0001F450 \\U0001F47F \\U0001F483 \\U0001F485 \\U0001F48B \\U0001F4AA \\U0001F58E-\\U0001F597 \\U0001F59E-\\U0001F5A3 \\U0001F5E2 \\U0001F600-\\U0001F637 \\U0001F641 \\U0001F642 \\U0001F64C \\U0001F64F \\U0001F6A3 \\U0001F6B4-\\U0001F6B6 \\U0001F6C0]");
    //    private static final UnicodeSet FITZ_MINIMAL = new UnicodeSet("[\\U0001F385 \\U0001F466- \\U0001F478 \\U0001F47C \\U0001F481 \\U0001F482 \\U0001F486 \\U0001F487 \\U0001F48F \\U0001F491 \\U0001F645- \\U0001F647 \\U0001F64B \\U0001F64D \\U0001F64E]");
    static final UnicodeSet ASCII_LETTERS = new UnicodeSet("[A-Za-z]").freeze();
    static final char EMOJI_VARIANT = '\uFE0F';
    static final String EMOJI_VARIANT_STRING = String.valueOf(EMOJI_VARIANT);
    static final char TEXT_VARIANT = '\uFE0E';
    static final String TEXT_VARIANT_STRING = String.valueOf(TEXT_VARIANT);

    public static boolean skipEmojiSequence(String string) {
        if (string.equals(" ") 
                || string.equals(EMOJI_VARIANT_STRING) 
                || string.equals(TEXT_VARIANT_STRING)
                || !EMOJI_CHARS.contains(string)) {
            return true;
        }
        return false;
    }


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

    static String getEmojiSequence(String line, int i) {
        // it is base + variant? + keycap
        // or
        // RI + RI + variant?
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
        //        if ((secondCodepoint == EMOJI_VARIANT || secondCodepoint == TEXT_VARIANT) && i + firstLen + secondLen < line.length()) {
        //            int codePoint3 = line.codePointAt(i+firstLen+secondLen);
        //            int len3 = Character.charCount(codePoint3);
        //            if (codePoint3 == ENCLOSING_KEYCAP) {
        //                return line.substring(i, i+firstLen+secondLen+len3);
        //            }
        //        }
        return line.substring(i, i+firstLen);
    }

    static final UnicodeSet U80 = new UnicodeSet("[🌭🌮🌯🍾🍿🏏🏐🏑🏒🏓🏸🏹🏺🏻🏼🏽🏾🏿📿🕋🕌🕍🕎🙃🙄🛐🤀🤐🤑🤒🤓🤔🤕🤖🤗🤘🦀🦁🦂🦃🦄🧀]").freeze();
    static final UnicodeSet APPLE = new UnicodeSet("[©®‼⁉™ℹ↔-↙↩↪⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✂✅✈-✌✏✒✔✖✨✳✴❄❇❌❎❓-❕❗❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌠🌰-🌵🌷-🍼🎀-🎓🎠-🏄🏆-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔽🕐-🕧🗻-🙀🙅-🙏🚀-🛅{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]").freeze();
}
