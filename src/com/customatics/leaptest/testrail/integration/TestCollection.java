package com.customatics.leaptest.testrail.integration;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;


public final class TestCollection
{
    private ArrayList<Test> results;

    public TestCollection(ArrayList<Test> tests)
    {
        this.results = tests;
    }

}



