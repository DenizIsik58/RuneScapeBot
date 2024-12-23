package scripts.api.utility;

import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Widget;
import org.tribot.script.sdk.util.Retry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringsUtility {

    public static Matcher getMatcher(String regex, String input) {
        final Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(input);
    }

    public static boolean hasMatches(String regex, String input) {
        return getMatcher(regex, input).find();
    }

    public static String extractFirstMatchGroup(String regex, String input) {
        var matchGroups = extractMatchGroups(regex, input);
        if (matchGroups.isEmpty()) return "";
        return matchGroups.get(0);
    }

    public static String extractLastMatchGroup(String regex, String input) {
        var matchGroups = extractMatchGroups(regex, input);
        if (matchGroups.isEmpty()) return "";
        return matchGroups.get(matchGroups.size() - 1);
    }

    public static List<String> extractMatchGroups(String regex, String input) {
        var matcher = getMatcher(regex, input);
        List<String> matches = new ArrayList<>();
        while(matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                matches.add(matcher.group(i));
            }
        }
        return matches;
    }

    public static String extractFirstCompleteMatch(String regex, String input) {
        var matches = extractAllCompleteMatches(regex, input);
        if (matches.isEmpty()) return "";
        return matches.get(0);
    }

    public static String extractLastCompleteMatch(String regex, String input) {
        var matches = extractAllCompleteMatches(regex, input);
        if (matches.isEmpty()) return "";
        return matches.get(matches.size() - 1);
    }

    public static List<String> extractAllCompleteMatches(String regex, String input) {
        List<String> matches = new ArrayList<>();
        var matcher = getMatcher(regex, input);
        while (matcher.find()) {
            matches.add(matcher.group(0));
        }
        return matches;
    }

    /**
     * Reliably compare strings with strings from Runescape
     */
    public static boolean runescapeStringsMatch(String one, String two) {
        if (one.equals(two)) return true;
        return fixSpaces(one).equals(fixSpaces(two));
    }

    /**
     * Change all space characters to matching chars
     * Sometimes runescape spaces are different bytecodes and will cause seemingly matching strings not to match.
     * Use with runescapeStringsMatch("string", "string);
     */
    public static String fixSpaces(String string) {
        StringBuilder returnString = new StringBuilder();
        string.chars().forEach(chars -> {
            if (Character.isSpaceChar(chars)) returnString.append(" ");
            else returnString.append(Character.toString(chars));
        });
        return returnString.toString();
    }

    public static String getTextFromFirstLineContaining(String... strings) {
        AtomicReference<String> text = new AtomicReference<>("");
        Retry.retry(3, () -> {
            text.set(Query.widgets().inIndexPath(162, 56).textContains(strings).sorted(Comparator.comparingInt(Widget::getIndex)).findFirst().flatMap(Widget::getText).orElse(null));
            return text.get() != null;
        });
        return text.get();
    }

}
