package com.customatics.leaptest.testrail.integration;

import java.util.ArrayList;

public final class Schedule
{
    private String scheduleTitle;

    private String scheduleId;

    private ArrayList<Case> cases;

    private Integer environmentsQuantity;


    public Schedule(String scheduleId, String title, int environmentsQuantity)
    {
        cases = new ArrayList<Case>();
        this.scheduleId = scheduleId;
        this.scheduleTitle = title;
        this.environmentsQuantity = environmentsQuantity;

    }


    public String getScheduleTitle() { return scheduleTitle;}

    public String getScheduleId()  { return scheduleId; }

    public ArrayList<Case> getCases() { return cases;}

    public Integer getEnvironmentsQuantity() {
        return environmentsQuantity;
    }
}
