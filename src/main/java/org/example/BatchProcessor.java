package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.example.Main.Commands;

public class BatchProcessor
{
    private String JsonFilePath;
    public BatchProcessor(String jsonFilePath) {
        JsonFilePath = jsonFilePath;
    }
    public String getJsonFilePath() {
        return JsonFilePath;
    }
    public void setJsonFilePath(String jsonFilePath) {
        JsonFilePath = jsonFilePath;
    }
    
    public Thread runBatchOperation(){
        Runnable backgroundTask = ()->{
            List<Operation> operations = extractValuesFromJSON();

            operations.forEach((op)->{
                ToolUtils.appendParamsToCommandString((op.getScriptPath()!=null),"--builtin="+op.getWorkload(), op.getProtocol(),
                        op.getScriptPath(),String.valueOf(op.getClients()),String.valueOf(op.getTime()),String.valueOf(op.getJobs()),String.valueOf(op.getPort()),op.getHost());

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
                        if (!isWarmupRun){
                            ToolUtils.savingResults((op.isJit()!= null),(op.isSc()!=null),(op.isFsync()!=null),((op.getPlanCM()!=null ? op.getPlanCM() : null)));
                        }
                        Thread.sleep(10000); //I did this in order for the windows background indexing/caching to idle
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ToolUtils.resetOptimisationsToDefaults();
                ToolUtils.flushOldCommandParams();
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