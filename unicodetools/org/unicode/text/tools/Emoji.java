package org.unicode.text.tools;

import java.util.Comparator;
import java.util.Locale;

import org.unicode.cldr.util.With;
import org.unicode.text.UCA.UCA;
import org.unicode.text.utility.Utility;

import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class Emoji {

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
    
    static public String parseFileName(String chars, String separator) {
        StringBuilder result = new StringBuilder();
        int dotPos = chars.lastIndexOf('.');
        if (dotPos >= 0) {
            chars = chars.substring(0,dotPos);
        }
        String[] parts = chars.split(separator);
        boolean first = true;
        for (String part : parts) {
            if (first) {
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

    static final int FIRST_REGIONAL = 0x1F1E6;
    public static final UnicodeSet EMOJI_CHARS = new UnicodeSet(
    "[©®‼⁉℗™ℹ↔-↙↩↪⌚⌛⌨⍾⎈⏏⏩-⏺Ⓜ▪▫▶◀◻-◾☀-☄☎-☒☔-☠☢-☤☮☯☹-☾♈-♯♲♻♾-⚅⚐-⚜⚠⚡⚪⚫⚰⚱⚽-⛊⛍-⛙⛛-⛡⛨⛪⛰-⛵⛷-⛺⛼-✒✔-✘✨✳✴❄❇❌❎❓-❕❗❢-❧➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕⸙〠〰〽㊗㊙🀀-🀫🀰-🂓🂠-🂮🂱-🂿🃁-🃏🃑-🃵🅰🅱🅾🅿🆎🆏🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌬🌰-🍽🎀-🏎🏔-🏷🐀-📾🔀-🔿🕊🕐-🕹🖁-🖣🖥-🖩🖮-🗳🗺-🙂🙅-🙏🙬-🙯🚀-🛏🛠-🛬🛰-🛳{*⃣}{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}{🇦🇩}{🇦🇪}{🇦🇫}{🇦🇬}{🇦🇮}{🇦🇱}{🇦🇲}{🇦🇴}{🇦🇶}{🇦🇷}{🇦🇸}{🇦🇹}{🇦🇺}{🇦🇼}{🇦🇽}{🇦🇿}{🇧🇦}{🇧🇧}{🇧🇩}{🇧🇪}{🇧🇫}{🇧🇬}{🇧🇭}{🇧🇮}{🇧🇯}{🇧🇱}{🇧🇲}{🇧🇳}{🇧🇴}{🇧🇶}{🇧🇷}{🇧🇸}{🇧🇹}{🇧🇻}{🇧🇼}{🇧🇾}{🇧🇿}{🇨🇦}{🇨🇨}{🇨🇩}{🇨🇫}{🇨🇬}{🇨🇭}{🇨🇮}{🇨🇰}{🇨🇱}{🇨🇲}{🇨🇳}{🇨🇴}{🇨🇷}{🇨🇺}{🇨🇻}{🇨🇼}{🇨🇽}{🇨🇾}{🇨🇿}{🇩🇪}{🇩🇯}{🇩🇰}{🇩🇲}{🇩🇴}{🇩🇿}{🇪🇨}{🇪🇪}{🇪🇬}{🇪🇭}{🇪🇷}{🇪🇸}{🇪🇹}{🇫🇮}{🇫🇯}{🇫🇰}{🇫🇲}{🇫🇴}{🇫🇷}{🇬🇦}{🇬🇧}{🇬🇩}{🇬🇪}{🇬🇫}{🇬🇬}{🇬🇭}{🇬🇮}{🇬🇱}{🇬🇲}{🇬🇳}{🇬🇵}{🇬🇶}{🇬🇷}{🇬🇸}{🇬🇹}{🇬🇺}{🇬🇼}{🇬🇾}{🇭🇰}{🇭🇲}{🇭🇳}{🇭🇷}{🇭🇹}{🇭🇺}{🇮🇩}{🇮🇪}{🇮🇱}{🇮🇲}{🇮🇳}{🇮🇴}{🇮🇶}{🇮🇷}{🇮🇸}{🇮🇹}{🇯🇪}{🇯🇲}{🇯🇴}{🇯🇵}{🇰🇪}{🇰🇬}{🇰🇭}{🇰🇮}{🇰🇲}{🇰🇳}{🇰🇵}{🇰🇷}{🇰🇼}{🇰🇾}{🇰🇿}{🇱🇦}{🇱🇧}{🇱🇨}{🇱🇮}{🇱🇰}{🇱🇷}{🇱🇸}{🇱🇹}{🇱🇺}{🇱🇻}{🇱🇾}{🇲🇦}{🇲🇨}{🇲🇩}{🇲🇪}{🇲🇫}{🇲🇬}{🇲🇭}{🇲🇰}{🇲🇱}{🇲🇲}{🇲🇳}{🇲🇴}{🇲🇵}{🇲🇶}{🇲🇷}{🇲🇸}{🇲🇹}{🇲🇺}{🇲🇻}{🇲🇼}{🇲🇽}{🇲🇾}{🇲🇿}{🇳🇦}{🇳🇨}{🇳🇪}{🇳🇫}{🇳🇬}{🇳🇮}{🇳🇱}{🇳🇴}{🇳🇵}{🇳🇷}{🇳🇺}{🇳🇿}{🇴🇲}{🇵🇦}{🇵🇪}{🇵🇫}{🇵🇬}{🇵🇭}{🇵🇰}{🇵🇱}{🇵🇲}{🇵🇳}{🇵🇷}{🇵🇸}{🇵🇹}{🇵🇼}{🇵🇾}{🇶🇦}{🇷🇪}{🇷🇴}{🇷🇸}{🇷🇺}{🇷🇼}{🇸🇦}{🇸🇧}{🇸🇨}{🇸🇩}{🇸🇪}{🇸🇬}{🇸🇭}{🇸🇮}{🇸🇯}{🇸🇰}{🇸🇱}{🇸🇲}{🇸🇳}{🇸🇴}{🇸🇷}{🇸🇸}{🇸🇹}{🇸🇻}{🇸🇽}{🇸🇾}{🇸🇿}{🇹🇨}{🇹🇩}{🇹🇫}{🇹🇬}{🇹🇭}{🇹🇯}{🇹🇰}{🇹🇱}{🇹🇲}{🇹🇳}{🇹🇴}{🇹🇷}{🇹🇹}{🇹🇻}{🇹🇼}{🇹🇿}{🇺🇦}{🇺🇬}{🇺🇲}{🇺🇸}{🇺🇾}{🇺🇿}{🇻🇦}{🇻🇨}{🇻🇪}{🇻🇬}{🇻🇮}{🇻🇳}{🇻🇺}{🇼🇫}{🇼🇸}{🇽🇰}{🇾🇪}{🇾🇹}{🇿🇦}{🇿🇲}{🇿🇼}]")
    .freeze();
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

    static final int LAST_REGIONAL = 0x1F1FF;
    static final char ENCLOSING_KEYCAP = '\u20E3';
    static final Comparator<String> CODEPOINT_LENGTH = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.codePointCount(0, o1.length()) - o2.codePointCount(0, o2.length());
        }
    };



}
