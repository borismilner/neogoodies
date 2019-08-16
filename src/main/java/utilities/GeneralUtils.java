package utilities;

public class GeneralUtils {

    public static String removeComments(String source) {
        String commentsRegexPattern = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)";
        return source.replaceAll(commentsRegexPattern, "").trim(); // Remove comments
    }

    private GeneralUtils() {
    }
}
