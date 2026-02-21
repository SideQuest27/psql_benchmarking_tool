package org.example;

import java.util.regex.Pattern;

public class RegexPatterns {
    public static Pattern tpsPatern = Pattern.compile("tps = ([0-9.]+)");
    public static Pattern latencyPattern = Pattern.compile("latency average = ([0-9.]+) ms");
    public static Pattern connectPattern = Pattern.compile("(?:initial|average) connection time = ([0-9.]+) ms");
    public static Pattern txPattern = Pattern.compile("number of transactions actually processed: (\\d+)");
    public static Pattern builtinPattern = Pattern.compile("--builtin=([A-Za-z0-9_-]+)");
    public static Pattern modePattern = Pattern.compile("-M\\s+([A-Za-z0-9_-]+)");
    public static Pattern clientsPattern = Pattern.compile("-c\\s+([0-9]+)");
    public static Pattern timePattern = Pattern.compile("-T\\s+([0-9]+)");
    public static Pattern threadsPattern = Pattern.compile("-j\\s+([0-9]+)");
    public static Pattern portPattern = Pattern.compile("-p\\s+([0-9]+)");
    public static Pattern filePattern = Pattern.compile("-f\\s+([^\\s]+)");
    public static Pattern hostPattern = Pattern.compile("(?:-h\\s+|--host=)([a-zA-Z0-9\\.\\-]+)");
    public static Pattern planCacheModePattern = Pattern.compile("planCM\\s*=\\s*([A-Za-z_]+)");
    public static Pattern dbPattern = Pattern.compile("\\b(\\w+)(?=\\s*(?:\\(|$))");
    public static Pattern partitionsPattern = Pattern.compile("partitions=([0-9]+)");
    public static Pattern partitionMethodPattern = Pattern.compile("partition_method=([A-Za-z0-9_-]+)");

}
