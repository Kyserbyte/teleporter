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

package com.orientechnologies.teleporter.importengine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.orientechnologies.teleporter.context.OTeleporterContext;
import com.orientechnologies.teleporter.context.OTeleporterStatistics;
import com.orientechnologies.teleporter.mapper.OAggregatorEdge;
import com.orientechnologies.teleporter.model.dbschema.OAttribute;
import com.orientechnologies.teleporter.model.dbschema.OEntity;
import com.orientechnologies.teleporter.model.dbschema.ORelationship;
import com.orientechnologies.teleporter.model.graphmodel.OModelProperty;
import com.orientechnologies.teleporter.model.graphmodel.OVertexType;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * Executes the necessary operations of insert and upsert for the destination Orient DB populating.
 * 
 * @author Gabriele Ponzi
 * @email  <gabriele.ponzi--at--gmail.com>
 *
 */

public class OGraphDBCommandEngine {


	public OGraphDBCommandEngine() {}


	/**
	 * Return true if the record is "full-imported" in OrientDB: the correspondent vertex is visited (all properties are set).
	 * @param record
	 * @throws SQLException 
	 */
	public boolean alreadyFullImportedInOrient(OrientBaseGraph orientGraph, ResultSet record, OVertexType vertexType, Set<String> propertiesOfIndex, OTeleporterContext context) throws SQLException {

		try {

			boolean toResolveNames = false;

			// building keys and values for the lookup

			if(propertiesOfIndex == null) {
				toResolveNames = true;
				propertiesOfIndex = new LinkedHashSet<String>();

				for(OModelProperty currentProperty: vertexType.getAllProperties()) {
					// only attribute coming from the primary key are given
					if(currentProperty.isFromPrimaryKey())
						propertiesOfIndex.add(currentProperty.getName());
				}
			}

			String[] propertyOfKey = new String[propertiesOfIndex.size()];
			String[] valueOfKey = new String[propertiesOfIndex.size()];

			int cont = 0;
			for(String property: propertiesOfIndex) {
				propertyOfKey[cont] = property;
				if(toResolveNames)
					valueOfKey[cont] = record.getString(context.getNameResolver().reverseTransformation(property));
				else
					valueOfKey[cont] = record.getString(property);

				cont++;
			}

			String s = "Keys and values in the lookup (upsertVisitedVertex):\t";
			for(int i=0; i<propertyOfKey.length;i++) {
				s += propertyOfKey[i] + ":" + valueOfKey[i] + ",";
			}
			s = s.substring(0, s.length()-1);
			context.getOutputManager().debug("\n" + s + "\n");

			// lookup
			OrientVertex vertex = this.getVertexByIndexedKey(orientGraph, propertyOfKey, valueOfKey, vertexType.getName());

			if(vertex != null && vertexType.getAllProperties().size() <= vertex.getPropertyKeys().size()) // there aren't properties to add into the vertex (<=)
				return true;

		} catch(Exception e) {
			if(e.getMessage() != null)
				context.getOutputManager().error(e.getClass().getName() + " - " + e.getMessage());
			else
				context.getOutputManager().error(e.getClass().getName());

			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s = writer.toString();
			context.getOutputManager().debug("\n" + s + "\n");
			if(orientGraph != null)
				orientGraph.shutdown();
			System.exit(0);
		}
		return false;
	}



	/**
	 * The method perform on the passed OrientBaseGraph a lookup for a OrientVertex starting from a record and from a vertex type.
	 * It return the vertex if present, null if not present. 
	 * 
	 * @param orientGraph
	 * @param keys
	 * @param values
	 * @param ClassName
	 * @return
	 */
	public OrientVertex getVertexByIndexedKey(OrientBaseGraph orientGraph, String[] keys, String[] values, String vertexClassName) {

		OrientVertex vertex = null;

		Iterator<Vertex> iterator = orientGraph.getVertices(vertexClassName, keys, values).iterator();

		if(iterator.hasNext())
			vertex = (OrientVertex) iterator.next();

		return vertex;
	}


