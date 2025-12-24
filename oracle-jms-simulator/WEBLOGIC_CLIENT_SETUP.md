# WebLogic Client Setup Guide

This guide provides detailed instructions for setting up the Oracle WebLogic client library required by the Oracle JMS Simulator.

## Why is this needed?

The Oracle JMS Simulator connects to Oracle WebLogic JMS servers using the WebLogic JNDI API. The WebLogic client JAR contains the necessary classes (like `weblogic.jndi.WLInitialContextFactory`) to establish this connection.

Oracle does not publish the WebLogic client JAR to public Maven repositories due to licensing restrictions. You must obtain it from your WebLogic Server installation.

## Quick Setup (Recommended)

This is the easiest method - just place the JAR file in the `lib` directory and build.

### Step 1: Obtain the WebLogic Client JAR

Choose one of the following options:

#### Option A: Full Client (wlfullclient.jar) - Recommended

1. Open a terminal/command prompt
2. Navigate to your WebLogic Server installation:
   ```bash
   cd $WL_HOME/server/lib
   ```
   Where `$WL_HOME` is your WebLogic installation directory, e.g.:
   - Linux: `/opt/oracle/middleware/wlserver`
   - Windows: `C:\Oracle\Middleware\wlserver`

3. Generate the full client JAR:
   ```bash
   java -jar wljarbuilder.jar
   ```

4. This creates `wlfullclient.jar` in the same directory (typically 70-100 MB)

#### Option B: Thin Client (wlthint3client.jar) - Lightweight

The thin client is pre-built and located at:
```bash
$WL_HOME/server/lib/wlthint3client.jar
```
This is a smaller file (5-10 MB) and suitable for most use cases.

### Step 2: Copy JAR to lib Directory

Copy the WebLogic client JAR to the `lib` directory in the Oracle JMS Simulator project:

```bash
# For full client:
cp $WL_HOME/server/lib/wlfullclient.jar /path/to/oracle-jms-simulator/lib/

# OR for thin client:
cp $WL_HOME/server/lib/wlthint3client.jar /path/to/oracle-jms-simulator/lib/
```

**Important:** The file must be named exactly:
- `wlfullclient.jar` OR
- `wlthint3client.jar`

### Step 3: Build the Application

```bash
cd oracle-jms-simulator
mvn clean package
```

**That's it!** The build process automatically includes the JAR from the `lib` directory, and it will be bundled with the final application.

## Alternative Method: Maven Local Repository

If you prefer the traditional Maven approach instead of using the `lib` directory:
```

### Step 2: Install to Local Maven Repository

```bash
mvn install:install-file \
  -Dfile=$WL_HOME/server/lib/wlthint3client.jar \
  -DgroupId=com.oracle.weblogic \
  -DartifactId=wlthint3client \
  -Dversion=14.1.1.0 \
  -Dpackaging=jar
```

Replace `$WL_HOME` with your actual path and adjust the version as needed.

### Step 3: Add Dependency to pom.xml

Edit `oracle-jms-simulator/pom.xml` and add:

```xml
<!-- Oracle WebLogic Thin Client -->
<dependency>
    <groupId>com.oracle.weblogic</groupId>
    <artifactId>wlthint3client</artifactId>
    <version>14.1.1.0</version>
</dependency>
```

## Verification

After installing the WebLogic client JAR and adding the dependency:

1. Rebuild the application:
   ```bash
   cd oracle-jms-simulator
   mvn clean package
   ```

2. Run the application:
   ```bash
   java -jar target/oracle-jms-simulator-1.0.0.jar
   ```

3. In the application:
   - Enter your connection details
   - Click "Test Connection"
   - You should see "Connection test successful" if everything is configured correctly

## Troubleshooting

### Maven install command fails

**Error:** `mvn: command not found`

**Solution:** Install Maven or use the full path to mvn executable

---

**Error:** File not found

**Solution:** Verify the path to the JAR file. Use `ls -la` (Linux/Mac) or `dir` (Windows) to confirm the file exists.

### Build fails after adding dependency

**Error:** `Could not find artifact com.oracle.weblogic:wlfullclient:jar:14.1.1.0`

**Solution:** 
1. Verify the JAR was installed to local Maven repository (check `~/.m2/repository/com/oracle/weblogic/`)
2. Ensure the version in pom.xml matches the version used in `mvn install:install-file`

### Connection still fails with NoInitialContextException

**Error:** `Cannot instantiate class: weblogic.jndi.WLInitialContextFactory`

**Possible causes:**
1. Dependency not added to pom.xml
2. Application not rebuilt after adding dependency
3. Wrong artifact ID or version in pom.xml

**Solution:**
1. Verify dependency is in pom.xml
2. Run `mvn clean package` to rebuild
3. Check that the WebLogic client JAR is included in the built JAR:
   ```bash
   jar tf target/oracle-jms-simulator-1.0.0.jar | grep weblogic
   ```

## Which Option Should I Choose?

| Feature | Full Client (wlfullclient.jar) | Thin Client (wlthint3client.jar) |
|---------|-------------------------------|----------------------------------|
| Size | 70-100 MB | 5-10 MB |
| Compatibility | Best | Good |
| Generation Required | Yes (run wljarbuilder) | No (pre-built) |
| Recommended For | Production, Complex scenarios | Development, Simple scenarios |

**Recommendation:** Use the thin client (wlthint3client.jar) for development and testing. Use the full client (wlfullclient.jar) if you encounter any compatibility issues.

## Alternative: System Classpath

If you cannot modify the pom.xml or prefer not to install to Maven repository, you can add the WebLogic client JAR to the classpath when running:

```bash
java -cp "target/oracle-jms-simulator-1.0.0.jar:$WL_HOME/server/lib/wlthint3client.jar" \
  org.springframework.boot.loader.JarLauncher
```

Note: This approach is not recommended as it's less portable.

## Support

If you continue to have issues:
1. Check the console logs for detailed error messages
2. Verify your WebLogic Server is running and accessible
3. Ensure network connectivity to the WebLogic Server
4. Test connection using WebLogic's built-in tools first
