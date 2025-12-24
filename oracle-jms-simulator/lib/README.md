# WebLogic Client Library Directory

## Purpose
This directory is designated for the Oracle WebLogic client JAR file required by the Oracle JMS Simulator.

## Required File
Place one of the following files in this directory:
- `wlfullclient.jar` (Full client, recommended)
- `wlthint3client.jar` (Thin client, smaller size)

## How to Obtain the WebLogic Client JAR

### Option 1: Full Client (wlfullclient.jar)
1. Navigate to your WebLogic installation:
   ```bash
   cd $WL_HOME/server/lib
   ```
2. Generate the full client JAR:
   ```bash
   java -jar wljarbuilder.jar
   ```
3. Copy the generated `wlfullclient.jar` to this directory:
   ```bash
   cp wlfullclient.jar /path/to/oracle-jms-simulator/lib/
   ```

### Option 2: Thin Client (wlthint3client.jar)
1. Locate the thin client in your WebLogic installation:
   ```bash
   $WL_HOME/server/lib/wlthint3client.jar
   ```
2. Copy it to this directory:
   ```bash
   cp $WL_HOME/server/lib/wlthint3client.jar /path/to/oracle-jms-simulator/lib/
   ```

## After Placing the JAR
Once you've placed the WebLogic client JAR in this directory:
1. The application will automatically detect and use it
2. Rebuild the application:
   ```bash
   mvn clean package
   ```
3. The JAR will be included in the final packaged application

## Important Notes
- Due to Oracle licensing restrictions, we cannot distribute the WebLogic client JAR
- You must obtain it from your own WebLogic Server installation
- The JAR file should be named exactly as:
  - `wlfullclient.jar` OR
  - `wlthint3client.jar`
- Add this directory to your `.gitignore` to avoid committing the proprietary JAR