	/**
	 * @param record
	 */
	public Vertex upsertVisitedVertex(OrientBaseGraph orientGraph, ResultSet record, OVertexType vertexType, Set<String> propertiesOfIndex, OTeleporterContext context) {

		OrientVertex vertex = null;
		String[] propertyOfKey = null;
		String[] valueOfKey = null;

		try {

			Map<String,Object> properties = new LinkedHashMap<String,Object>();

			OTeleporterStatistics statistics = context.getStatistics();

			boolean toResolveNames = false;

			// building keys and values for the lookup

			if(propertiesOfIndex == null) {
				toResolveNames = true;
				propertiesOfIndex = new LinkedHashSet<String>();

				for(OModelProperty currentProperty: vertexType.getAllProperties()) {
					// only attribute coming from the primary key are given
					if(currentProperty.isFromPrimaryKey())
						propertiesOfIndex.add(currentProperty.getName());
				}
			}

			propertyOfKey = new String[propertiesOfIndex.size()];
			valueOfKey = new String[propertiesOfIndex.size()];

			int cont = 0;
			for(String property: propertiesOfIndex) {
				propertyOfKey[cont] = property;
				if(toResolveNames)
					valueOfKey[cont] = record.getString(context.getNameResolver().reverseTransformation(property));
				else
					valueOfKey[cont] = record.getString(property);

				cont++;
			}

			String s = "Keys and values in the lookup (upsertVisitedVertex):\t";
			for(int i=0; i<propertyOfKey.length;i++) {
				s += propertyOfKey[i] + ":" + valueOfKey[i] + ",";
			}
			s = s.substring(0, s.length()-1);
			context.getOutputManager().debug("\n" + s + "\n");

			// lookup
			vertex = this.getVertexByIndexedKey(orientGraph, propertyOfKey, valueOfKey, vertexType.getName());

			// setting properties to the vertex
			String currentAttributeValue = null;
			Date currentDateValue = null;
			byte[] currentBinaryValue = null;
			String currentPropertyType;


			// extraction of inherited and not inherited properties from the record (through "getAllProperties()" method)
			for(OModelProperty currentProperty : vertexType.getAllProperties()) {

				currentPropertyType = context.getDataTypeHandler().resolveType(currentProperty.getPropertyType().toLowerCase(Locale.ENGLISH),context).toString();

				try {

					if(currentPropertyType.equals("DATE")) {
						currentDateValue = record.getDate(context.getNameResolver().reverseTransformation(currentProperty.getName()));
						properties.put(currentProperty.getName(), currentDateValue);
					}

					else if(currentPropertyType.equals("DATETIME")) {
						currentDateValue = record.getTimestamp(context.getNameResolver().reverseTransformation(currentProperty.getName()));
						properties.put(currentProperty.getName(), currentDateValue);
					}

					else if(currentPropertyType.equals("BINARY")) {
						currentBinaryValue = record.getBytes(context.getNameResolver().reverseTransformation(currentProperty.getName()));
						properties.put(currentProperty.getName(), currentBinaryValue);
					}

					else if(currentPropertyType.equals("BOOLEAN")) {
						currentAttributeValue = record.getString(context.getNameResolver().reverseTransformation(currentProperty.getName()));
						switch(currentAttributeValue) {

						case "t": properties.put(currentProperty.getName(), "true");
						break;
						case "f": properties.put(currentProperty.getName(), "false");
						break;
						default: break;
						}
					}

					else {
						currentAttributeValue = record.getString(context.getNameResolver().reverseTransformation(currentProperty.getName()));
						properties.put(currentProperty.getName(), currentAttributeValue);
					}

				} catch(Exception e) {
					if(e.getMessage() != null)
						context.getOutputManager().error(e.getClass().getName() + " - " + e.getMessage());
					else
						context.getOutputManager().error(e.getClass().getName());

					context.getOutputManager().debug("Mismatch between 'parent-table' attributes and 'child-table' attributes, check the schema of the tables involved in inheritance relationships.");
					Writer writer = new StringWriter();
					e.printStackTrace(new PrintWriter(writer));
					String s1 = writer.toString();
					context.getOutputManager().debug(s1);
				}

			}

			if(vertex == null) {
				String classAndClusterName = vertexType.getName(); 
				vertex = orientGraph.addVertex("class:"+classAndClusterName, properties);
				statistics.orientAddedVertices++;
				context.getOutputManager().debug("\nLoaded properties: " + properties.toString());
				context.getOutputManager().debug("\nNew vertex inserted (all props setted): %s\n", vertex.toString());
			}
			else {

				// discerning between a reached-vertex updating (only original primary key's properties are present) and a full-vertex updating
				boolean justReachedVertex = true;

				for(String property: vertex.getPropertyKeys()) {
					if(!this.containsProperty(propertiesOfIndex,property)) {
						justReachedVertex = false;
						break;
					}
				}

				// updating a reached-vertex (only original primary key's properties are present)
				if(justReachedVertex) {

					// setting new properties and save
					vertex.setProperties(properties);
					vertex.save();
					statistics.orientAddedVertices++;
					context.getOutputManager().debug("\nLoaded properties: " + properties.toString());
					context.getOutputManager().debug("\nNew vertex inserted (all props setted): %s\n", vertex.toString());
				}

				// updating a full-vertex
				else {

					// comparing old version of vertex with the new one: if the two versions are equals no rewriting is performed

					boolean equalVersions = true;
					boolean equalProperties = true;

					if(vertex.getPropertyKeys().size() == properties.size()) {

						// comparing properties
						for(String propertyName: vertex.getPropertyKeys()) {
							if(!properties.keySet().contains(propertyName)) {
								equalProperties = false;
								equalVersions = false;
								break;
							}
						}

						if(equalProperties) {
							// comparing values of the properties
							for(String propertyName: vertex.getPropertyKeys()) {
								if(!(vertex.getProperty(propertyName) == null && properties.get(propertyName) == null) ) {
									currentPropertyType = context.getDataTypeHandler().resolveType(vertexType.getPropertyByName(propertyName).getPropertyType().toLowerCase(Locale.ENGLISH),context).toString();
									if(!this.areEquals(vertex.getProperty(propertyName),properties.get(propertyName), currentPropertyType)) {
										equalVersions = false;
										break;
									} 
								}
							}
						}
						else {
							equalProperties = false;
							equalVersions = false;
						}

						if(!equalVersions) {
							// removing old eventual properties
							for(String propertyKey: vertex.getPropertyKeys()) {
								vertex.removeProperty(propertyKey);
							}

							// setting new properties and save
							vertex.setProperties(properties);
							vertex.save();
							statistics.orientUpdatedVertices++;
							context.getOutputManager().debug("\nLoaded properties: " + properties.toString());
							context.getOutputManager().debug("\nNew vertex inserted (all props setted): %s\n", vertex.toString());
						}
					}
				}
			} 
		} catch(Exception e) {
			if(e.getMessage() != null)
				context.getOutputManager().error(e.getClass().getName() + " - " + e.getMessage());
			else
				context.getOutputManager().error(e.getClass().getName());

			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s2 = writer.toString();
			context.getOutputManager().debug(s2);
			if(orientGraph != null)
				orientGraph.shutdown();
			System.exit(0);
		}

		return vertex;
	}



