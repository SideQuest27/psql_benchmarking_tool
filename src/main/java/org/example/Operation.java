package org.example;

import java.sql.Connection;
import java.util.List;

public class Operation {
    private int Clients;
    private int Jobs;
    private int Time;
    private String Workload;
    private String Protocol;
    private String ScriptPath;
    private Boolean Jit;
    private Boolean Fsync;
    private Boolean Sc;

    public Operation() {
    }

    public Operation(int clients, int jobs, int time, String workload, String protocol, String scriptPath,Boolean jit,Boolean fsync, Boolean sc) {
        Clients = clients;
        Jobs = jobs;
        Time = time;
        Workload = workload;
        Protocol = protocol;
        ScriptPath = scriptPath;
        Jit = jit;
        Fsync = fsync;
        Sc = sc;
    }

    public Boolean isJit() {
        return Jit;
    }

    public void setJit(boolean jit) {
        Jit = jit;
    }

    public Boolean isFsync() {
        return Fsync;
    }

    public void setFsync(boolean fsync) {
        Fsync = fsync;
    }

    public Boolean isSc() {
        return Sc;
    }

    public void setSc(boolean sc) {
        Sc = sc;
    }

    public int getClients() {
        return Clients;
    }

    public void setClients(int clients) {
        Clients = clients;
    }

    public int getJobs() {
        return Jobs;
    }

    public void setJobs(int jobs) {
        Jobs = jobs;
    }

    public int getTime() {
        return Time;
    }

    public void setTime(int time) {
        Time = time;
    }

    public String getWorkload() {
        return Workload;
    }

    public void setWorkload(String workload) {
        Workload = workload;
    }

    public String getProtocol() {
        return Protocol;
    }

    public void setProtocol(String protocol) {
        Protocol = protocol;
    }

    public String getScriptPath() {
        return ScriptPath;
    }

    public void setScriptPath(String scriptPath) {
        ScriptPath = scriptPath;
    }
}
