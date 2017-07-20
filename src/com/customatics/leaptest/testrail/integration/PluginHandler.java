package com.customatics.leaptest.testrail.integration;

import com.google.gson.*;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.io.*;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;


public final class PluginHandler {

    private static PluginHandler pluginHandler = null;

    private static Logger logger = null;

    private PluginHandler(String loggerName){
        logger = Logger.getLogger(loggerName);
    }

    public static PluginHandler getInstance(String loggerName)
    {
        if( pluginHandler == null ) pluginHandler = new PluginHandler(loggerName);

        return pluginHandler;
    }

    public int getTimeDelay(String rawTimeDelay)
    {
        int timeDelay = 5;  //5 seconds - default
        try {
            if (!rawTimeDelay.isEmpty() || !"".equals(rawTimeDelay))
                timeDelay =  Integer.parseInt(rawTimeDelay);
            else
                throw new NumberFormatException(Messages.EMPTY_TIME_DELAY);
        } catch (NumberFormatException e) {
            timeDelay = 5;
            logger.warning(String.format("$1%s%2$s%3$s", e.getMessage(),Messages.NEW_LINE, String.format(Messages.SET_TIME_DELAY_TO_DEFAULT_VALUE,timeDelay)));
        } finally {
            return timeDelay;
        }
    }

    public int getDoneStatusAs(String rawDoneStatusAs)
    {
        int doneStatus = 4; // default value RETEST

        try {

            if (!rawDoneStatusAs.isEmpty() || !"".equals(rawDoneStatusAs))
            {
                doneStatus = Integer.parseInt(rawDoneStatusAs);

                if (doneStatus < 1 || doneStatus > 5 || doneStatus == 3)
                    throw  new NumberFormatException(String.format(Messages.UNSUPPORTED_STATUS_VALUE_WARNING, doneStatus));

            }
            else
                throw new NumberFormatException(Messages.EMPTY_DONE_STATUS);
        } catch (NumberFormatException e)
        {
            logger.warning(String.format("%1$s%2$s%3$s", e.getMessage(), Messages.NEW_LINE ,Messages.SET_DONE_STATUS_TO_DEFAULT_VALUE ));
            doneStatus = 4; //SET_DEFAULT VALUE
        }
        finally {
            return doneStatus;
        }
    }

    public void checkEnvironmentsAndStepsQuantity(Schedule schedule, ArrayList<Test> tests)
    {
        for(Test test: tests)
        {
            if(test.isStepCase())
            {
                int steps = test.getSteps().size();
                int envs = schedule.getEnvironmentsQuantity();
                if(steps != envs) {
                    test.addComment(String.format(Messages.STEPS_AND_ENVIRONMENTS_QUANTITY_ARE_NOT_EQUAL, steps, envs));
                    logger.warning(String.format(Messages.STEPS_AND_ENVIRONMENTS_QUANTITY_ARE_NOT_EQUAL, steps, envs));
                }
            }
        }
    }


    public ArrayList<Test> getTestRailTests(
            String testRailRunId,
            APIClient testRailAPIClient
    ) throws Exception {
        ArrayList<Test> tests = new ArrayList<>();

        try
        {
            JsonArray jsonTests = testRailAPIClient.sendGet(String.format(Messages.GET_TESTRAIL_TESTS_GET,testRailRunId)).getAsJsonArray();

            int currentTest = 0;

            for(JsonElement jsonElement : jsonTests)
            {
                JsonObject jsonTest = jsonElement.getAsJsonObject();

                tests.add(new Test(
                        jsonTest.get("id").getAsInt(),
                        jsonTest.get("case_id").getAsInt(),
                        jsonTest.get("title").getAsString(),
                        testRailAPIClient.getTestRailAddress(),
                        Utils.defaultIntegerIfNull(jsonTest.get("assignedto_id"),null)
                ));

                JsonElement jsonSteps = jsonTest.get("custom_steps_separated");

                if(jsonSteps == null || jsonSteps == JsonNull.INSTANCE)
                {
                    tests.get(currentTest).setStepCase(false); //This case does not support steps
                    logger.warning(String.format(Messages.UNSUPPORTED_CASE_TYPE, tests.get(currentTest).getTestTitle()));
                    tests.get(currentTest).setStatusId(Test.Status.RETEST);
                    tests.get(currentTest).setComment(Test.Status.RETEST);
                    tests.get(currentTest).addComment(String.format(Messages.UNSUPPORTED_CASE_TYPE, tests.get(currentTest).getTestTitle()));
                    tests.get(currentTest).setElapsed("1s");

                }
                else
                {
                    tests.get(currentTest).setStepCase(true);
                    JsonArray jsonTestSteps = jsonSteps.getAsJsonArray();

                    int currentStep = 1;

                    for(JsonElement jsonElementStep: jsonTestSteps)
                    {
                        JsonObject jsonStep = jsonElementStep.getAsJsonObject();

                        tests.get(currentTest).getSteps().add(
                                new Step(Utils.defaultStringIfNull(jsonStep.get("expected"),""),Utils.defaultStringIfNull(jsonStep.get("content"),String.format("Step %1$d", currentStep)))
                        );

                        currentStep++;
                    }
                }

                currentTest++;
            }

        } catch (UnknownHostException e)
        {
            String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
            throw new Exception(connectionErrorMessage);
        } catch (APIException e) {
            throw new Exception(e);
        } catch (MalformedURLException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        }
        catch (Exception e) {
            logger.severe(e.getMessage());
            throw new Exception(e.getMessage());
        }

        return tests;
    }


