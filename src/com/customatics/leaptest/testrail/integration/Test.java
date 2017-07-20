package com.customatics.leaptest.testrail.integration;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by User on 13.07.2017.
 */
public final class Test {

    @SerializedName("test_id")
    private Integer testId;
    private transient String testTitle;
    private transient Integer testRailCaseId;
    private transient String leaptestCaseId;
    @SerializedName("status_id")
    private int statusId;
    private String comment;
    private String version;
    private String elapsed;
    private String defects;
    @SerializedName("assignedto_id")
    private Integer assignedTo;
    private transient String testURL;
    private transient boolean isStepCase;
    @SerializedName("custom_step_results")
    private ArrayList<Step> steps;


    public Test(Integer testId, Integer testRailCaseId, String testTitle, String testRailAddress, Integer assignedTo){

        this.testId = testId;
        this.testTitle = testTitle;
        this.testRailCaseId = testRailCaseId;
        this.leaptestCaseId = null;
        this.statusId = Status.RETEST;  //default value
        this.steps = new ArrayList<>();
        this.comment = null;
        this.version = null;
        this.elapsed = null;
        this.defects = null;
        this.assignedTo = assignedTo;
        this.isStepCase = false;
        this.testURL = String.format(Messages.GET_TESTRAIL_TEST_GET,testRailAddress,testId);
    }

    public Integer getTestId() {
        return testId;
    }

    public String getTestTitle() {
        return testTitle;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public String getComment() {
        return comment;
    }

    public void addComment(String comment) {
        if(this.comment == null)
            this.comment = comment;
        else
            this.comment += String.format("%1$s%2$s",Messages.NEW_LINE,comment);
    }

    public void setComment(int statusId) {

        switch (statusId)
        {
            case Status.PASSED:
                addComment(String.format(Messages.COMMENT_FORMAT,"Passed", this.testURL));
            break;
            case Status.BLOCKED:
                addComment(String.format(Messages.COMMENT_FORMAT,"Blocked", this.testURL));
            break;
            case Status.RETEST:
                addComment(String.format(Messages.COMMENT_FORMAT,"Retest", this.testURL));
            break;
            case Status.FAILED:
                addComment(String.format(Messages.COMMENT_FORMAT,"Failed", this.testURL));
            break;
            default:
                addComment(String.format(Messages.COMMENT_FORMAT,"Blocked", this.testURL));
            break;
        }

    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getElapsed() {
        return elapsed;
    }

    public void setElapsed(String elapsed) {
        this.elapsed = elapsed;
    }

    public String getDefects() {
        return defects;
    }

    public void setDefects(String defects) {
        this.defects = defects;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public String getTestURL() {
        return testURL;
    }

    public ArrayList<Step> getSteps() {
        return steps;
    }

    public boolean isStepCase() {
        return isStepCase;
    }

    public void setStepCase(boolean stepCase) {
        this.isStepCase = stepCase;
    }

    public String getLeaptestCaseId() {
        return leaptestCaseId;
    }

    public void setLeaptestCaseId(String leaptestCaseId) {
        this.leaptestCaseId = leaptestCaseId;
    }

    public Integer getTestRailCaseId() {
        return testRailCaseId;
    }

    public static class Status
    {
        public static final int PASSED = 1;
        public static final int BLOCKED = 2;
        public static final int UNTESTED = 3; //not allowed by TestRail API
        public static final int RETEST = 4;
        public static final int FAILED = 5;
    }
}
