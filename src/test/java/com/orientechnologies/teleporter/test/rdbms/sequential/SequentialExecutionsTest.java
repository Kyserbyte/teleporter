/*
 * Copyright 2015 OrientDB LTD (info--at--orientdb.com)
 * All Rights Reserved. Commercial License.
 * 
 * NOTICE:  All information contained herein is, and remains the property of
 * OrientDB LTD and its suppliers, if any.  The intellectual and
 * technical concepts contained herein are proprietary to
 * OrientDB LTD and its suppliers and may be covered by United
 * Kingdom and Foreign Patents, patents in process, and are protected by trade
 * secret or copyright law.
 * 
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from OrientDB LTD.
 * 
 * For more information: http://www.orientdb.com
 */

package com.orientechnologies.teleporter.test.rdbms.sequential;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.teleporter.context.OOutputStreamManager;
import com.orientechnologies.teleporter.context.OTeleporterContext;
import com.orientechnologies.teleporter.importengine.rdbms.dbengine.ODBQueryEngine;
import com.orientechnologies.teleporter.model.dbschema.OSourceDatabaseInfo;
import com.orientechnologies.teleporter.nameresolver.OJavaConventionNameResolver;
import com.orientechnologies.teleporter.persistence.handler.OHSQLDBDataTypeHandler;
import com.orientechnologies.teleporter.strategy.rdbms.ODBMSNaiveAggregationStrategy;
import com.orientechnologies.teleporter.strategy.rdbms.ODBMSNaiveStrategy;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.teleporter.util.OFileManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Gabriele Ponzi
 * @email <g.ponzi--at--orientdb.com>
 */

public class SequentialExecutionsTest {

  private OTeleporterContext            context;
  private ODBMSNaiveStrategy            naiveImportStrategy;
  private ODBMSNaiveAggregationStrategy naiveAggregationImportStrategy;
  private ODBQueryEngine                dbQueryEngine;
  private String driver   = "org.hsqldb.jdbc.JDBCDriver";
  private String jurl     = "jdbc:hsqldb:mem:mydb";
  private String username = "SA";
  private String password = "";
  private String dbName = "testOrientDB";
  private String outParentDirectory = "embedded:target/";
  private String outOrientGraphUri = this.outParentDirectory + this.dbName;
  private OSourceDatabaseInfo sourceDBInfo;

  // Queries
  private String getElementsFromClassQuery = "select * from ?";

  @Before
  public void init() {
    this.context = OTeleporterContext.newInstance();
    this.dbQueryEngine = new ODBQueryEngine(this.driver);
    this.context.setDbQueryEngine(this.dbQueryEngine);
    this.context.setOutputManager(new OOutputStreamManager(0));
    this.context.setNameResolver(new OJavaConventionNameResolver());
    this.context.setDataTypeHandler(new OHSQLDBDataTypeHandler());
    this.naiveImportStrategy = new ODBMSNaiveStrategy();
    this.naiveAggregationImportStrategy = new ODBMSNaiveAggregationStrategy();
    this.sourceDBInfo = new OSourceDatabaseInfo("source", this.driver, this.jurl, this.username, this.password);
  }

