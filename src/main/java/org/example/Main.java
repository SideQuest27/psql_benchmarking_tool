package org.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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

    // TODO: 10/01/2026 need to handle these exceptions and also need to add the port and host as an input and not fixed
    // TODO: 11/01/2026 Fix the bug with the double file printing
    // TODO: 13/01/2026 need to  make the port number and the db name dynamic values
    // TODO: 17/01/2026 see if the code is slowing down the final benchmark of the first run 
    // TODO: 17/01/2026 need to also add the fsync and synchronous commit
    // TODO: 11/01/2026 make all the helper functions to private
    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        conn = DriverManager.getConnection(
                AppConfig.get("app.psql_url"),
                AppConfig.get("app.psql_user"),
                AppConfig.get("app.psql_password")
        );

        System.out.println("Would you like to run a batch benchmarking operation? (y/n)");
        String batchBenchmarkingDecision = sc.nextLine();

        if(batchBenchmarkingDecision.trim().equalsIgnoreCase("y")){
            BatchProcessor batchProcessor = new BatchProcessor(AppConfig.get("app_batch_workload_json_path"));
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

            initialiseTables();

            processBuilder.redirectErrorStream(true);

            do {
                Process process = processBuilder.start();
                readAndPrintOutputStream(process);

                System.out.println("would you like to save the results (y/n)");
                String saveDecision = sc.nextLine();
                if(saveDecision.trim().equalsIgnoreCase("y")) {
                    savingResults(Jit,Fsync,Sc);
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
