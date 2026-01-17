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

    // TODO: 10/01/2026 need to handle these exceptions and also need to add the port and host as an input and not fixed
    // TODO: 11/01/2026 Fix the bug with the double file printing
    // TODO: 13/01/2026 need to  make the port number and the db name dynamic values
    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/pgbenchdb",
                "postgres",
                "12345"
        );

        System.out.println("Would you like to run a batch benchmarking operation? (y/N)");
        String batchBenchmarkingDecision = sc.nextLine();

        if(batchBenchmarkingDecision.equalsIgnoreCase("y")){
            BatchProcessor batchProcessor = new BatchProcessor("C:\\Users\\nirav\\OneDrive\\Desktop\\batchOperation.json");
            batchProcessorThread =  batchProcessor.runBatchOperation();
        }
        else {

            System.out.println("Would you like to reuse existing pgbench commands? (y/n)");
            String reuseCommandDecesion = sc.nextLine();
            if (reuseCommandDecesion.equalsIgnoreCase("y")) {
                reusePreviousPgbenchCommands();
            } else {
                setBenchmarkParamValues();
            }

            ProcessBuilder processBuilder = new ProcessBuilder(Commands);
            processBuilder.environment().put("PGPASSWORD", "12345");

            initialiseTables();

            processBuilder.redirectErrorStream(true);

            do {
                Process process = processBuilder.start();
                readAndPrintOutputStream(process);
                savingResults();
                System.out.println("Would you like to rerun the same benchmark? (y/n)");
                String rerunDecesion = sc.nextLine();
                if (!rerunDecesion.equalsIgnoreCase("y")) {
                    flushOldCommandParams();
                    break;
                }
            } while (true);
        }

        try {
            batchProcessorThread.join(); // This line PAUSES the main thread until 't' is finished
            System.out.println("Thread is done. Moving on!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        printResultsSummery();
    }
}
