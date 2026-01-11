package org.example;

import java.util.regex.Pattern;

public class RegexPatterns {
    public static Pattern tpsPatern = Pattern.compile("tps = ([0-9.]+)");
    public static Pattern latencyPattern = Pattern.compile("latency average = ([0-9.]+) ms");
    public static Pattern connectPattern = Pattern.compile("initial connection time = ([0-9.]+) ms");
    public static Pattern txPattern = Pattern.compile("number of transactions actually processed: (\\d+)");
}
