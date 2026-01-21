package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.Main.*;
import static org.example.RegexPatterns.*;

public class ToolUtils {

    static Scanner sc = new Scanner(System.in);
    public static String pgBenchOutput;
    public static void setBenchmarkParamValues(){

        // TODO: 20/01/2026  need to add the port and optimisations as a user entry value

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
        appendParamsToCommandString((Workload == 4),WorkloadString,ProtocolString,ScriptPath,String.valueOf(Clients),String.valueOf(Time),String.valueOf(Jobs),null,null);
    }

    public static boolean checkForSqlScript(String scriptPath){
        Path path = Paths.get(scriptPath);
        return  (Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".sql"));
    }

    public static void appendParamsToCommandString(boolean CustomWorkload,String WorkloadString, String ProtocolString,String ScriptPath,String Clients,String Time,String Jobs,String Port,String Host){

        String portString = (Port.equals("null")) ? AppConfig.get("app.psql_port") : Port;

        Commands = new ArrayList<>();
        Commands.add(AppConfig.get("app.pgbench_url"));
        if(!CustomWorkload) Commands.add(WorkloadString);
        Commands.addAll(List.of("-M",ProtocolString));
        if(CustomWorkload) Commands.addAll(List.of("-f",ScriptPath));
        Commands.addAll(List.of(
                "-c", Clients,
                "-T", Time,
                "-j", Jobs,
                "-p", portString,
                "-U", AppConfig.get("app.psql_user")));
        if(Host != null) Commands.addAll(List.of("-h",Host));
        Commands.add(AppConfig.get("app.psql_db_name"));
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

        Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:"+AppConfig.get("app.psql_port")+"/postgres",
                AppConfig.get("app.psql_user"),
                AppConfig.get("app.psql_password"));

        boolean exists;
        String checkDbQuery = "SELECT 1 FROM pg_database WHERE datname = ?";
        PreparedStatement ps = conn.prepareStatement(checkDbQuery);
        ps.setString(1,AppConfig.get("app.psql_db_name"));
        ResultSet rs = ps.executeQuery();
        exists = rs.next();

        if(!exists){
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE " + AppConfig.get("app.psql_db_name"));
            System.out.println("Db created...");

            try {
                Commands = new ArrayList<>();
                Commands.addAll(List.of(AppConfig.get("app.pgbench_url"),
                        "-i",
                        "-s",AppConfig.get("app.pgbench_scale_factor"),
                        "-p", AppConfig.get("app.psql_port"),
                        "-U", AppConfig.get("app.psql_user"),
                        AppConfig.get("app.psql_db_name")));

                ProcessBuilder processBuilder = new ProcessBuilder(Commands);
                processBuilder.environment().put("PGPASSWORD", AppConfig.get("app.psql_password"));
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                readAndPrintOutputStream(process);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                flushOldCommandParams();
                conn.close();
            }
        }

        Connection conn2 = DriverManager.getConnection(
                AppConfig.get("app.psql_url"),
                AppConfig.get("app.psql_user"),
                AppConfig.get("app.psql_password")
        );

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
        conn2.createStatement().execute(sql);
        conn2.close();
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

    public static void savingResults(boolean jit, boolean fsync, boolean sc) throws SQLException {
        StringBuilder pgbench_cmd = new StringBuilder(String.join(" ", Commands).replace("C:\\Program Files\\PostgreSQL\\16\\bin\\pgbench.exe", "pgbench"));
        if(jit) pgbench_cmd.append(" (jit)");
        if(fsync) pgbench_cmd.append(" (fsync)");
        if(sc) pgbench_cmd.append(" (sc)");
        double tps = extractDouble(tpsPatern, pgBenchOutput);
        double latency = extractDouble(latencyPattern, pgBenchOutput);
        double connectTime = extractDouble(connectPattern, pgBenchOutput);
        int transactions = extractInt(txPattern, pgBenchOutput);
        insertingResultsIntoSQLTable(pgbench_cmd.toString(),transactions,latency,connectTime,tps);
        System.out.println("\u001B[1;32m"+"Benchmark saved..."+ "\u001B[0m"+"\n");
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
            rs.close();
            ps.close();
            ps2.close();
        }
    }

    public static void  reusePreviousPgbenchCommands() throws SQLException {

        PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT pgbench_cmd FROM benchmark_run",
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        ResultSet rs =  ps.executeQuery();

        StringBuilder results = new StringBuilder("");
        int id = 1;
        while (rs.next()) {
            results.append(String.valueOf(id++) +": "+ rs.getString(1) + "\n");   // or rs.getString(1)
        }

        int selectedId;

        while (true) {
            System.out.println("Provide the id of the command you would like to reuse :");
            System.out.println(results);

            selectedId = sc.nextInt();
            sc.nextLine();

            if (!(selectedId < id && selectedId > 0)) {
                System.out.println("Invalid input!");
            } else break;
        }

        String selectedCommand;
        rs.beforeFirst();

        for(int i=0;i< selectedId;i++){
            rs.next();
        }
        selectedCommand = rs.getString(1);

        commandValueExtractor(selectedCommand);

        rs.close();
        ps.close();
    }

    private static void commandValueExtractor(String cmd){
        Matcher m;

        m = builtinPattern.matcher(cmd);
        String builtin = m.find() ? m.group(1) : null;

        m = modePattern.matcher(cmd);
        String mode = m.find() ? m.group(1) : null;

        m = clientsPattern.matcher(cmd);
        String clients = m.find() ? m.group(1) : null;

        m = timePattern.matcher(cmd);
        String duration = m.find() ? m.group(1) : null;

        m = threadsPattern.matcher(cmd);
        String threads = m.find() ? m.group(1) : null;

        m = portPattern.matcher(cmd);
        String port = m.find() ? m.group(1) : null;

        m = filePattern.matcher(cmd);
        String file = m.find() ? m.group(1) : null;

        m = hostPattern.matcher(cmd);
        String host = m.find() ? m.group(1) : null;

        Jit = cmd.contains("(jit)");
        Fsync = cmd.contains("(fsync)");
        Sc = cmd.contains("(sc)");

        applyBmOptimizations(Jit,false,Sc,false,Fsync,false);
        appendParamsToCommandString((file!=null),"--builtin="+builtin,mode,file,clients,duration,threads,port,host);
    }

    public static void flushOldCommandParams(){
        Commands = null;
        Clients = 0;
        Jobs = 0;
        Time = 0;
        Workload = 0;
        Protocol = 0;
        WorkloadString = null;
        ProtocolString = null;
        ScriptPath = null;
        Jit = null;
        Sc = null;
        Fsync = null;
    }
    public static void printResultsSummery() throws SQLException {

        final String RESET  = "\u001B[0m";
        final String CYAN   = "\u001B[36m";
        final String GREEN  = "\u001B[32m";
        final String YELLOW = "\u001B[33m";
        final String BLUE   = "\u001B[34m";
        final String PURPLE = "\u001B[35m";
        final String RED    = "\u001B[31m";

        String sql = """
                SELECT
                    pgbench_cmd,
                    COUNT(*) AS runs,
                    ROUND(AVG(tps)::numeric, 2) AS avg_tps,
                    ROUND(AVG(latency)::numeric, 2) AS avg_latency,
                    ROUND(AVG(transactions_processed)::numeric, 2) AS avg_transactions_processed,
                    ROUND(AVG(connect_time)::numeric, 2) AS avg_connect_time
                FROM benchmark_run
                GROUP BY pgbench_cmd;
                """;
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while(rs.next()){
            System.out.println(
                    CYAN   + "Query: " + rs.getString(1) + RESET + " | " +
                    GREEN  + "Runs: " + rs.getInt(2) + RESET + " | " +
                    YELLOW + "Avg TPS: " + rs.getDouble(3) + RESET + " | " +
                    BLUE   + "Avg Latency: " + rs.getDouble(4) + RESET + " | " +
                    PURPLE + "Avg Tx: " + rs.getDouble(5) + RESET + " | " +
                    RED    + "Avg Connect: " + rs.getDouble(6) + RESET
            );
        }
    }

    public static void applyBmOptimizations(boolean changeJIT,Boolean jitSwitch, boolean changeSC, Boolean scSwitch ,boolean changeFSYNC ,Boolean fsyncSwitch) {
        try (Statement stmt = conn.createStatement()) {
            if(changeJIT) {
                stmt.execute("ALTER SYSTEM SET jit = '"+ convertToOnOff(jitSwitch)+"'");
                System.out.println("\u001B[1;32m" + "jit = "+ convertToOnOff(jitSwitch)+" " + "\u001B[0m");
            }
            if (changeSC) {
                stmt.execute("ALTER SYSTEM SET synchronous_commit = '"+ convertToOnOff(scSwitch)+"' ");
                System.out.println("\u001B[1;32m" + "synchronous_commit = "+ convertToOnOff(scSwitch)+" " + "\u001B[0m");
            }
            if (changeFSYNC) {
                stmt.execute("ALTER SYSTEM SET fsync = '"+convertToOnOff(fsyncSwitch)+"'");
                System.out.println("\u001B[1;32m" + "fsync = "+ convertToOnOff(fsyncSwitch)+" " + "\u001B[0m");
            }
            stmt.execute("SELECT pg_reload_conf()");


        } catch (SQLException e) {
            System.err.println("Failed to update settings: " + e.getMessage());
        }
    }

    private static String convertToOnOff(Boolean val){
        if (val) return  "on";
        else return "off";
    }

    public static void resetOptimisationsToDefaults() {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER SYSTEM RESET jit");
            stmt.execute("ALTER SYSTEM RESET synchronous_commit");
            stmt.execute("ALTER SYSTEM RESET fsync");
            stmt.execute("SELECT pg_reload_conf()");

            System.out.println("\u001B[1;34m" + "Settings reverted to original postgresql.conf defaults." + "\u001B[0m");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void stabilisationBlock(){
        try (Statement stmt = conn.createStatement()) {
            System.out.println("\n"+"\u001B[35m"+"Stabilising the benchmark environment..."+"\u001B[0m"+"\n");
            stmt.execute("CHECKPOINT"); // This flushes dirty buffers to disk so the background writer is quiet
            stmt.execute("VACUUM ANALYZE"); //This cleans up dead tuples and updates statistics for the query planner
            stmt.execute("DISCARD ALL"); // This resets session state, drops temporary tables, and clears the plan cache

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
