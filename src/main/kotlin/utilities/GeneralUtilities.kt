package utilities

fun removeComments(string: String): String {
    val regex = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)".toRegex()
    return regex.replace(string, "")
}
