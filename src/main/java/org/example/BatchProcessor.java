package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.Commands;
import static org.example.Main.conn;
import static org.example.ToolUtils.establishPsqlConnection;

public class BatchProcessor
{
    private String JsonFilePath;
    private Operation currentOperation;
    public BatchProcessor(String jsonFilePath) {
        JsonFilePath = jsonFilePath;
    }
    public String getJsonFilePath() {
        return JsonFilePath;
    }
    public void setJsonFilePath(String jsonFilePath) {
        JsonFilePath = jsonFilePath;
    }

    public Operation getCurrentOperation() {
        return currentOperation;
    }

    public void setCurrentOperation(Operation currentOperation) {
        this.currentOperation = currentOperation;
    }

    public Thread runBatchOperation(){
        Runnable backgroundTask = ()->{
            List<Operation> operations = extractValuesFromJSON();

            operations.forEach((op)->{
                currentOperation = op;
                String partitionMethod = op.getPartitionMethod();
                Integer partitionSize = op.getPartitionSize();
                ArrayList<String> additionalCmd = new ArrayList<>();
                if (partitionMethod!= null) additionalCmd.addAll(List.of("--partition-method",partitionMethod));
                if (partitionSize!= null) additionalCmd.addAll(List.of("--partitions",partitionSize.toString()));
                if(!additionalCmd.isEmpty()){
                    try {
                        ToolUtils.initialiseTables(AppConfig.get("app.psql_db_name_partition"),additionalCmd.toArray(new String[0]));
                        conn = ToolUtils.establishPsqlConnection(AppConfig.get("app.psql_url_partition"),true);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                } else conn = ToolUtils.establishPsqlConnection(AppConfig.get("app.psql_url"),true);

                String db;
                if(!additionalCmd.isEmpty()) db = AppConfig.get("app.psql_db_name_partition");
                else db = AppConfig.get("app.psql_db_name");

                ToolUtils.appendParamsToCommandString((op.getScriptPath()!=null),"--builtin="+op.getWorkload(), op.getProtocol(),
                        op.getScriptPath(),String.valueOf(op.getClients()),String.valueOf(op.getTime()),String.valueOf(op.getJobs()),
                        (op.getShortConn()!=null && op.getShortConn() == true),String.valueOf(op.getPort()),op.getHost(),db);  // TODO: 16/02/2026  this is bad code pass op as the param to this method

                ToolUtils.applyBmOptimizations((op.isJit()!= null), op.isJit(),(op.isSc()!=null),op.isSc(),(op.isFsync()!=null),op.isFsync(),op.getPlanCM());

                ProcessBuilder processBuilder = new ProcessBuilder(Commands);
                processBuilder.environment().put("PGPASSWORD",AppConfig.get("app.psql_password"));
                processBuilder.redirectErrorStream(true);
                try {
                    for(int i = 0; i<6;i++) {
                        boolean isWarmupRun = (i<3);
                        ToolUtils.stabilisationBlock();
                        if(isWarmupRun){
                            System.out.println("\u001B[1;33m" + "Warmup run!" + "\u001B[0m"+"\n");
                        }
                        Process process = processBuilder.start();
                        ToolUtils.readAndPrintOutputStream(process);
                        ToolUtils.savingResults((op.isJit()!= null),(op.isSc()!=null),(op.isFsync()!=null),((op.getPlanCM()!=null ? op.getPlanCM() : null)), isWarmupRun, partitionMethod, partitionSize);
                        Thread.sleep(10000); //I did this in order for the windows background indexing/caching to idle

                    }
                    ToolUtils.resetOptimisationsToDefaults();
                    ToolUtils.flushOldCommandParams();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        };

        Thread thread = new Thread(backgroundTask);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private List<Operation> extractValuesFromJSON(){
        Path path = Paths.get(this.JsonFilePath);
        List<Operation> operations = null;
        if (Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".json")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES,true);
            try {
                operations = mapper.readValue(
                    new File(this.getJsonFilePath()),
                    new TypeReference<>() {}
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return operations;
    }
}