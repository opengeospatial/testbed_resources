/*
 *    OGC TestBed 19 D001
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T19D001.report;

import static java.lang.Math.*;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * A method for computing distance and azimuth using a map projection.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
final class ProjectionMethod extends Method {
    /**
     * The map projection to be tested.
     */
    private final MathTransform projection;

    /**
     * Creates a new method based on a map projection.
     *
     * @param  name       name of EPSG operation method for the map projection to use.
     * @param  ellipsoid  ellipsoid to use for the map projection.
     * @param  numStdLat  number of standard parallels (0, 1 or 2) used by the map projection.
     * @param  centerY    latitude of standard parallel, in degrees.
     * @throws FactoryException if an error occurred while creating the map projection.
     */
    @SuppressWarnings("fallthrough")
    ProjectionMethod(final String name, final Ellipsoid ellipsoid, final int numStdLat, final double centerY)
            throws FactoryException
    {
        super(name);
        final var factory = new DefaultMathTransformFactory();
        final ParameterValueGroup param = factory.getDefaultParameters(name);
        param.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
        param.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());
        switch (numStdLat) {
            default: throw new AssertionError(numStdLat);
            case 2: param.parameter("Latitude of 2nd standard parallel").setValue(centerY);      // Fall-through
            case 1: param.parameter("Latitude of 1st standard parallel").setValue(centerY);
            case 0: break;
        }
        projection = factory.createParameterizedTransform(param, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void computeErrors(final DirectPosition startPoint, final DirectPosition endPoint,
                              final double distance, final double azimuth) throws TransformException
    {
        final DirectPosition p1 = projection.transform(startPoint, null);
        final DirectPosition p2 = projection.transform(endPoint,   null);
        final double dx = p2.getOrdinate(0) - p1.getOrdinate(0);
        final double dy = p2.getOrdinate(1) - p1.getOrdinate(1);
        addErrors(distance, hypot(dx, dy)  - distance,
                  toDegrees(atan2(dx, dy)) - azimuth);
        /*
         * Above call to `atan2(…)` really needs (x,y) argument order, not (y,x),
         * because the angle needs to be geographic, not an arithmetic angle.
         */
    }
}
