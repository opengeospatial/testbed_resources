/*
 *    OGC TestBed 19 D001
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T19D001.report;

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.GeodeticCalculator;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * Compare the accuracy of different map projection methods for computing distances and azimuths.
 * This method create points in circle around a given position, using geodesic calculator.
 * Then the distance is computed by different methods.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public final class CompareDistanceCalculations {
    /**
     * Test all distances from 1 meter to 1000 kilometers using a logarithmic scale.
     * The result is printed on the standard output stream as rows with the following columns.
     *
     * <ol>
     *   <li>Tested distance (m)</li>
     *   <li>Average error in distances (%)</li>
     *   <li>Standard deviation of distance errors (below)</li>
     *   <li>Standard deviation of distance errors (above)</li>
     *   <li>Average error in azimuths (%)</li>
     *   <li>Standard deviation of azimuths errors (below)</li>
     *   <li>Standard deviation of azimuths errors (above)</li>
     *   <li>Columns 2 to 7 repeated for all others operation methods.</li>
     * </ol>
     *
     * @param  args  ignored.
     * @throws Exception if an error occurred while computing a value.
     */
    public static void main(String[] args) throws Exception {
        var c = new CompareDistanceCalculations(CommonCRS.defaultGeographic(), 0, 45);
        for (double p = 0; p <= 6; p += 0.125) {
            c.test(Math.pow(10, p));
        }
        System.out.println(c);
    }

    /**
     * Coordinate around which to compare distances and azimuths.
     */
    private final double centerX, centerY;

    /**
     * The geodetic calculator to use as a reference.
     */
    private final GeodeticCalculator geodesy;

    /**
     * The methods to use for computing distances and azimuths.
     */
    private final Method[] methods;

    /**
     * The results of measuring error according all methods.
     * Each array has a length of {@value #TUPLE_LENGTH} times the number of methods plus one.
     * The first value is the tested distance.
     * All following values are repetitions of the following tuple:
     *
     * <ul>
     *   <li>Average error in distances (%)</li>
     *   <li>Standard deviation of distance errors (below)</li>
     *   <li>Standard deviation of distance errors (above)</li>
     *   <li>Average error in azimuths (%)</li>
     *   <li>Standard deviation of azimuths errors (below)</li>
     *   <li>Standard deviation of azimuths errors (above)</li>
     * </ul>
     *
     * @see Method#writeErrors(double[], int)
     */
    private final List<double[]> results;

    /**
     * Number of values in a tuple.
     * This is the number of items listed in {@link #results}.
     */
    private static final int TUPLE_LENGTH = 6;

    /**
     * Creates a new accuracy benchmark.
     *
     * @param baseCRS  CRS using the datum on which to compute distances.
     * @param centerX  longitude of the coordinate around which to compare distances and azimuths.
     * @param centerY  latitude  of the coordinate around which to compare distances and azimuths.
     * @throws FactoryException if an error occurred while creating a map projection.
     */
    private CompareDistanceCalculations(final GeographicCRS baseCRS, final double centerX, final double centerY)
            throws FactoryException
    {
        this.centerX = centerX;
        this.centerY = centerY;

        final var ellipsoid = DefaultEllipsoid.castOrCopy(baseCRS.getDatum().getEllipsoid());
        final var authalic  = new GeodesicMethod("Authalic radius", baseCRS, ellipsoid.getAuthalicRadius());
        geodesy = GeodeticCalculator.create(baseCRS);
        methods = new Method[] {
            new GeodesicMethod  ("Consistency check", geodesy),
            new GeodesicMethod  ("Authalic radius without datum shift", authalic), authalic,
            new GeodesicMethod  ("Radius at latitude", baseCRS, ellipsoid.getRadius(centerY)),
            new ProjectionMethod("Popular Visualisation Pseudo Mercator", ellipsoid, 0, centerY),
            new ProjectionMethod("Mercator (variant A)",                  ellipsoid, 0, centerY),
            new ProjectionMethod("Mercator (variant B)",                  ellipsoid, 1, centerY),
            new ProjectionMethod("Lambert Conic Conformal (2SP)",         ellipsoid, 2, centerY)
        };
        results = new ArrayList<>(100);
    }

    /**
     * Tests calculations of all methods for the given distance.
     *
     * @param  distance  the distance to test.
     * @throws TransformException if a method failed to transform a point.
     */
    private void test(final double distance) throws TransformException {
        final DirectPosition center = new DirectPosition2D(centerX, centerY);
        for (double azimuth=-180; azimuth<180; azimuth++) {
            geodesy.setStartPoint(center);
            geodesy.setStartingAzimuth(azimuth);
            geodesy.setGeodesicDistance(distance);
            final DirectPosition startPoint = geodesy.getStartPoint();
            final DirectPosition endPoint   = geodesy.getEndPoint();
            for (final Method method : methods) {
                method.computeErrors(startPoint, endPoint, distance, azimuth);
            }
        }
        final double[] result = new double[methods.length * TUPLE_LENGTH + 1];
        int i = 0;
        result[i++] = distance;
        for (final Method method : methods) {
            i = method.writeErrors(result, i);
        }
        assert i == result.length;
        results.add(result);
    }

    /**
     * Returns the results of comparing distance calculations.
     * The result is provided as a coma-separated document.
     *
     * @return results in a coma-separated document.
     */
    @Override
    public String toString() {
        final String lineSeparator = System.lineSeparator();
        final var buffer = new StringBuilder(1000);
        for (final Method method : methods) {
            buffer.append(',').append(method.name).append(",,,");
        }
        buffer.append(lineSeparator).append("Distance (m)");
        for (int i=0; i<methods.length; i++) {
            buffer.append(",ΔD (%),σ (%),Δα (°),σ (°)");
        }
        buffer.append(lineSeparator);
        for (final double[] result : results) {
            for (int i=0; i<result.length; i++) {
                if (i != 0) buffer.append(',');
                buffer.append(result[i]);
            }
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
