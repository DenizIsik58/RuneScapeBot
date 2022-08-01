package scripts.api.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public static Optional<String> extractFirstMatch(String regex, String input) {
        var matcher = getMatcher(regex, input);
        if (matcher.find()) return Optional.of(matcher.group(0));
        return Optional.empty();
    }

    public static Optional<String> extractLastMatch(String regex, String input) {
        var matches = extractAllMatches(regex, input);
        if (matches.isEmpty()) return Optional.empty();
        return Optional.ofNullable(matches.get(matches.size() - 1));
    }

    public static List<String> extractAllMatches(String regex, String input) {
        List<String> matches = new ArrayList<>();
        var matcher = getMatcher(regex, input);
        while (matcher.find()) {
            matches.add(matcher.group(0));
        }
        return matches;
    }

}
