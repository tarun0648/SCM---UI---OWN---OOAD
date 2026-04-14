@echo off
REM SCM UI Subsystem #16 — Team T3N50R — Windows Run Script
REM Download JARs and place in same folder as scm-t3n50r.jar
REM Required: jfreechart-1.0.19.jar, jcommon-1.0.23.jar, mariadb-java-client-2.7.6.jar
set CP=scm-t3n50r.jar;jfreechart.jar;jcommon.jar;mariadb-java-client.jar
echo Starting SCM UI Subsystem #16...
java -cp "%CP%" com.scm.ui.SCMApplication
pause