	/**
	 * @param propertiesOfIndex
	 * @param property
	 * @return
	 */
	private boolean containsProperty(Set<String> propertiesOfIndex, String property) {

		for(String currentProp: propertiesOfIndex) {
			if(currentProp.equalsIgnoreCase(property))
				return true;
		}

		return false;
	}


	/**
	 * @param currentPropertyType 
	 * @param property
	 * @param string
	 * @return
	 */
	private boolean areEquals(Object oldProperty, Object newProperty, String currentPropertyType) {

		if(oldProperty != null && newProperty != null) {

			if(currentPropertyType.equals("BOOLEAN")) {

				if (oldProperty.toString().equalsIgnoreCase(newProperty.toString()))
					return true;

				else if(oldProperty.toString().equalsIgnoreCase("t") && newProperty.toString().equalsIgnoreCase("true") 
						|| oldProperty.toString().equalsIgnoreCase("f") && newProperty.toString().equalsIgnoreCase("false"))
					return true;

				else
					return false;
			}

			else if(currentPropertyType.equals("DATE")) {
				return oldProperty.equals(newProperty);
			}

			else if(currentPropertyType.equals("DATETIME")) {
				// oldProperty : Date
				// newProperty : Timestamp
				Date oldPropertyDate = (Date) oldProperty;  
				// new variable to compare dates
				Date newPropertyDate = new Date(((Timestamp)newProperty).getTime());

				return oldPropertyDate.equals(newPropertyDate);
			}

			else if(currentPropertyType.equals("BINARY")) {
				byte[] oldPropertyBinary = (byte[]) oldProperty;
				byte[] newPropertyBinary = (byte[]) newProperty;
				return Arrays.equals(oldPropertyBinary, newPropertyBinary);
			}

			else {
				return oldProperty.toString().equals(newProperty.toString());
			}
		}

		else if(oldProperty == null && newProperty == null)
			return true;

		else
			return false;
	}


