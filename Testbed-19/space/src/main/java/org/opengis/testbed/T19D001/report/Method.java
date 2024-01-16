/*
 *    OGC TestBed 19 D001
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T19D001.report;

import static java.lang.Math.*;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;


/**
 * A method for computing distance and azimuth.
 * Calculations may be done on a sphere or using a map projection.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
abstract class Method {
    /**
     * Name of this method. In the case of map projections,
     * it should be an EPSG operation method name.
     */
    protected final String name;

    /**
     * Errors in distance calculations.
     */
    private final Statistics distanceErrors;

    /**
     * Errors in azimuth calculations.
     */
    private final Statistics azimuthErrors;

    /**
     * Creates a new method.
     *
     * @param  name  EPSG operation method name, or arbitrary name if not a map projection.
     */
    protected Method(final String name) {
        this.name = name;
        distanceErrors = new Statistics("Distance (%)");
        azimuthErrors  = new Statistics("Azimuth (°)");
    }

    /**
     * Computes distance and azimuth errors between the given points.
     *
     * @param startPoint  the start point of the path to measure.
     * @param endPoint    the end point of the path to measure.
     * @param distance    expected distance.
     * @param azimuth     expected azimuth.
     * @throws TransformException if the calculation required a map projection and that operation failed.
     */
    public abstract void computeErrors(DirectPosition startPoint, DirectPosition endPoint, double distance, double azimuth)
            throws TransformException;

    /**
     * Adds statistics about distance and azimuth errors.
     *
     * @param distance       the distance in meters.
     * @param distanceError  error in distance calculation, in meters.
     * @param azimuthError   error in azimuth calculation, in decimal degrees.
     */
    protected final void addErrors(final double distance, double distanceError, double azimuthError) {
        distanceError *= 100 / distance;
        azimuthError = IEEEremainder(azimuthError, 360);
        distanceErrors.accept(abs(distanceError));
        azimuthErrors .accept(abs(azimuthError));
    }

    /**
     * Writes the average errors and their standard deviations in the given array.
     * The values are written in the tuple documented in {@link CompareDistanceCalculations#results}.
     *
     * @param  result  where to write the result.
     * @param  offset  index of the first element to write.
     * @return index after the last element written.
     */
    final int writeErrors(final double[] result, int offset) {
        final double de = distanceErrors.mean();
        final double ae =  azimuthErrors.mean();
        final double ds = distanceErrors.standardDeviation(false);
        final double as =  azimuthErrors.standardDeviation(false);
        result[offset++] = de;
        result[offset++] = min(ds, de - distanceErrors.minimum());
        result[offset++] = min(ds, distanceErrors.maximum() - de);
        result[offset++] = ae;
        result[offset++] = min(as, ae - azimuthErrors.minimum());
        result[offset++] = min(as, azimuthErrors.maximum() - ae);
        return offset;
    }

    /**
     * Prints the statistics results of distance and azimuth errors.
     *
     * @return string representation of statistics results.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuffer(800)
                .append("Errors when using ").append(name)
                .append(':').append(System.lineSeparator());
        final var f = StatisticsFormat.getInstance();
        return f.format(new Statistics[] {distanceErrors, azimuthErrors}, buffer, null).toString();
    }
}
