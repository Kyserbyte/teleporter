/*
 * Copyright 2015 Orient Technologies LTD (info--at--orientechnologies.com)
 * All Rights Reserved. Commercial License.
 * 
 * NOTICE:  All information contained herein is, and remains the property of
 * Orient Technologies LTD and its suppliers, if any.  The intellectual and
 * technical concepts contained herein are proprietary to
 * Orient Technologies LTD and its suppliers and may be covered by United
 * Kingdom and Foreign Patents, patents in process, and are protected by trade
 * secret or copyright law.
 * 
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Orient Technologies LTD.
 * 
 * For more information: http://www.orientechnologies.com
 */

package com.orientechnologies.teleporter.context;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.orientechnologies.teleporter.ui.OStatisticsListener;

/**
 * Collects and updates statistics about the Drakkar execution state.
 * It identifies and monitors 4 step in the global execution:
 * 1. Source DB Schema building
 * 2. Graph Model building
 * 3. OrientDB Schema writing
 * 4. OrientDB importing
 * 
 * @author Gabriele Ponzi
 * @email  <gabriele.ponzi--at--gmail.com>
 *
 */

public class OTeleporterStatistics {

  // indicates the running step, -1 if no step are running
  public volatile int runningStepNumber;

  // Source DB Schema building statistics
  public volatile int totalNumberOfEntities;
  public volatile int builtEntities;
  public volatile int doneEntity4Relationship;
  public volatile int detectedRelationships;
  public volatile Date startWork1Time;

  // Graph Model building statistics
  public volatile int totalNumberOfModelVertices;
  public volatile int builtModelVertexTypes;
  public volatile int totalNumberOfRelationships;
  public volatile int analizedRelationships;
  public volatile int builtModelEdgeTypes;
  public volatile Date startWork2Time;

  // OrientDB Schema writing statistics
  public volatile int totalNumberOfVertexType;
  public volatile int wroteVertexType;
  public volatile int totalNumberOfEdgeType;
  public volatile int wroteEdgeType;
  public volatile int totalNumberOfIndices;
  public volatile int wroteIndexes;
  public volatile Date startWork3Time;

  // OrientDB importing
  public volatile int totalNumberOfRecords;
  public volatile int analyzedRecords;
  public volatile int orientAddedVertices;
  public volatile int orientUpdatedVertices;
  public volatile int orientAddedEdges;
  public volatile Date startWork4Time;

  // Warnings Messages
  public volatile Set<String> warningMessages;

  // Listeners
  private volatile List<OStatisticsListener> listeners;

  public OTeleporterStatistics() {
    this.init();
    this.warningMessages = new HashSet<String>();
    this.listeners = new ArrayList<OStatisticsListener>();
  }


  private void init() {

    this.runningStepNumber = -1;

    this.totalNumberOfEntities = 0;
    this.builtEntities = 0;
    this.doneEntity4Relationship = 0;
    this.detectedRelationships = 0;

    this.totalNumberOfModelVertices = 0;
    this.builtModelVertexTypes = 0;
    this.totalNumberOfRelationships = 0;
    this.analizedRelationships = 0;
    this.builtModelEdgeTypes = 0;

    this.totalNumberOfVertexType = 0;
    this.wroteVertexType = 0;
    this.totalNumberOfEdgeType = 0;
    this.wroteEdgeType = 0;
    this.totalNumberOfIndices = 0;
    this.wroteIndexes = 0;

    this.totalNumberOfRecords = 0;
    this.analyzedRecords = 0;
    this.orientAddedVertices = 0;
    this.orientAddedEdges = 0;

  }

  public void reset() {
    this.init();
  }

  /*
   * Publisher-Subscribers
   */

  public void registerListener(OStatisticsListener listener) {
    this.listeners.add(listener);
  }

  public void notifyListeners() {
    for(OStatisticsListener listener: this.listeners) {
      listener.updateOnEvent(this);
    }
  }


  /*
   *  toString methods
   */

  public String sourceDbSchemaBuildingProgress() {
    String s ="Source DB Schema\n";
    s += "Entities: " + this.builtEntities;
    s += "\nRelationships: " + this.detectedRelationships;
    return s;
  }

  public String graphModelBuildingProgress() {
    String s ="Graph Model Building\n";
    s += "Built Model Vertices: " + this.builtModelVertexTypes;
    s += "\nBuilt Model Edges: " + this.builtModelEdgeTypes;
    return s;
  }

  public String orientSchemaWritingProgress() {
    String s ="OrientDB Schema\n";
    s += "Vertex Type: " + this.wroteVertexType;
    s += "\nEdge Type: " + this.wroteEdgeType;
    s += "\nIndexes: " + this.wroteIndexes;
    return s;
  }

  public String importingProgress() {
    String s ="OrientDB Importing\n";
    s += "Analyzed Records: " + this.analyzedRecords + "/" + this.totalNumberOfRecords;
    s += "\nAdded Vertices on OrientDB: " + this.orientAddedVertices;
    s += "\nUpdated Vertices on OrientDB: " + this.orientUpdatedVertices;
    s += "\nAdded Edges on OrientDB: " + this.orientAddedEdges;

    return s;
  }

  public String toString() {
    String s = "\n\nSUMMARY\n\n";
    s += this.sourceDbSchemaBuildingProgress() + "\n\n" + this.orientSchemaWritingProgress() + "\n\n" + this.importingProgress() + "\n\n";

    if(this.warningMessages.size() > 0) {
      s += "Warning Messages:\n";
      for(String message: this.warningMessages) {
        s += message + "\n";
      }
    }
    return s;
  }



}