	/**
	 * @param ResultSet foreignRecord: the record correspondent to the current-out-vertex
	 * @param ORelationship relation: the relation between two entities
	 * @param OrientVertex currentOutVertex: the current-out-vertex
	 * @param OVertexType currentInVertexType: the type correspondent to the current-in-vertex
	 * @param String edgeType: type of the OEdgeType present between the two OVertexType, used as label during the insert of the edge in the graph
	 * 
	 * The method executes insert on reached vertex:
	 * - if the vertex is not already reached, it's inserted in the graph and an edge between the out-visited-vertex and the in-reached-vertex is added
	 * - if the vertex is already present in the graph no update is performed, neither on reached-vertex neither on the relative edge
	 * @throws SQLException 
	 */

	public OrientVertex upsertReachedVertexWithEdge(OrientBaseGraph orientGraph, ResultSet foreignRecord, ORelationship relation, OrientVertex currentOutVertex, OVertexType currentInVertexType,
			String edgeType, OTeleporterContext context) throws SQLException {

		OrientVertex currentInVertex = null;

		try {

			// building keys and values for the lookup 

			String[] propertyOfKey = new String[relation.getForeignKey().getInvolvedAttributes().size()];
			String[] valueOfKey = new String[relation.getForeignKey().getInvolvedAttributes().size()];

			int index = 0;
			for(OAttribute foreignAttribute: relation.getForeignKey().getInvolvedAttributes())  {
				propertyOfKey[index] = context.getNameResolver().resolveVertexProperty(relation.getPrimaryKey().getInvolvedAttributes().get(index).getName());
				valueOfKey[index] = foreignRecord.getString((foreignAttribute.getName()));
				index++;
			}

			String s = "Keys and values in the lookup (upsertReachedVertex):\t";
			for(int i=0; i<propertyOfKey.length;i++) {
				s += propertyOfKey[i] + ":" + valueOfKey[i] + ",";
			}
			s = s.substring(0, s.length()-1);
			context.getOutputManager().debug("\n" + s + "\n");

			// new vertex is added only if all the values in the foreign key are different from null
			boolean ok = true;

			for(int i=0; i<valueOfKey.length; i++) {
				if(valueOfKey[i] == null) {
					ok = false;
					break;
				}
			}

			// all values are different from null, thus vertex is searched in the graph and in case is added if not found.
			if(ok) {

				currentInVertex = this.getVertexByIndexedKey(orientGraph, propertyOfKey, valueOfKey, currentInVertexType.getName());

				/*
				 *  if the vertex is not already present in the graph it's built, set and inserted to the graph,
				 *  then the edge beetwen the current-out-vertex and the current-in-vertex is added 
				 */
				if(currentInVertex == null) {

					Map<String,String> partialProperties = new LinkedHashMap<String,String>();

					// for each attribute in the foreign key belonging to the relationship, attribute name and correspondent value are added to a 'properties map'
					for(int i=0; i<propertyOfKey.length; i++) {                
						partialProperties.put(propertyOfKey[i], valueOfKey[i]);
					}

					context.getOutputManager().debug("\nNEW Reached vertex (id:value) --> %s:%s", Arrays.toString(propertyOfKey), Arrays.toString(valueOfKey));
					String classAndClusterName = currentInVertexType.getName(); 
					currentInVertex = orientGraph.addVertex("class:"+classAndClusterName, partialProperties);
					context.getOutputManager().debug("\nNew vertex inserted (only pk props setted): %s\n", currentInVertex.toString());

				}

				else {
					context.getOutputManager().debug("\nNOT NEW Reached vertex, vertex %s:%s already present in the Orient Graph.\n", Arrays.toString(propertyOfKey), Arrays.toString(valueOfKey));
				}

				// upsert of the edge between the currentOutVertex and the currentInVertex
				this.upsertEdge(orientGraph, currentOutVertex, currentInVertex, edgeType, context);
			}

		} catch(Exception e) {
			if(e.getMessage() != null)
				context.getOutputManager().error(e.getClass().getName() + " - " + e.getMessage());
			else
				context.getOutputManager().error(e.getClass().getName());

			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s = writer.toString();
			context.getOutputManager().debug(s);
			if(orientGraph != null)
				orientGraph.shutdown();
			System.exit(0);
		}

		return currentInVertex;
	}

