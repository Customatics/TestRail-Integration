package com.customatics.leaptest.testrail.integration;


public final class Case
{
    private String caseId;
    private String caseName;
    private Integer caseStatus;
    private Integer seconds;
    private String description;
    private String keyFramesLogs;
    private String environment;
    private boolean isResultAlreadySet;

    public Case(String caseId, String caseTitle, Integer caseStatus, Integer seconds, String description, String keyFramesLogs, String environment)
    {
        this.caseId = caseId;
        this.caseName = caseTitle;
        this.caseStatus = caseStatus;
        this.seconds = seconds;
        this.description = description;
        this.keyFramesLogs = keyFramesLogs;
        this.environment = environment;
        this.isResultAlreadySet = false;
    }


    public String getCaseName()  { return  caseName; }

    public Integer getCaseStatus() { return  caseStatus;}

    public Integer getSeconds() { return seconds; }

    public String getDescription() {
        return description;
    }

    public String getKeyFramesLogs() {
        return keyFramesLogs;
    }

    public String getEnvironment() {
        return environment;
    }

    public boolean isResultAlreadySet() {
        return isResultAlreadySet;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setResultAlreadySet(boolean resultAlreadySet) {
        isResultAlreadySet = resultAlreadySet;
    }
}
