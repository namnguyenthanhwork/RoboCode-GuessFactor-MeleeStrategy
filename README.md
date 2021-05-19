# Introduction
## Overview
This project is my robot for the game RoboCode
- You should read docs in our project ^^
- https://robocode.sourceforge.io/
- https://github.com/robo-code/robocode

## My Robots
Chaos mode
1. <strong>Cham</strong>:
  - Radar: scan 360
  - Gun: Use my own implementation. After that, I realized that the idea is similar to Waves algorithm.
  - Movement: 
    - Just random moving in the same direction. 
    - Wall-smooth: implemented by myself. The code is huge, but it works nicely. May need huge refactor, so I won't, lol!!!
    - 1 enemie: reverse direction Solo mode.
2. <strong>The Unfolding Robot</strong>:
  - My advanced robot compare to Cham.
    I named it after reading the book "Reinventing Organizations": the robot is on the journey of unfolding itself.
  - In general, it will behave differently when 1-on-1 and melee (battle with many bots). It combine strategies for Radar, Gun and Movement differently depend on the situations.
  - Radar: 
    - Melee: Optimal Scan (just scan area with enemies, don't scan redundant areas)
    - One-on-One: Lock radar to the enemy.
  - Gun: Use Waves & GuessFactoring Target + Circular & Linear Pattern Prediction.
  - Movement:
    - Melee:
      - Anti-Gravity movement: 
        - When near the walls: it will run to the destination point in the shortest way (turn the body with the shortest angle, reverse the movement direction if necessary).
        - In safe area (far away from the walls):  to reach a destination, it runs smoothly without reversing the movement direction, the turning angle can be large. The path may longer but it can avoid being stuck at moving in a small area.
    - One-on-One: Apply Oscillator movement which is running perpendicular with enemy.   
    - Wall-smooth: In any case, if it's near the wall, run smoothly to avoid the wall without reversing the movement direction, just change the movement angle: reuse the code of Briareos.
    - Hitting wall or Enemies: In any case, if it hits walls or enemies, reverse direction and turn 90 degree (to avoid back-and-forth stuck).

## Robot Guideline
### Steps to run the current robots inside my project (`The Unfolding Robot` or `Cham`):
The RoboCode's library could not be found in remote Maven repository, hence we have to add that library (which is stored in RoboCode game's `./libs` folder) into our local Maven repository by the following steps: 

In the root `pom.xml` file, change the home folder where you installed your RoboCode game:
```
<!-- This is the folder you setup your robocode -->
<robocode.home.folder>D:/SourceCode/RoboCode/robocode</robocode.home.folder>
```

#### In Eclipse
http://robowiki.net/wiki/Robocode/Running_from_Eclipse

#### In Intellij
In Intellij > on the menu bar > Run > Edit Configurations
    ![Alt text](docs/SetUpYourRobotInIntelliJ.png?raw=true "Edit Configurations")
Then run your application, it will start the RoboCode game.
- Main class: robocode.RoboCode
- VM Options: -Xmx512M -Dsun.io.useCanonCaches=false -Ddebug=true
- Working Directory: /home/kevintran/SourceCode/Personal/RoboCode/robocode
- User class and module: the-unfolding-robot

#### In RoboCode game:
At this time, when you starting a battle, you won't see your robot because you haven't imported it into the game yet. To do that, follow these steps:
 - On the menu bar > Options > Preferences > Development Options: Add the path to the folder storing your robot's build classes: for example: `$YOUR_ROBOTS_PROJECT_FOLDER/the-unfolding-robot/target/classes`
 - Now, starting a new battle (menu bar > Battle > New), you will see your robot.

### Steps to create your new robot
- http://robowiki.net/wiki/Robocode/Eclipse/Create_a_Project
- http://robowiki.net/wiki/Robocode/Eclipse/Create_a_Robot
- http://robowiki.net/wiki/Robocode/Add_a_Robot_Project
- http://robowiki.net/wiki/Robocode/Running_from_Eclipse
- http://robowiki.net/wiki/Robocode/Developers_Guide_for_building_Robocode

**The summary setup to package your Robot to a jar file:**
Instead of starting your RoboCode from IntelliJ, you make want to package your bot into a jar file and share with your friend. To do that, your bot must follow the below structure:
- robot package name: 
    - You can use any name here
    - For example: `org.tnmk.robocode.robot`
- robot class name: 
    - This is the robot name
    - You can put any name here
    - For example: `BeginnerBasicRobot` (in package `org.tnmk.robocode.robot`)
- properties:
    - This file describes what should be the main class of Robot.  
    - Put it at the same package in `/resources` folder and have the same name of robot's class.
    - For example: `org.tnmk.robocode.robot.BeginnerBasicRobot.properties`
- build jar:
    - The final jar file must have the same name of robot's name 
    - For example: `BeginnerBasicRobot.jar`

### Start your robot
http://robowiki.net/wiki/Robocode/Getting_Started

### Implement your robot
Best diagram to show terms in RoboCode:
- https://coggle.it/diagram/51ade2c0e354014b1c00a43c/t/robocode-strategies/a19ae89e8368aa6171bd485adc1017fae44904e554ae9272fec52f6bb85c2294
- Basic information with great images and definitions: https://slideplayer.com/slide/3731495/
- https://www.ibm.com/developerworks/java/library/j-robocode/
- https://www.ibm.com/developerworks/java/library/j-robocode2/j-robocode2-pdf.pdf (some very useful information in the core)

List of sample code: http://old.robowiki.net/robowiki?CodeSnippets

