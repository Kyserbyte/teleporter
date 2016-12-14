
OrientDB									
 ______________________________________________________________________________ 
 ___  __/__  ____/__  /___  ____/__  __ \_  __ \__  __ \__  __/__  ____/__  __ \
 __  /  __  __/  __  / __  __/  __  /_/ /  / / /_  /_/ /_  /  __  __/  __  /_/ /
 _  /   _  /___  _  /___  /___  _  ____// /_/ /_  _, _/_  /   _  /___  _  _, _/ 
 /_/    /_____/  /_____/_____/  /_/     \____/ /_/ |_| /_/    /_____/  /_/ |_|    

                                                  http://orientdb.com/teleporter


OrientDB Teleporter is a tool that synchronizes a RDBMS to an OrientDB database.
You can use Teleporter to:

- Import your existing RDBMS to OrientDB.
- Keep your OrientDB database synchronized with changes from the RDBMS. In this case the database
  on your RDBMS remains the primary and the database on OrientDB a synchronized copy. 
  Synchronization is one way, so all the changes in OrientDB database will not be propagated to
  the RDBMS.


---------------
HOW TO INSTALL
---------------

Teleporter is really easy to install, just follow these two steps:

1. Move orientdb-teleporter-${project.version}.jar contained in plugins/ folder to the
   $ORIENTDB_HOME/plugins folder.
2. Move both the scripts oteleporter.sh and oteleporter.bat (for Windows users) contained in bin/
   folder to the $ORIENTDB_HOME/bin folder.
3. Move the jdbc-drivers.json configuration file contained in config/ folder to the $ORIENTDB_HOME/config folder.


Now Teleporter is ready, you can execute it via OrientDB Studio as described here:
https://github.com/orientechnologies/orientdb-labs/blob/master/Studio-Teleporter.md

or just run the tool through the script as described in the documentation:
https://github.com/orientechnologies/orientdb-labs/blob/master/Teleporter-Index.md.