    public Schedule detectSchedule(
            String leaptestAddress,
            String scheduleId
    ) throws Exception {

        Schedule schedule = null;

        String scheduleListUri = String.format(Messages.GET_SPECIFIC_SCHEDULE_URI, leaptestAddress, scheduleId);

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(scheduleListUri).execute().get();
            client = null;


            switch (response.getStatusCode()) {
                case 200:

                    JsonParser parser = new JsonParser();
                    JsonObject jsonSchedule = parser.parse(response.getResponseBody()).getAsJsonObject();
                    String Id = Utils.defaultStringIfNull(jsonSchedule.get("Id"), "null Id");
                    String Title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");
                    int environmentsQuantity = 0;
                    JsonElement jsonEnvironments = jsonSchedule.get("EnvironmentIds");
                    if(jsonEnvironments != null || jsonEnvironments != JsonNull.INSTANCE)
                    {
                       environmentsQuantity = jsonEnvironments.getAsJsonArray().size();
                    }

                    if(Id.contentEquals("null Id")) throw  new Exception(Messages.NO_SUCH_SCHEDULE);

                    schedule = new Schedule(Id, Title, environmentsQuantity);
                    logger.info(String.format(Messages.SCHEDULE_DETECTED, Title, Id));

                    break;

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("%1$s%2$s", Messages.NEW_LINE, Messages.NO_SUCH_SCHEDULE);
                    throw new Exception(errorMessage404);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("%1$s%2$s", Messages.NEW_LINE, Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }


        } catch (ConnectException e){
            String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
            throw new Exception(connectionErrorMessage);
        } catch (InterruptedException e) {
            throw new Exception(e);
        } catch (ExecutionException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        } catch (Exception e) {
            logger.severe(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
            logger.severe(e.getMessage());
            logger.severe(Messages.PLEASE_CONTACT_SUPPORT);
            throw new Exception(String.format("%1$s%2$s%3$s", Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT, Messages.NEW_LINE, e.getMessage()));
        }

        return schedule;
    }


    public boolean runSchedule(
            String leaptestAddress,
            Schedule schedule
    ) throws Exception {
        boolean isSuccessfullyRun = false;

        String uri = String.format(Messages.RUN_SCHEDULE_URI, leaptestAddress, schedule.getScheduleId());

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.preparePut(uri).setBody("").execute().get();
            client = null;

            switch (response.getStatusCode())
            {
                case 204:
                    isSuccessfullyRun = true;
                    String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, schedule.getScheduleTitle(), schedule.getScheduleId());
                    logger.info(successMessage);
                break;

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("%1$s%2$s", Messages.NEW_LINE, String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, schedule.getScheduleTitle(), schedule.getScheduleId()));
                    throw new Exception(errorMessage404);

                case 444:
                    String errorMessage444 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage444 += String.format("%1$s%2$s", Messages.NEW_LINE, String.format(Messages.SCHEDULE_HAS_NO_CASES,schedule.getScheduleTitle(), schedule.getScheduleId()));
                    throw new Exception(errorMessage444);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("%1$s%2$s", Messages.NEW_LINE, String.format(Messages.SCHEDULE_IS_RUNNING_NOW, schedule.getScheduleTitle(), schedule.getScheduleId()));
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (ConnectException e){
            String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
            throw new Exception(connectionErrorMessage);
        } catch (InterruptedException e) {
            throw new Exception(e);
        } catch (ExecutionException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        }
        catch (Exception e){
            String errorMessage = String.format(Messages.SCHEDULE_RUN_FAILURE,  schedule.getScheduleTitle(), schedule.getScheduleId());
            logger.severe(errorMessage);
            logger.severe(e.getMessage());
            logger.severe(Messages.PLEASE_CONTACT_SUPPORT);
            throw new Exception(String.format("%1$s%2$s%3$s", errorMessage, Messages.NEW_LINE, e.getMessage()));
        }

        return isSuccessfullyRun;

    }


