package com.customatics.leaptest.testrail.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

    private static String loggerName = "logger";

    private static Logger logger = Logger.getLogger(loggerName);

    private static PluginHandler pluginHandler = PluginHandler.getInstance(loggerName);


    public static void main(String[] args) throws IOException, APIException {


        FileHandler logFileHandler = new FileHandler(String.format(Messages.LOG_FILE_NAME, args[0]));
        logger.addHandler(logFileHandler);
        SimpleFormatter formatter = new SimpleFormatter();
        logFileHandler.setFormatter(formatter);

        logger.info("Starting plugin, args length = " + args.length);

        logger.info(String.format("Passed parameters:\n%1$s\n%2$s\n%3$s\n%4$s\n%5$s\n%6$s\n%7$s\n",
                String.format("Run Id: %1$s",args[0]),
                String.format("TestRail URL: %1$s",args[1]),
                String.format("TestRail User: %1$s",args[2]),
                String.format("Leaptest Controller URL: %1$s",args[4]),
                String.format("Schedule Id: %1$s",args[5]),
                String.format("Time Delay in seconds: %1$s",args[6]),
                String.format("Done Status is interpreted as status number: %1$s", args[7])
        ).replace("\n",Messages.NEW_LINE));


        final String testRailRunId = args[0];
        final String testRailAddress = args[1];
        final String testRailLogin = args[2];
        final String testRailPassword = args[3];
        final String leaptestControllerURL = args[4];
        final String scheduleId = args[5];
        final String delay = args[6];
        final String doneStatusAs = args[7];


        APIClient testRailAPIclient = null;
        Schedule schedule = null;
        ArrayList<Test> testRailTests = null;

        try
        {

            testRailAPIclient = new APIClient(testRailAddress);
            testRailAPIclient.setUser(testRailLogin);
            testRailAPIclient.setPassword(testRailPassword);

            int timeDelay = pluginHandler.getTimeDelay(delay);

            int doneStatus = pluginHandler.getDoneStatusAs(doneStatusAs);

            testRailTests = pluginHandler.getTestRailTests(testRailRunId, testRailAPIclient);

            schedule = pluginHandler.detectSchedule(leaptestControllerURL,scheduleId);

            pluginHandler.checkEnvironmentsAndStepsQuantity(schedule,testRailTests);

            if (pluginHandler.runSchedule(leaptestControllerURL,schedule)) // if schedule was successfully run
            {
                boolean isStillRunning = true;

                do
                {
                    Thread.sleep(timeDelay * 1000); //Time delay
                    isStillRunning = pluginHandler.getScheduleState(leaptestControllerURL,schedule, doneStatus);
                }
                while (isStillRunning);
            }

            pluginHandler.setTestRailTestResults(testRailRunId,testRailAPIclient,schedule,testRailTests);

            logger.info(Messages.PLUGIN_SUCCESSFUL_FINISH);

        }
        catch (Exception e)
        {
            if(testRailTests != null)
            {
                for(Test test: testRailTests)
                {
                    test.setStatusId(Test.Status.RETEST);
                    test.setElapsed("1s");
                    test.addComment(e.getMessage());
                }

                testRailAPIclient.sendPost(String.format(Messages.SET_TESTRAIL_TESTS_RESULTS_POST, testRailRunId),new TestCollection(testRailTests) );
            }
            else
                logger.severe(Messages.CANNOT_SET_ANY_RESULTS_AS_TESTS_ARE_NOT_GOT);

            logger.severe(Messages.PLUGIN_ERROR_FINISH);
            logger.severe(e.getMessage());
            logger.severe(Messages.PLEASE_CONTACT_SUPPORT);

        } finally {
            logger.removeHandler(logFileHandler);
            logFileHandler.close();
        }
    }


}
