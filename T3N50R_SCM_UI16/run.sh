#!/bin/bash
# SCM UI Subsystem #16 — Team T3N50R — Run Script
# Requires: Java 11+, jfreechart.jar, jcommon.jar, mariadb-java-client.jar

# On Debian/Ubuntu: sudo apt-get install libjfreechart-java libmariadb-java
CP="/usr/share/java/jfreechart.jar:/usr/share/java/jcommon.jar:/usr/share/java/mariadb-java-client.jar"

# Optional: set DB credentials via environment
# export SCM_DB_URL="jdbc:mysql://localhost:3306/scm_db"
# export SCM_DB_USER="root"
# export SCM_DB_PASS="yourpassword"

echo "Starting SCM UI Subsystem #16..."
java -cp "scm-t3n50r.jar:$CP" com.scm.ui.SCMApplication
