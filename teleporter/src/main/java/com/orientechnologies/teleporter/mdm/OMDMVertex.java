package com.orientechnologies.teleporter.mdm;

import com.orientechnologies.common.collection.OMultiCollectionIterator;
import com.orientechnologies.common.util.OPair;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.teleporter.configuration.api.OConfiguredEdgeClass;
import com.orientechnologies.teleporter.configuration.api.OEdgeMappingInformation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import java.util.ArrayList;
import java.util.List;

/**
 * Orient Graph Vertex extension to execute a run-time join to retrieve the edges.
 *
 * @author Luca Garulli
 */
public class OMDMVertex extends OrientVertex {
  public OMDMVertex() {
  }

  public OMDMVertex(final OrientBaseGraph graph, final String className, final Object... fields) {
    super(graph, className, fields);
  }

  public OMDMVertex(final OrientBaseGraph graph, final OIdentifiable record) {
    super(graph, record);
  }

  @Override
  public Iterable<Vertex> getVertices(final Direction iDirection, final String... iLabels) {
    Iterable<Vertex> result = null;

    if (iLabels != null && iLabels.length > 0) {
      result = getVerticesByLabel(iDirection, iLabels[0]);
    } else {
      final ODocument doc = getRecord();
      final String[] fieldNames = doc.fieldNames();

      for (String fieldName : fieldNames) {
        final OPair<Direction, String> connection = getConnection(iDirection, fieldName, iLabels);
        if (connection == null)
          // SKIP THIS FIELD
          continue;

        final Object fieldValue = doc.rawField(fieldName);
        if (fieldValue != null) {
          result = getVerticesByLabel(iDirection, connection.getValue());
          if (result != null)
            break;
        }
      }
    }

    if (result != null)
      return result;

    return super.getVertices(iDirection, iLabels);
  }

  @Override
  public Iterable<Edge> getEdges(final Direction iDirection, final String... iLabels) {
    Iterable<Edge> result = null;

    if (iLabels != null && iLabels.length > 0) {
      result = getEdgesByLabel(iDirection, iLabels[0]);
    } else {
      final OMDMGraphNoTx g = (OMDMGraphNoTx) getGraph();
      for( OConfiguredEdgeClass cls : g.getConfiguration().getEdgeClasses(g.getRawGraph().getName()) ){
        result = getEdgesByLabel(iDirection, cls.getName());
        if (result != null)
          break;
      }

    }

    if (result != null)
      return result;

    return super.getEdges(iDirection, iLabels);
  }

  private Iterable<Vertex> getVerticesByLabel(Direction iDirection, final String label) {
    final OMDMGraphNoTx g = (OMDMGraphNoTx) getGraph();
    final OConfiguredEdgeClass cls = g.getConfiguration().getEdgeClass(g.getRawGraph().getName(), label);
    if (cls != null && cls.isLogical()) {

      for (OEdgeMappingInformation m : cls.getMappings()) {
        String[] properties;
        Object[] joinValues;

        OMultiCollectionIterator result = new OMultiCollectionIterator();

        if(iDirection==Direction.OUT || iDirection==Direction.BOTH){
          String clazz = m.getToClass();
          properties = m.getToProperties();
          joinValues = new Object[properties.length];
          final StringBuilder sqlTo = new StringBuilder("select from " + clazz + " where ");
          for (int i = 0; i < properties.length; ++i) {
            final String p = properties[i];
            joinValues[i] = getProperty(p);

            if (i > 0)
              sqlTo.append(" and ");

            sqlTo.append(p + " = ?");
          }

          Iterable iterable = g.command(new OCommandSQL(sqlTo.toString())).execute(joinValues);
          result.add(iterable);

        } else if(iDirection==Direction.IN || iDirection==Direction.BOTH){
          String clazz = m.getFromClass();
          properties = m.getFromProperties();
          joinValues = new Object[properties.length];
          final StringBuilder sqlTo = new StringBuilder("select from " +clazz+ " where ");
          for (int i = 0; i < properties.length; ++i) {
            final String p = properties[i];
            joinValues[i] = getProperty(p);

            if (i > 0)
              sqlTo.append(" and ");

            sqlTo.append(p + " = ?");
          }

          Iterable iterable = g.command(new OCommandSQL(sqlTo.toString())).execute(joinValues);
          result.add(iterable);
        }
        return result;
      }
    }
    return null;
  }

