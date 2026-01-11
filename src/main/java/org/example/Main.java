package org.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

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

    // TODO: 10/01/2026 need to handle these exceptions and also need to add the port and host as an input and not fixed

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5433/pgbench",
                "postgres",
                "12345"
        );

        setBenchmarkParamValues();

        ProcessBuilder processBuilder = new ProcessBuilder(Commands);
        processBuilder.environment().put("PGPASSWORD", "12345");

        initialiseTables();

        processBuilder.redirectErrorStream(true);

        do {
            Process process = processBuilder.start();
            readAndPrintOutputStream(process);
            savingResults();
            System.out.println("Would you like to rerun the same benchmark? (y/n)");
            String rerunDecesion =  sc.nextLine();
            if(!rerunDecesion.equalsIgnoreCase("y")) break;
        } while (true);
    }

}