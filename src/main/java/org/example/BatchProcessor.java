package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.tools.Tool;
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
                        op.getScriptPath(),op.getClients(),op.getTime(),op.getJobs());

                ProcessBuilder processBuilder = new ProcessBuilder(Commands);
                processBuilder.environment().put("PGPASSWORD", "12345");
                processBuilder.redirectErrorStream();
                try {
                    for(int i = 0; i<2;i++) {
                        Process process = processBuilder.start();
                        ToolUtils.readAndPrintOutputStream(process);
                        // TODO: 16/01/2026 add the logic for checking for warmup runs and then saving the hot runs
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

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
            try {
                operations = mapper.readValue(
                    new File(this.getJsonFilePath()),
                    new TypeReference<List<Operation>>() {}
                );

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return operations;
    }

}