  private Iterable<Edge> getEdgesByLabel(Direction iDirection, final String label) {
    final OMDMGraphNoTx g = (OMDMGraphNoTx) getGraph();
    final OConfiguredEdgeClass cls = g.getConfiguration().getEdgeClass(g.getRawGraph().getName(), label);
    if (cls != null && cls.isLogical()) {

      final List<Edge> result = new ArrayList<Edge>();

      for (OEdgeMappingInformation m : cls.getMappings()) {
        String[] properties;
        Object[] joinValues;

        OMultiCollectionIterator<OrientVertex> resultVertices = new OMultiCollectionIterator();

        if(iDirection==Direction.OUT || iDirection==Direction.BOTH){
          String clazz = m.getToClass();
          properties = m.getToProperties();
          joinValues = new Object[properties.length];
          final StringBuilder sqlTo = new StringBuilder("select from " + clazz + " where ");
          for (int i = 0; i < properties.length; ++i) {
            final String p = properties[i];
            joinValues[i] = getProperty(p);

            if (i > 0)
              sqlTo.append(" and ");

            sqlTo.append(p + " = ?");
          }

          Iterable<OrientVertex> iterable = g.command(new OCommandSQL(sqlTo.toString())).execute(joinValues);
          resultVertices.add(iterable);

          for (OrientVertex v : resultVertices) {
            result.add(new OMDMEdge(g, getIdentity(), v.getIdentity(), label));
          }

        } else if(iDirection==Direction.IN || iDirection==Direction.BOTH){
          String clazz = m.getFromClass();
          properties = m.getFromProperties();
          joinValues = new Object[properties.length];
          final StringBuilder sqlTo = new StringBuilder("select from " +clazz+ " where ");
          for (int i = 0; i < properties.length; ++i) {
            final String p = properties[i];
            joinValues[i] = getProperty(p);

            if (i > 0)
              sqlTo.append(" and ");

            sqlTo.append(p + " = ?");
          }

          Iterable<OrientVertex> iterable = g.command(new OCommandSQL(sqlTo.toString())).execute(joinValues);
          resultVertices.add(iterable);

          for (OrientVertex v : resultVertices) {
            result.add(new OMDMEdge(g, v.getIdentity(), getIdentity(), label));
          }
        }

      }
      return result;
    }
    return null;
  }

  /*private Iterable<Edge> getEdgesByLabel(final String label) {
    final OMDMGraphNoTx g = (OMDMGraphNoTx) getGraph();
    final OConfiguredEdgeClass cls = g.getConfiguration().getEdgeClass(g.getRawGraph().getName(), label);
    if (cls != null && cls.isLogical()) {

      final List<Edge> result = new ArrayList<Edge>();

      for (OEdgeMappingInformation m : cls.getMappings()) {
        final String[] properties = m.getFromProperties();
        final Object[] joinValues = new Object[properties.length];

        final StringBuilder sqlTo = new StringBuilder("select from " + m.getToClass() + " where ");
        for (int i = 0; i < properties.length; ++i) {
          final String p = properties[i];
          joinValues[i] = getProperty(p);
          if (i > 0)
            sqlTo.append(" and ");

          sqlTo.append(m.getToProperties()[i] + " = ?");
        }

        final Iterable<OrientVertex> resultVertices = g.command(new OCommandSQL(sqlTo.toString())).execute(joinValues);
        for (OrientVertex v : resultVertices) {
          result.add(new OMDMEdge(g, getIdentity(), v.getIdentity(), label));
        }
      }
      return result;
    }
    return null;
  }*/

}
