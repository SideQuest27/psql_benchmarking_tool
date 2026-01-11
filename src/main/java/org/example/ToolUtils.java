package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.Main.*;
import static org.example.RegexPatterns.*;

public class ToolUtils {

    static Scanner sc = new Scanner(System.in);
    public static String pgBenchOutput;
    public static void setBenchmarkParamValues(){


        System.out.println("Clients:");
        Clients = sc.nextInt();

        System.out.println("Jobs:");
        Jobs = sc.nextInt();

        System.out.println("Time:");
        Time = sc.nextInt();

        while (true) {
            System.out.println("Workload id:\n" +
                    "1) select-only  \n" +
                    "2) simple-update\n" +
                    "3) tpcb-like    \n" +
                    "4) custom workload");
            Workload = sc.nextInt();
            if(Workload < 5 && Workload > 0){
                if(Workload == 4){
                    while (true) {
                        System.out.println("Enter the file path to your script:");
                        ScriptPath = sc.nextLine();
                        if (checkForSqlScript(ScriptPath)) break;
                        WorkloadString = "";
                    }
                }
                switch (Workload){
                    case 1: WorkloadString = "--builtin=select-only";
                        break;
                    case 2: WorkloadString = "--builtin=simple-update";
                        break;
                    case 3: WorkloadString = "--builtin=tpcb-like";
                }
                break;
            }else {
                System.out.println("Invalid input! enter again.");
            }
        }

        while(true){
            System.out.println("Protocol id: \n" +
                    "1) simple    \n" +
                    "2) extended  \n" +
                    "3) prepared  \n");
            Protocol = sc.nextInt();
            sc.nextLine();
            if(Protocol < 4 && Protocol > 0){
                switch (Protocol){
                    case 1 : ProtocolString = "simple";
                        break;
                    case 2: ProtocolString = "extended";
                        break;
                    case 3: ProtocolString = "prepared";
                        break;
                    default: ProtocolString = "";
                }
                break;
            }else {
                System.out.println("Invalid input! enter again.");
            }
        }
        appendParamsToCommandString();
    }

    public static boolean checkForSqlScript(String scriptPath){
        Path path = Paths.get(scriptPath);
        return  (Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".sql"));
    }

    public static void appendParamsToCommandString(){
        Commands = new ArrayList<>();
        Commands.add("C:\\Program Files\\PostgreSQL\\16\\bin\\pgbench.exe");
        if(Workload < 4) Commands.add(WorkloadString);
        Commands.addAll(List.of("-M",ProtocolString));
        if(Workload == 4) Commands.addAll(List.of("-f",ScriptPath));
        Commands.addAll(List.of(
                "-c", String.valueOf(Clients),
                "-T", String.valueOf(Time),
                "-j", String.valueOf(Jobs),
                "-p", "5433",
                "-U", "postgres",
                "pgbench"));
    }

    public static void readAndPrintOutputStream(Process process) throws IOException, InterruptedException {
        System.out.print("Running... ");
        Commands.stream().forEach(x -> System.out.print(" "+x));
        System.out.println();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        StringBuilder output = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        process.waitFor();
        pgBenchOutput = output.toString();
        System.out.println(pgBenchOutput);
    }

    public static void initialiseTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS benchmark_run (
                id SERIAL PRIMARY KEY,
                pgBench_cmd TEXT,
                transactions_processed INTEGER,
                latency DOUBLE PRECISION,
                connect_time DOUBLE PRECISION,
                tps DOUBLE PRECISION,
                run_time TIMESTAMP DEFAULT now()
            )
        """;
        conn.createStatement().execute(sql);
    }

    private static double extractDouble(Pattern p, String text) {
        Matcher m = p.matcher(text);
        if (!m.find())
            throw new RuntimeException("Value not found: " + p);
        return Double.parseDouble(m.group(1));
    }

    private static int extractInt(Pattern p, String text) {
        Matcher m = p.matcher(text);
        if (!m.find())
            throw new RuntimeException("Value not found: " + p);
        return Integer.parseInt(m.group(1));
    }

    public static void insertingResultsIntoSQLTable(String pgbench_cmd,int transactions,double latency,double connectionTime,double tps) throws SQLException {
        checkIfThreeResultsExistAndReplace(pgbench_cmd);
        String insert = """
            INSERT INTO benchmark_run (pgbench_cmd,transactions_processed, latency, connect_time, tps)
            VALUES (?, ?, ?, ?, ?)
            """;

        PreparedStatement ps = conn.prepareStatement(insert);
        ps.setString(1, pgbench_cmd);
        ps.setInt(2, transactions);
        ps.setDouble(3, latency);
        ps.setDouble(4, connectionTime);
        ps.setDouble(5, tps);
        ps.executeUpdate();
    }

    public static void savingResults() throws SQLException {
        while(true) {
            System.out.println("would you like to save the results (y/n)");
            String saveDecision = sc.nextLine();
            if(saveDecision.trim().equalsIgnoreCase("y")){
                String pgbench_cmd = String.join(" ", Commands).replace("C:\\Program Files\\PostgreSQL\\16\\bin\\pgbench.exe","pgbench");
                double tps = extractDouble(tpsPatern, pgBenchOutput);
                double latency = extractDouble(latencyPattern, pgBenchOutput);
                double connectTime = extractDouble(connectPattern, pgBenchOutput);
                int transactions = extractInt(txPattern, pgBenchOutput);
                insertingResultsIntoSQLTable(pgbench_cmd,transactions,latency,connectTime,tps);
                System.out.println("Benchmark saved...");
                break;
            }
            else break;
        }
    }

    public static void checkIfThreeResultsExistAndReplace(String cmd) throws SQLException {
        String sql = """ 
                SELECT COUNT(*) FROM benchmark_run WHERE pgbench_cmd = ?
                """;
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1,cmd);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int resultSize =  rs.getInt(1);

        if(resultSize >= 3){
            PreparedStatement ps2 = conn.prepareStatement(
                    """
                    DELETE FROM benchmark_run
                    WHERE id = (
                        SELECT id
                        FROM benchmark_run
                        WHERE pgbench_cmd = ?
                        ORDER BY run_time ASC
                        LIMIT 1
                    )
                    """
            );
            ps2.setString(1, cmd);
            ps2.executeUpdate();
            System.out.println("Oldest Benchmark Record replaced!");
        }
    }

    public static void  reusePreviousPgbenchCommands(){
        System.out.println("Would you like to reuse existing pgbench commands");
        String reuseCommandDecesion =  sc.nextLine();
    }
}