	public void upsertEdge(OrientBaseGraph orientGraph, OrientVertex currentOutVertex, OrientVertex currentInVertex, String edgeType, OTeleporterContext context) {

		try {

			boolean edgeAlreadyPresent = false;
			Iterator<Edge> it = currentOutVertex.getEdges(Direction.OUT, edgeType).iterator();
			Edge currentEdge;

			OTeleporterStatistics statistics = context.getStatistics();

			if(it.hasNext()) {
				while(it.hasNext()) {
					currentEdge = it.next();

					if(((OrientVertex)currentEdge.getVertex(Direction.IN)).getId().equals(currentInVertex.getId())) {
						edgeAlreadyPresent = true;
						break;
					}
				}
				if(edgeAlreadyPresent) {
					context.getOutputManager().debug("\nEdge beetween '%s' and '%s' already present.\n", currentOutVertex.toString(), currentInVertex.toString());
				}
				else {
					OrientEdge edge = orientGraph.addEdge(null, currentOutVertex, currentInVertex, edgeType);
					edge.save();
					statistics.orientAddedEdges++;
					context.getOutputManager().debug("\nNew edge inserted: %s\n", edge.toString());
				}
			}
			else {
				OrientEdge edge = orientGraph.addEdge(null, currentOutVertex, currentInVertex, edgeType);
				edge.save();
				statistics.orientAddedEdges++;
				context.getOutputManager().debug("\nNew edge inserted: %s\n", edge.toString());
			}
		} catch(Exception e) {
			if(e.getMessage() != null)
				context.getOutputManager().error(e.getClass().getName() + " - " + e.getMessage());
			else
				context.getOutputManager().error(e.getClass().getName());

			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s = writer.toString();
			context.getOutputManager().debug("\n" + s + "\n");
			if(orientGraph != null)
				orientGraph.shutdown();
			System.exit(0);
		}
	}

	public void upsertAggregatorEdge(OrientBaseGraph orientGraph, ResultSet jointTableRecord, OEntity joinTable, OAggregatorEdge aggregatorEdge, OTeleporterContext context) throws SQLException {

		try {

			Iterator<ORelationship> it = joinTable.getRelationships().iterator();
			ORelationship relationship1 = it.next();
			ORelationship relationship2 = it.next();


			// Building keys and values for out-vertex lookup

			String[] keysOutVertex = new String[relationship1.getForeignKey().getInvolvedAttributes().size()];
			String[] valuesOutVertex = new String[relationship1.getForeignKey().getInvolvedAttributes().size()];

			int index = 0;
			for(OAttribute foreignKeyAttribute: relationship1.getForeignKey().getInvolvedAttributes()) {
				keysOutVertex[index] = context.getNameResolver().resolveVertexProperty(relationship1.getPrimaryKey().getInvolvedAttributes().get(index).getName());
				valuesOutVertex[index] = jointTableRecord.getString(foreignKeyAttribute.getName());
				index++;
			}

			// Building keys and values for in-vertex lookup

			String[] keysInVertex = new String[relationship2.getPrimaryKey().getInvolvedAttributes().size()];
			String[] valuesInVertex = new String[relationship2.getPrimaryKey().getInvolvedAttributes().size()];

			index = 0;
			for(OAttribute foreignKeyAttribute: relationship2.getForeignKey().getInvolvedAttributes()) {
				keysInVertex[index] = context.getNameResolver().resolveVertexProperty(relationship2.getPrimaryKey().getInvolvedAttributes().get(index).getName());
				valuesInVertex[index] = jointTableRecord.getString(foreignKeyAttribute.getName());
				index++;
			}

			OrientVertex currentOutVertex = this.getVertexByIndexedKey(orientGraph, keysOutVertex, valuesOutVertex, aggregatorEdge.getOutVertexClassName());
			OrientVertex currentInVertex = this.getVertexByIndexedKey(orientGraph, keysInVertex, valuesInVertex, aggregatorEdge.getInVertexClassName());

			this.upsertEdge(orientGraph, currentOutVertex, currentInVertex, aggregatorEdge.getEdgeType(), context);

		} catch(Exception e) {
			if(e.getMessage() != null)
				context.getOutputManager().error(e.getClass().getName() + " - " + e.getMessage());
			else
				context.getOutputManager().error(e.getClass().getName());

			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s = writer.toString();
			context.getOutputManager().debug(s);
			if(orientGraph != null )
				orientGraph.shutdown();
			System.exit(0);
		}
	}

}