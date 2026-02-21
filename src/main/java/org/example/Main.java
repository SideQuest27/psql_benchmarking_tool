package org.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.example.ToolUtils.*;

public class Main {
    public static int Clients;
    public static int Jobs;
    public static int Time;
    public static int Workload;
    public static int Protocol;
    public static String WorkloadString;
    public static String ProtocolString;
    public static String ScriptPath;
    public static List<String> Commands;
    public static Connection conn;
    public static Thread batchProcessorThread;
    public static Boolean Jit;
    public static Boolean Fsync;
    public static Boolean Sc;
    public static String PlanCM;
    public static BatchProcessor batchProcessor;

    // TODO: 11/01/2026 Fix the bug with the double file printing
    // TODO: 17/02/2026  need to add tags for the pgbench_partition commands like (partition size ect..)
    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        System.out.println(AppArt.art);

        initialiseTables(AppConfig.get("app.psql_db_name"));

        conn =  establishPsqlConnection(AppConfig.get("app.psql_url"), true);

        System.out.println("Would you like to run a batch benchmarking operation? (y/n)");
        String batchBenchmarkingDecision = sc.nextLine();

        if(batchBenchmarkingDecision.trim().equalsIgnoreCase("y")){
            batchProcessor = new BatchProcessor(AppConfig.get("app.batch_workload_json_path"));
            batchProcessorThread =  batchProcessor.runBatchOperation();
        }
        else {

            System.out.println("Would you like to reuse existing pgbench commands? (y/n)");
            String reuseCommandDecision = sc.nextLine();
            if (reuseCommandDecision.trim().equalsIgnoreCase("y")) {
                reusePreviousPgbenchCommands();
            } else {
                setBenchmarkParamValues();
            }

            ProcessBuilder processBuilder = new ProcessBuilder(Commands);
            processBuilder.environment().put("PGPASSWORD", AppConfig.get("app.psql_password"));


            processBuilder.redirectErrorStream(true);

            do {
                Process process = processBuilder.start();
                readAndPrintOutputStream(process);

                System.out.println("would you like to save the results (y/n)");
                String saveDecision = sc.nextLine();
                if(saveDecision.trim().equalsIgnoreCase("y")) {
                    // Interactive runs are never marked as warmup runs nor do i bother with saving the tags.
                    savingResults(Jit,Fsync,Sc,PlanCM,false, null, null);
                }
                System.out.println("Would you like to rerun the same benchmark? (y/n)");
                String rerunDecesion = sc.nextLine();
                if (!rerunDecesion.trim().equalsIgnoreCase("y")) {
                    flushOldCommandParams();
                    break;
                }
            } while (true);
        }

        if (batchProcessorThread != null) {
            try {
                batchProcessorThread.join();
                System.out.println("Batch thread is done.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            resetOptimisationsToDefaults();
        }

        printResultsSummery();
    }
}