Some tutorial, terms and algorithm for your robot: 
- http://robowiki.net/wiki/Tutorials
- https://www.ibm.com/developerworks/library/j-robotips/index.html
- https://robowiki.net/wiki/Melee_Strategy#Evaluating_your_Melee_bot

Radar:
  - Basic information: http://robowiki.net/wiki/Melee_Radar
  - https://www.ibm.com/developerworks/library/j-radar/index.html

Aim Target:
  - Waves: 
    - http://robowiki.net/wiki/Waves    
    - http://old.robowiki.net/robowiki?EnemyWave
  - GuessFactors: 
    - http://robowiki.net/wiki/GuessFactors
    - http://robowiki.net/wiki/GuessFactor_Targeting_(traditional)
  - Displacement Vector: http://robowiki.net/wiki/Displacement_Vector  
  - Play It Forward:
    - http://robowiki.net/wiki/Play_It_Forward
  - VirtualBullet:
    - http://old.robowiki.net/robowiki?VirtualBullets
    - http://old.robowiki.net/robowiki?VirtualBullets/VirtualBulletsSampleBot          
    
http://mark.random-article.com/robocode/index.html

Movement: http://old.robowiki.net/robowiki?Movement
  - Anti-Gravity: 
    - Basic implementation: http://robowiki.net/wiki/Anti-Gravity_Tutorial
    - Detail implementation: https://www.ibm.com/developerworks/java/library/j-antigrav/index.html?ca=drs-
  - Enemy Dodging Movement: http://robowiki.net/wiki/Enemy_Dodging_Movement (it seems not to be as effective as anti-gravity, but much simpler to implement)
  - Wall Smoothing: 
    - http://robowiki.net/wiki/Wall_Smoothing
    - http://robowiki.net/wiki/Wall_Smoothing/Implementations
    - http://old.robowiki.net/robowiki?EscapeArea
  - Random Movement:
    - http://old.robowiki.net/robowiki?RandomMovementBot
  - Utilities: 
    - http://old.robowiki.net/robowiki?Movement/CodeSnippetGoTo    

Utilities functions: https://www.programcreek.com/java-api-examples/index.php?api=robocode.util.Utils

Save data between rounds (matches):
 - http://old.robowiki.net/robowiki?IntelligenceManagement

# Some interesting Robots
http://robowiki.net/wiki/DrussGT
http://robowiki.net/wiki/Diamond

# Troubleshoot
In order to RoboCode can recognize your Robot jar, the jar's filename must be the same as the robot's classname (case sensitive?)
And your Robot should need the properties file to include some information.

In application > Preferences > Development Options: 
- You will point to the folder which will contains package of robot classes, for example: `/SourceCode/RoboCode/simple-robot/target/classes`
- Anyway, this folder should not contains different folder point to the same robot's class name. Otherwise, the application will confuse and pickup the first one.  
For example, if you point to `/SourceCode/RoboCode/simple-robot/target/`, that folder will have `./classes/` and `*.jar` files which are both store robot's classes. It will cause problem.

---------------------------------------------------

If a robot fails to complete his turn in the time allotted, the turn will be skipped. 
https://stackoverflow.com/questions/33527613/onscannedrobot-method-never-being-called

# Debug

## Debug by Logging
Use `robot.out.println("XXX");`
Then, when starting game, click to the name of the robot, it will shows logs for that specific robot.

## Debug by Painting
In the code, use `robot.getGraphics().drawXxx()` methods.
When viewing log of a specific robot, you can see it's painting log by click to button "Paint"

# Some notes when calculate angles in RoboCode.
Usually, the bearing and heading values you receive from the game is not the same of the actual values in Geometry maths.
Hence when calculating sin/cos..., the result will be wrong.
That's why before applying sin/cos..., you should convert them to geometry angles.

Use `toGeometryRadian(double inGameRadian)` and `toGeometryDegree(double inGameDegree)` before calculate geometry formulas such as `Math.sin(radian)`, `Math.cos(radian)`

# UnitTest
Some example code:
 - https://hiraidekeone.wordpress.com/2013/02/26/robocode-quality-assurance-and-junit-testing/
 - https://bretkikehara.wordpress.com/2013/02/26/robocode-unit-testing-goodness/
 - https://github.com/robo-code/robocode/blob/master/plugins/testing/robocode.testing.samples/src/main/java/sample/TestWallBehavior.java

Set up to run this test:
 - In IntelliJ menu > Run > Edit Configurations > In VM Options, add: " -Drobocode.home=D:\SourceCode\RoboCode\robocode"
 - Or use the command line: 
    ```
    mvn clean install -Drobocode.home="D:\SourceCode\RoboCode\robocode"
    ```

# Future improvement
General: strategy for the end game: http://old.robowiki.net/robowiki?EndingGame:
 - Don't fire when we already missed target a lot.
 - Or if we still have a lot of energy, while the enemy does not have much:
   - Try to run directly to hit the enemy.
   - Fire bullet surround enemies' escape area.
 - Or when the risk we get hit is high, don't try to save energy, try to hit back.

Movement:
 - Movement:
    - Move to a destination: instead of using the same velocity, learn from the SuperSpinBot: reduce velocity and increase the turn rate.
    - Zic-Zac movement to a destination position.
    - Random move when doing Anti-Gravity: avoid anti-gravity moving around one area for too long.
 - Gun: 
    - the enemies with low energy (especially lower than 1) will become the high priority targets.
    - the damage of bullet should not be bigger than the enemy's energy.  