  @After
  public void tearDown() {

    // closing OrientDB instance
    this.context.closeOrientDBInstance();

    try {

      // Deleting database directory
      OFileManager.deleteResource(this.outOrientGraphUri.replace("embedded:",""));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Test

  /*
   *  Adding a column to a table.
   */

  public void schemaModificationTest1() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, primary key (ID))";
      st = connection.createStatement();
      st.execute(actorTableBuilding);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      OClass actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(3, actorVertexType.properties().size());

      OProperty currentProperty = null;
      Iterator<OProperty> itProperties = actorVertexType.properties().iterator();

      Map<String, OProperty> name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(3, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      this.context.closeOrientDBInstance();


      /*
       * Modify of the db schema, adding a column
       */

      String addColumn = "alter table ACTOR " + "add BIRTHDAY date";
      st.execute(addColumn);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      currentProperty = null;
      itProperties = actorVertexType.properties().iterator();

      name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("birthday");
      assertEquals("birthday", currentProperty.getName());
      assertEquals(OType.DATE, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   *  Removing a column from a table.
   */

  public void schemaModificationTest2() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, birthday date, primary key (ID))";
      st = connection.createStatement();
      st.execute(actorTableBuilding);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      OClass actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      OProperty currentProperty = null;
      Iterator<OProperty> itProperties = actorVertexType.properties().iterator();

      Map<String, OProperty> name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("birthday");
      assertEquals("birthday", currentProperty.getName());
      assertEquals(OType.DATE, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      this.context.closeOrientDBInstance();


      /*
       * Modify of the db schema, removing a column
       */

      String removeColumn = "alter table ACTOR " + "drop column BIRTHDAY";
      st.execute(removeColumn);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(3, actorVertexType.properties().size());

      currentProperty = null;
      itProperties = actorVertexType.properties().iterator();

      name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(3, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   *  Modify of a column.
   */

  public void schemaModificationTest3() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, birthday varchar(256), primary key (ID))";
      st = connection.createStatement();
      st.execute(actorTableBuilding);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      OClass actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      OProperty currentProperty = null;
      Iterator<OProperty> itProperties = actorVertexType.properties().iterator();

      Map<String, OProperty> name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("birthday");
      assertEquals("birthday", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      this.context.closeOrientDBInstance();


      /*
       * Modify of the db schema, changing a column (name and type)
       */

      String modifyColumn = "alter table ACTOR " + "alter column BIRTHDAY date";
      st.execute(modifyColumn);

      modifyColumn = "alter table ACTOR " + "alter column BIRTHDAY rename to ANNIVERSARY";
      st.execute(modifyColumn);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      currentProperty = null;
      itProperties = actorVertexType.properties().iterator();

      name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("anniversary");
      assertEquals("anniversary", currentProperty.getName());
      assertEquals(OType.DATE, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   *  Modify of a relationship (foreign key).
   */

  public void schemaModificationTest4() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String directorTableBuilding = "create memory table DIRECTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, BESTFILM varchar(256) not null, primary key (ID))";
      st = connection.createStatement();
      st.execute(directorTableBuilding);

      String filmTableBuilding =
          "create memory table FILM (ID varchar(256) not null," + " TITLE varchar(256) not null, DIRECTOR varchar(256) not null,"
              + " primary key (ID), constraint director foreign key (DIRECTOR) references DIRECTOR(ID))";
      st.execute(filmTableBuilding);

      // Records Inserting

      String directorFilling = "insert into DIRECTOR (ID,NAME,SURNAME,BESTFILM) values (" + "('D001','Quentin','Tarantino','F001'),"
          + "('D002','Martin','Scorsese','F002'))";
      st.execute(directorFilling);

      String filmFilling =
          "insert into FILM (ID,TITLE,DIRECTOR) values (" + "('F001','Pulp Fiction','D001')," + "('F002','Shutter Island','D002'))";
      st.execute(filmFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(4, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));
      assertEquals(2, orientGraph.countClass("Film"));

      // edges check

      assertEquals(2, orientGraph.countClass("E"));
      assertEquals(2, orientGraph.countClass("HasDirector"));

      this.context.closeOrientDBInstance();


      /*
       * Modify of the db schema, adding a table
       */

      String dropOldForeignKey = "alter table FILM drop constraint director";
      st.execute(dropOldForeignKey);

      String addNewForeignKey = "alter table DIRECTOR add foreign key (BESTFILM) references FILM(ID)";
      st.execute(addNewForeignKey);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(4, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));
      assertEquals(2, orientGraph.countClass("Film"));

      // edges check

      assertEquals(2, orientGraph.countClass("E"));
      assertEquals(2, orientGraph.countClass("HasBestFilm"));

      assertNull(orientGraph.getClass("HasDirector"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   *  Adding a table.
   */

  public void schemaModificationTest5() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, birthday varchar(256), primary key (ID))";
      st = connection.createStatement();
      st.execute(actorTableBuilding);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      OClass actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      OProperty currentProperty = null;
      Iterator<OProperty> itProperties = actorVertexType.properties().iterator();

      Map<String, OProperty> name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("birthday");
      assertEquals("birthday", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      this.context.closeOrientDBInstance();


      /*
       * Modify of the db schema, adding a table
       */

      String addTable = "create memory table FILM (ID varchar(256) not null, "
          + "TITLE varchar(256) not null, DIRECTOR varchar(256) not null, primary key (ID))";
      st.execute(addTable);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      currentProperty = null;
      itProperties = actorVertexType.properties().iterator();

      name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("birthday");
      assertEquals("birthday", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      OClass filmVertexType = orientGraph.getClass("Film");

      assertNotNull(filmVertexType);
      assertEquals(3, filmVertexType.properties().size());

      currentProperty = null;
      itProperties = filmVertexType.properties().iterator();

      name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(3, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("title");
      assertEquals("title", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("director");
      assertEquals("director", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   *  Adding a relationship (foreign key).
   */

  public void schemaModificationTest6() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String directorTableBuilding = "create memory table DIRECTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, primary key (ID))";
      st = connection.createStatement();
      st.execute(directorTableBuilding);

      String filmTableBuilding = "create memory table FILM (ID varchar(256) not null,"
          + " TITLE varchar(256) not null, DIRECTOR varchar(256) not null, primary key (ID))";
      st.execute(filmTableBuilding);

      // Records Inserting

      String directorFilling =
          "insert into DIRECTOR (ID,NAME,SURNAME) values (" + "('D001','Quentin','Tarantino')," + "('D002','Martin','Scorsese'))";
      st.execute(directorFilling);

      String filmFilling =
          "insert into FILM (ID,TITLE,DIRECTOR) values (" + "('F001','Pulp Fiction','D001')," + "('F002','Shutter Island','D002'))";
      st.execute(filmFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(4, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));
      assertEquals(2, orientGraph.countClass("Film"));

      // edges check

      assertEquals(0, orientGraph.countClass("E"));

      this.context.closeOrientDBInstance();


      /*
       *  Adding a relationship (foreign key).
       */

      String addNewForeignKey = "alter table FILM add foreign key (DIRECTOR) references DIRECTOR(ID)";
      st.execute(addNewForeignKey);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(4, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));
      assertEquals(2, orientGraph.countClass("Film"));

      // edges check

      assertEquals(2, orientGraph.countClass("E"));
      assertEquals(2, orientGraph.countClass("HasDirector"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   *  Removing a table.
   */

  public void schemaModificationTest7() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, birthday varchar(256), primary key (ID))";
      st = connection.createStatement();
      st.execute(actorTableBuilding);

      String filmTableBuilding = "create memory table FILM (ID varchar(256) not null,"
          + " TITLE varchar(256) not null, DIRECTOR varchar(256) not null, primary key (ID))";
      st.execute(filmTableBuilding);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      OClass actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      OProperty currentProperty = null;
      Iterator<OProperty> itProperties = actorVertexType.properties().iterator();

      Map<String, OProperty> name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("birthday");
      assertEquals("birthday", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      OClass filmVertexType = orientGraph.getClass("Film");

      assertNotNull(filmVertexType);
      assertEquals(3, filmVertexType.properties().size());

      currentProperty = null;
      itProperties = filmVertexType.properties().iterator();

      name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(3, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("title");
      assertEquals("title", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("director");
      assertEquals("director", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      this.context.closeOrientDBInstance();


      /*
       * Modify of the db schema, removing a table
       */

      String removeTable = "drop table FILM";
      st.execute(removeTable);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      actorVertexType = orientGraph.getClass("Actor");

      assertNotNull(actorVertexType);
      assertEquals(4, actorVertexType.properties().size());

      currentProperty = null;
      itProperties = actorVertexType.properties().iterator();

      name2props = new HashMap<String, OProperty>();

      while (itProperties.hasNext()) {
        currentProperty = itProperties.next();
        name2props.put(currentProperty.getName(), currentProperty);
      }

      assertEquals(4, name2props.keySet().size());
      currentProperty = name2props.get("id");
      assertEquals("id", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("birthday");
      assertEquals("birthday", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("name");
      assertEquals("name", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());
      currentProperty = name2props.get("surname");
      assertEquals("surname", currentProperty.getName());
      assertEquals(OType.STRING, currentProperty.getType());

      filmVertexType = orientGraph.getClass(filmTableBuilding);

      assertNull(filmVertexType);

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   *  Removing a relationship (foreign key).
   */

  public void schemaModificationTest8() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String directorTableBuilding = "create memory table DIRECTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, primary key (ID))";
      st = connection.createStatement();
      st.execute(directorTableBuilding);

      String filmTableBuilding = "create memory table FILM (ID varchar(256) not null,"
          + " TITLE varchar(256) not null, DIRECTOR varchar(256) not null, primary key (ID),"
          + " constraint director foreign key (DIRECTOR) references DIRECTOR(ID))";
      st.execute(filmTableBuilding);

      // Records Inserting

      String directorFilling =
          "insert into DIRECTOR (ID,NAME,SURNAME) values (" + "('D001','Quentin','Tarantino')," + "('D002','Martin','Scorsese'))";
      st.execute(directorFilling);

      String filmFilling =
          "insert into FILM (ID,TITLE,DIRECTOR) values (" + "('F001','Pulp Fiction','D001')," + "('F002','Shutter Island','D002'))";
      st.execute(filmFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(this.outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(4, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));
      assertEquals(2, orientGraph.countClass("Film"));

      // edges check

      ODatabaseRecordThreadLocal.INSTANCE.set((ODatabaseDocumentInternal) orientGraph);
      assertEquals(2, orientGraph.countClass("E"));
      assertEquals(2, orientGraph.countClass("HasDirector"));

      this.context.closeOrientDBInstance();


      /*
       *  Removing a relationship
       */

      String dropForeignKey = "alter table FILM drop constraint director";
      st.execute(dropForeignKey);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(4, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));
      assertEquals(2, orientGraph.countClass("Film"));

      // edges check

      assertEquals(0, orientGraph.countClass("E"));
      assertNull(orientGraph.getClass("HasDirector"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }

  }

  @Test

  /*
   * Adding records to a table.
   */

  public void contentModificationTest1() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, birthday varchar(256), primary key (ID))";
      st = connection.createStatement();
      st.execute(actorTableBuilding);

      // Records Inserting

      String actorFilling =
          "insert into ACTOR (ID,NAME,SURNAME) values (" + "('A001','John','Travolta')," + "('A002','Samuel','Lee Jackson'),"
              + "('A003','Bruce','Willis')," + "('A004','Leonardo','Di Caprio')," + "('A005','Ben','Kingsley'),"
              + "('A006','Mark','Ruffalo')," + "('A007','Jack','Nicholson')," + "('A008','Matt','Damon'))";
      st.execute(actorFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(8, orientGraph.countClass("V"));
      assertEquals(8, orientGraph.countClass("Actor"));

      // edges check

      assertEquals(0, orientGraph.countClass("E"));

      this.context.closeOrientDBInstance();


      /*
       * Adding records to the film table
       */

      actorFilling = "insert into ACTOR (ID,NAME,SURNAME) values (" + "('A009','Christian','Bale')," + "('A010','Hugh','Jackman'))";
      st.executeQuery(actorFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(10, orientGraph.countClass("V"));
      assertEquals(10, orientGraph.countClass("Actor"));

      // edges check

      assertEquals(0, orientGraph.countClass("E"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   * Adding records to a join table (aggregation strategy).
   */

  public void contentModificationTest2() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String filmTableBuilding =
          "create memory table FILM (ID varchar(256) not null," + " TITLE varchar(256) not null, primary key (ID))";
      st = connection.createStatement();
      st.execute(filmTableBuilding);

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, primary key (ID))";
      st.execute(actorTableBuilding);

      String film2actorTableBuilding = "create memory table FILM_ACTOR (FILM_ID varchar(256) not null, ACTOR_ID  varchar(256),"
          + " primary key (FILM_ID,ACTOR_ID), foreign key (FILM_ID) references FILM(ID), foreign key (ACTOR_ID) references ACTOR(ID))";
      st.execute(film2actorTableBuilding);

      // Records Inserting

      String filmFilling =
          "insert into FILM (ID,TITLE) values (" + "('F001','The Wolf Of Wall Street')," + "('F002','Shutter Island'),"
              + "('F003','The Departed'))";
      st.execute(filmFilling);

      String actorFilling =
          "insert into ACTOR (ID,NAME,SURNAME) values (" + "('A001','Leonardo','Di Caprio')," + "('A002','Matthew', 'McConaughey'),"
              + "('A003','Ben','Kingsley')," + "('A004','Mark','Ruffalo')," + "('A005','Jack','Nicholson'),"
              + "('A006','Matt','Damon'))";
      st.execute(actorFilling);

      String film2actorFilling =
          "insert into FILM_ACTOR (FILM_ID,ACTOR_ID) values (" + "('F001','A001')," + "('F001','A002')," + "('F002','A001'),"
              + "('F002','A003')," + "('F002','A004')," + "('F003','A001')," + "('F003','A005')," + "('F003','A006'))";
      st.execute(film2actorFilling);

      this.naiveAggregationImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(this.outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(9, orientGraph.countClass("V"));
      assertEquals(6, orientGraph.countClass("Actor"));
      assertEquals(3, orientGraph.countClass("Film"));

      // edges check

      assertEquals(8, orientGraph.countClass("E"));
      assertEquals(8, orientGraph.countClass("FilmActor"));

      this.context.closeOrientDBInstance();


      /*
       * Adding records to the film, actor and join tables
       */

      actorFilling = "insert into ACTOR (ID,NAME,SURNAME) values (" + "('A007','Michael','Caine'))";
      st.executeQuery(actorFilling);

      filmFilling = "insert into FILM (ID,TITLE) values (" + "('F004','Inception'))";
      st.executeQuery(filmFilling);

      film2actorFilling = "insert into FILM_ACTOR (FILM_ID,ACTOR_ID) values (" + "('F004','A001')," + "('F004','A007'))";
      st.executeQuery(film2actorFilling);

      this.naiveAggregationImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(this.outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(11, orientGraph.countClass("V"));
      assertEquals(7, orientGraph.countClass("Actor"));
      assertEquals(4, orientGraph.countClass("Film"));

      // edges check

      assertEquals(10, orientGraph.countClass("E"));
      assertEquals(10, orientGraph.countClass("FilmActor"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   * Adding a table with records.
   */

  public void contentModificationTest3() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String directorTableBuilding = "create memory table DIRECTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, primary key (ID))";
      st = connection.createStatement();
      st.execute(directorTableBuilding);

      // Records Inserting

      String directorFilling =
          "insert into DIRECTOR (ID,NAME,SURNAME) values (" + "('D001','Quentin','Tarantino')," + "('D002','Martin','Scorsese'))";
      st.execute(directorFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(2, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));

      this.context.closeOrientDBInstance();


      /*
       * Adding a table with records e foreign key
       */

      String filmTableBuilding = "create memory table FILM (ID varchar(256) not null,"
          + " TITLE varchar(256) not null, DIRECTOR varchar(256) not null, primary key (ID), "
          + " foreign key (DIRECTOR) references DIRECTOR(ID))";
      st.executeQuery(filmTableBuilding);

      // Records Inserting

      String filmFilling =
          "insert into FILM (ID,TITLE,DIRECTOR) values (" + "('F001','Pulp Fiction','D001')," + "('F002','Shutter Island','D002'),"
              + "('F003','The Departed','D002'))";
      st.execute(filmFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */
      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(5, orientGraph.countClass("V"));
      assertEquals(2, orientGraph.countClass("Director"));
      assertEquals(3, orientGraph.countClass("Film"));

      // edges check

      assertEquals(3, orientGraph.countClass("E"));
      assertEquals(3, orientGraph.countClass("HasDirector"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }

  }

  @Test

  /*
   * Adding a join table with records (aggregation strategy).
   */

  public void contentModificationTest4() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String filmTableBuilding =
          "create memory table FILM (ID varchar(256) not null," + " TITLE varchar(256) not null, primary key (ID))";
      st = connection.createStatement();
      st.execute(filmTableBuilding);

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, primary key (ID))";
      st.execute(actorTableBuilding);

      // Records Inserting

      String filmFilling =
          "insert into FILM (ID,TITLE) values (" + "('F001','The Wolf Of Wall Street')," + "('F002','Shutter Island'),"
              + "('F003','The Departed'))";
      st.execute(filmFilling);

      String actorFilling =
          "insert into ACTOR (ID,NAME,SURNAME) values (" + "('A001','Leonardo','Di Caprio')," + "('A002','Matthew', 'McConaughey'),"
              + "('A003','Ben','Kingsley')," + "('A004','Mark','Ruffalo')," + "('A005','Jack','Nicholson'),"
              + "('A006','Matt','Damon'))";
      st.execute(actorFilling);

      this.naiveAggregationImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(9, orientGraph.countClass("V"));
      assertEquals(6, orientGraph.countClass("Actor"));
      assertEquals(3, orientGraph.countClass("Film"));

      // edges check

      assertEquals(0, orientGraph.countClass("E"));

      this.context.closeOrientDBInstance();


      /*
       * Adding a join table with records (aggregation strategy).
       */

      String film2actorTableBuilding = "create memory table FILM_ACTOR (FILM_ID varchar(256) not null, ACTOR_ID  varchar(256),"
          + " primary key (FILM_ID,ACTOR_ID), foreign key (FILM_ID) references FILM(ID), foreign key (ACTOR_ID) references ACTOR(ID))";
      st.execute(film2actorTableBuilding);

      String film2actorFilling =
          "insert into FILM_ACTOR (FILM_ID,ACTOR_ID) values (" + "('F001','A001')," + "('F001','A002')," + "('F002','A001'),"
              + "('F002','A003')," + "('F002','A004')," + "('F003','A001')," + "('F003','A005')," + "('F003','A006'))";
      st.execute(film2actorFilling);

      this.naiveAggregationImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(9, orientGraph.countClass("V"));
      assertEquals(6, orientGraph.countClass("Actor"));
      assertEquals(3, orientGraph.countClass("Film"));

      // edges check

      assertEquals(8, orientGraph.countClass("E"));
      assertEquals(8, orientGraph.countClass("FilmActor"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test

  /*
   * Update of n records of a table.
   */

  public void contentModificationTest5() {

    Connection connection = null;
    Statement st = null;
    ODatabaseDocument orientGraph = null;

    try {

      Class.forName(this.driver);
      connection = DriverManager.getConnection(this.jurl, this.username, this.password);

      // Tables Building

      String actorTableBuilding = "create memory table ACTOR (ID varchar(256) not null, NAME  varchar(256),"
          + " SURNAME varchar(256) not null, birthday varchar(256), primary key (ID))";
      st = connection.createStatement();
      st.execute(actorTableBuilding);

      // Records Inserting

      String actorFilling =
          "insert into ACTOR (ID,NAME,SURNAME) values (" + "('A001','John','Unaltravolta')," + "('A002','Samuel','Lì Clacson'),"
              + "('A003','Bruce','Willis')," + "('A004','Leonardo','Di Caprio')," + "('A005','Ben','Kingsley'),"
              + "('A006','Mark','Ruffalo')," + "('A007','Jack','Nicholson')," + "('A008','Matto','Demone'))";
      st.execute(actorFilling);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */


      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(8, orientGraph.countClass("V"));
      assertEquals(8, orientGraph.countClass("Actor"));

      OResultSet result = orientGraph.command(this.getElementsFromClassQuery, "Actor");
      OVertex currentVertex = result.next().getVertex().get();
      assertEquals("John", currentVertex.getProperty("name"));
      assertEquals("Unaltravolta", currentVertex.getProperty("surname"));
      currentVertex = result.next().getVertex().get();
      assertEquals("Samuel", currentVertex.getProperty("name"));
      assertEquals("Lì Clacson", currentVertex.getProperty("surname"));
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      assertEquals("Matto", currentVertex.getProperty("name"));
      assertEquals("Demone", currentVertex.getProperty("surname"));
      assertFalse(result.hasNext());

      // edges check

      assertEquals(0, orientGraph.countClass("E"));

      this.context.closeOrientDBInstance();


      /*
       * Update of n records of a table.
       */

      String update = "update ACTOR set surname='Travolta' where id='A001'";
      st.executeQuery(update);
      update = "update ACTOR set surname='Lee Jackson' where id='A002'";
      st.executeQuery(update);
      update = "update ACTOR set name='Matt', surname='Damon' where id='A008'";
      st.executeQuery(update);

      this.naiveImportStrategy
          .executeStrategy(this.sourceDBInfo, this.outOrientGraphUri, "basicDBMapper", null, "java", null, null, null);


      /*
       *  Testing built OrientDB
       */

      this.context.initOrientDBInstance(outOrientGraphUri);
      orientGraph = this.context.getOrientDBInstance().open(this.dbName,"admin","admin");

      // vertices check

      assertEquals(8, orientGraph.countClass("V"));
      assertEquals(8, orientGraph.countClass("Actor"));

      result = orientGraph.command(this.getElementsFromClassQuery, "Actor");
      currentVertex = result.next().getVertex().get();
      assertEquals("John", currentVertex.getProperty("name"));
      assertEquals("Travolta", currentVertex.getProperty("surname"));
      currentVertex = result.next().getVertex().get();
      assertEquals("Samuel", currentVertex.getProperty("name"));
      assertEquals("Lee Jackson", currentVertex.getProperty("surname"));
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      currentVertex = result.next().getVertex().get();
      assertEquals("Matt", currentVertex.getProperty("name"));
      assertEquals("Damon", currentVertex.getProperty("surname"));
      assertFalse(result.hasNext());

      // edges check

      assertEquals(0, orientGraph.countClass("E"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      try {

        // Dropping Source DB Schema and OrientGraph
        String dbDropping = "drop schema public cascade";
        st.execute(dbDropping);
        connection.close();
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

}