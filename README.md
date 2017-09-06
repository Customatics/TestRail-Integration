# Leaptest integration for TestRail

This is Leaptest Integration plugin for TestRail

# More Details
Leaptest is a mighty automation testing system and now it can be used for running test runs (step case type only) in Testrail from version 4.x. Tested on version 5.4.0.3659.
You can easily configure integration directly in TestRail enjoying UI friendly configuration page with easy connection and schedule selection.

# Installation
1. Put files leaptest.php and TestRailsCMD.jar to TestRail root folder - the same folder where you can find index.php
2. Administration => Customizations => UI scripts => Add Script => Clear script text area => Copy all the text from Leaptest customization UI script.txt to script text area => Activate script => Save UI Script
3. If your TestRail uses Windows IIS, please provide IIS_USERS permissions for executing, creating, writing and reading files in TestRail root folder (where index.php). The same should be done for Unix/Linux systems if required. File "leaptest.php" executes "TestRailsCMD.jar", which creates/modifies log files. Each test run creates/modifies its own log file during execution.

# Instruction
1. Prepare a schedule to run in Leaptest, add cases and environments to it.
2. Create a TestRail case with STEPS. A TestRail case name MUST match a Leaptest case name in a schedule you are going to run. Add as many steps for your case as your schedule has environments. Your case MUST have a step per schedule environment.
3. Create TestRail test run with the same cases as Leaptest schedule has.
4. Press the button "Select Schedule". Leaptest integration dialog will open.
5. Enter Leaptest controller URL like http://{hostname}:9000
6. Press the button "Get Projects and Schedules". Project select menu will get all projects, and Schedule select menu will get all schedules of the selected project.
7. Select the schedule you've prepared.
8. Enter the time delay parameter in seconds. When the schedule is running, the plugin waits for this time and then asks Leaptest controller for schedule state. If the schedule is still running, the plugin will wait again. If you run cases that are executed within minutes or hours, it's better to set this parameter value equivalent to some minutes, for example 600 for 10 minutes interval.
9. Select how cases with "done" status should be interpreted.
10. Enter TestRail URL, you can find it in Administartion => Site Settings => Web Address.
11. Enter your Login.
12. Enter your password.
13. TestRail URL, login and password are required for TestRail API to set results.
14. Press the button "Run Schedule".
15. The schedule is running in the background and the results are automatically posted back to TestRail. To see them simply open/reload page with test run.
16. Enjoy.
17. Each run has its own log file. You can find it in the folder with TestRailsCMD.jar     

# Screenshots

![ScreenShot]()
