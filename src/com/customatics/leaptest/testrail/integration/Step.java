package com.customatics.leaptest.testrail.integration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by User on 13.07.2017.
 */
public final class Step {


    private String content;
    private String expected;
    private String actual;
    @SerializedName("status_id")
    private int statusId;
    private transient Integer seconds;
    private transient boolean isStepFilled;

    public Step (String expected, String content)
    {

        this.content = content;
        this.expected = expected;
        this.statusId = Test.Status.RETEST;
        this.actual = Messages.THIS_STEP_WAS_NOT_RUN;
        this.seconds = 0;
        this.isStepFilled = false;
    }



    public String getContent() {
        return content;
    }

    public String getExpected() {
        return expected;
    }

    public String getActual() {
        return actual;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public Integer getSeconds() {
        return seconds;
    }

    public void addSeconds(Integer seconds) {
        this.seconds += seconds;
    }

    public boolean isStepFilled() {
        return isStepFilled;
    }

    public void setStepFilled(boolean stepFilled) {
        isStepFilled = stepFilled;
    }
}
