package scripts.api.utility;

import java.util.ArrayList;
import java.util.List;
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


}