    public boolean getScheduleState(
            String leaptestAddress,
            Schedule schedule,
            Integer doneStatusValue
    ) throws Exception {
        boolean isScheduleStillRunning = true;

        String uri = String.format(Messages.GET_SCHEDULE_STATE_URI, leaptestAddress, schedule.getScheduleId());

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();
            client = null;

            switch (response.getStatusCode())
            {
                case 200:

                    JsonParser parser = new JsonParser();
                    JsonObject jsonState = parser.parse(response.getResponseBody()).getAsJsonObject();
                    parser = null;

                    String ScheduleId = jsonState.get("ScheduleId").getAsString();

                    if (isScheduleStillRunning(jsonState))
                        isScheduleStillRunning = true;
                    else
                    {
                            isScheduleStillRunning = false;

                        /////////Schedule Info
                            JsonElement jsonLastRun = jsonState.get("LastRun");

                            JsonObject lastRun = jsonLastRun.getAsJsonObject();

                            String ScheduleTitle = lastRun.get("ScheduleTitle").getAsString();


                            ///////////AutomationRunItemsInfo
                            JsonArray jsonAutomationRunItems = lastRun.get("AutomationRunItems").getAsJsonArray();

                            ArrayList<String> statuses = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                statuses.add(jsonAutomationRunItem.getAsJsonObject().get("Status").getAsString());
                            ArrayList<String> elapsed = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                elapsed.add(defaultElapsedIfNull(jsonAutomationRunItem.getAsJsonObject().get("Elapsed")));
                            ArrayList<String> environments = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                environments.add(jsonAutomationRunItem.getAsJsonObject().get("Environment").getAsJsonObject().get("Title").getAsString());
                            ArrayList<String> caseIds = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                caseIds.add(jsonAutomationRunItem.getAsJsonObject().get("Case").getAsJsonObject().get("Id").getAsString());

                            ArrayList<String> caseTitles = new ArrayList<String>();
                            ArrayList<String> caseDescriptions = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems) {
                                String caseTitle = Utils.defaultStringIfNull(jsonAutomationRunItem.getAsJsonObject().get("Case").getAsJsonObject().get("Title"), "Null case Title");
                                caseDescriptions.add(Utils.defaultStringIfNull(jsonAutomationRunItem.getAsJsonObject().get("Case").getAsJsonObject().get("Description"), ""));

                                if (caseTitle.contentEquals("Null case Title"))
                                    caseTitles.add(caseTitles.get(caseTitles.size() - 1));
                                else
                                    caseTitles.add(caseTitle);
                            }

                            for (int i = 0; i < jsonAutomationRunItems.size(); i++)
                            {
                                    Integer setStatusId = Test.Status.BLOCKED;

                                    switch (statuses.get(i))
                                    {
                                        case "Passed":
                                            setStatusId = Test.Status.PASSED;
                                        break;
                                        case "Failed":
                                            setStatusId = Test.Status.FAILED;
                                        break;
                                        case "Done":
                                            setStatusId = doneStatusValue;
                                        break;
                                        case "Cancelled":
                                            setStatusId = Test.Status.RETEST;
                                        break;
                                        case "Error":
                                            setStatusId = Test.Status.RETEST;
                                        break;
                                        default:
                                            setStatusId = Test.Status.RETEST;
                                        break;
                                    }


                                    JsonArray jsonKeyframes = jsonAutomationRunItems.get(i).getAsJsonObject().get("Keyframes").getAsJsonArray();

                                    //KeyframeInfo
                                    ArrayList<String> keyFrameTimeStamps = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameTimeStamps.add(jsonKeyFrame.getAsJsonObject().get("Timestamp").getAsString());
                                    ArrayList<String> keyFrameLogMessages = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameLogMessages.add(jsonKeyFrame.getAsJsonObject().get("LogMessage").getAsString());

                                    logger.info(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));

                                    String keyFrames = String.format("Environment: %1$s%2$s",environments.get(i), Messages.NEW_LINE);

                                    int currentKeyFrameIndex = 0;

                                    for (JsonElement jsonKeyFrame : jsonKeyframes) {
                                        String level = Utils.defaultStringIfNull(jsonKeyFrame.getAsJsonObject().get("Level"), "");
                                        if (!level.contentEquals("") && !level.contentEquals("Trace")) {
                                            String stacktrace = String.format(Messages.CASE_KEYFRAME_FORMAT, keyFrameTimeStamps.get(currentKeyFrameIndex), keyFrameLogMessages.get(currentKeyFrameIndex));

                                            keyFrames += stacktrace;
                                            keyFrames += Messages.NEW_LINE;
                                        }
                                        currentKeyFrameIndex++;
                                    }

                                    logger.info(keyFrames);

                                    schedule.getCases().add(new Case(caseIds.get(i),caseTitles.get(i), setStatusId, parseExecutionTimeToSeconds(elapsed.get(i)), caseDescriptions.get(i), keyFrames, environments.get(i)));

                            }
                    }

                break;

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("%1$s%2$s",Messages.NEW_LINE,String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, schedule.getScheduleTitle(), schedule.getScheduleId()));
                    throw new Exception(errorMessage404);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("%1$s%2$s",Messages.NEW_LINE, Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (ConnectException e){
            String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
            throw new Exception(connectionErrorMessage);
        } catch (InterruptedException e) {
            throw new Exception(e);
        } catch (ExecutionException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        } catch (Exception e)
        {
            String errorMessage = String.format(Messages.SCHEDULE_STATE_FAILURE, schedule.getScheduleTitle(), schedule.getScheduleId());
            logger.severe(errorMessage);
            logger.severe(e.getMessage());
            logger.severe(Messages.PLEASE_CONTACT_SUPPORT);
            throw new Exception(String.format("%1$s%2$s%3$s", errorMessage, Messages.NEW_LINE, e.getMessage()));
        }

        return isScheduleStillRunning;

    }


    public void setTestRailTestResults(
             String testRailRunId,
             APIClient testRailAPIClient,
             Schedule schedule,
             ArrayList<Test> testRailTests
    ) throws Exception {

        try {

            ////////MAPPING SCHEDULE CASES AND TEST RESULTS

            for(Test test: testRailTests) {
                if (test.isStepCase())
                {
                    for (Case aCase : schedule.getCases()) {
                        if (aCase.getCaseName().contentEquals(test.getTestTitle()) && !aCase.isResultAlreadySet()) {

                            for (Step step : test.getSteps()) {
                                if (!step.isStepFilled()) {
                                    step.setActual(aCase.getKeyFramesLogs());
                                    step.addSeconds(aCase.getSeconds());
                                    step.setStatusId(aCase.getCaseStatus());
                                    step.setStepFilled(true);
                                    break;
                                }
                            }
                            test.setLeaptestCaseId(aCase.getCaseId());
                            aCase.setResultAlreadySet(true);
                        }
                    }

                    Integer totalSeconds = 0;
                    Integer resultStatus = Test.Status.PASSED;

                    for (Step step : test.getSteps()) {
                        if (step.getStatusId() > resultStatus)
                            resultStatus = step.getStatusId();

                        totalSeconds += step.getSeconds();
                    }

                    //logger.info(convertSecondsToTime(totalSeconds));
                    test.setElapsed(convertSecondsToTime(totalSeconds)); //set test total time
                    test.setStatusId(resultStatus);   ///set test results
                    test.setComment(resultStatus);
                    test.addComment(String.format(Messages.ADD_CASE_ID_INFORMATION_TO_COMMENT, test.getTestRailCaseId(), test.getLeaptestCaseId()));

                }
            }

            testRailAPIClient.sendPost(String.format(Messages.SET_TESTRAIL_TESTS_RESULTS_POST, testRailRunId), new TestCollection(testRailTests));

        }  catch (UnknownHostException e)
        {
            String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
            throw new Exception(connectionErrorMessage);
        } catch (APIException e) {
            throw new Exception(e);
        } catch (MalformedURLException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        } catch (Exception e) {
            throw new Exception(e);
        }

    }


    private boolean isScheduleStillRunning(JsonObject jsonState)
    {
        String status = Utils.defaultStringIfNull(jsonState.get("Status"), "Finished");

        if (status.contentEquals("Running") || status.contentEquals("Queued"))
            return true;
        else
            return false;

    }

    private Integer parseExecutionTimeToSeconds(String rawExecutionTime)
    {
        String ExecutionTotalTime[] = rawExecutionTime.split(":|\\.");

        return  Integer.parseInt(ExecutionTotalTime[0]) * 60 * 60 +  //hours
                Integer.parseInt(ExecutionTotalTime[1]) * 60 +        //minutes
                Integer.parseInt(ExecutionTotalTime[2]);            //seconds
    }

    private String defaultElapsedIfNull(JsonElement rawElapsed)
    {
        if(rawElapsed != null)
            return rawElapsed.getAsString();
        else
            return "00:00:00.0000000";

    }

    private String convertSecondsToTime(int totalSeconds)
    {
        Integer hours = totalSeconds / 3600;
        Integer minutes = (totalSeconds - (hours * 3600))/60;
        Integer seconds = totalSeconds - hours * 3600 - minutes * 60;

        String resultString = "";

        if(seconds > 0)
        {
            if(hours != 0) resultString += String.format("%1$dh",hours);
            if(minutes != 0) {
                if (resultString.length() > 0)
                    resultString += String.format(" %1$dm", minutes);
                else
                    resultString += String.format("%1$dm", minutes);
            }
            if (resultString.length() > 0)
                resultString += String.format(" %1$ds",seconds);
            else
                resultString += String.format("%1$ds",seconds);
        }
        else
            resultString = "1s";

        return resultString;
    }
}
