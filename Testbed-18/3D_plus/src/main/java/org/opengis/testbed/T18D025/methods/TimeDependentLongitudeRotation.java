/*
 *    OGC TestBed 18 D025
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T18D025.methods;

import java.util.Map;
import java.time.Year;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

// Implementation-specific
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Longitude;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;


/**
 * Definition of the operation method for "Trajectory to Conventional frame".
 *
 * @author Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
public class TimeDependentLongitudeRotation extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * The CRS used in Testbed 18 for temporal coordinates.
     * An industrial application would infer this CRS from the GML file.
     * But for this simple demo application, we use hard-coded value for simplicity.
     */
    static final DefaultTemporalCRS TIME_CRS = DefaultTemporalCRS.castOrCopy(CommonCRS.Temporal.TRUNCATED_JULIAN.crs());

    /**
     * Converts a date and time from year to truncated Julian day.
     *
     * @param  yearAndDay  the date as the year with days as fractional amount of year.
     * @return the truncated Julian day.
     */
    static double toTruncatedJulian(double yearAndDay) {
        Year    year = Year.of((int) Math.floor(yearAndDay));
        Instant time = OffsetDateTime.of(year.atMonth(1).atDay(1), LocalTime.MIDNIGHT, ZoneOffset.UTC).toInstant();
        double  day  = (yearAndDay - year.getValue()) * year.length();
        time         = time.plusMillis(Math.round(day * (24*60*60*1000L)));
        return TIME_CRS.toValue(time);
    }

    /**
     * Creates a new operation method.
     */
    public TimeDependentLongitudeRotation() {
        super(Map.of(IDENTIFIERS_KEY,  new ImmutableIdentifier(Citations.OGC, "TB18-D025", "TimeDependentLongitudeRotation"),
                     NAME_KEY,         "Time-dependent longitude rotation (equatorial CS)",
                     FORMULA_KEY,      "See TB18 D025.",
                     REMARKS_KEY,      "Simple implementation for TestBed purposes."),
              parameters());
    }

    /**
     * Builds the set of parameters accepted by this operation method.
     */
    private static ParameterDescriptorGroup parameters() {
        var b     = new ParameterBuilder();
        var init  = b.addName("Longitude axis rotation").create(Double.NaN, Units.DEGREE);
        var rate  = b.addName("Rate of change of longitude axis rotation").create(Double.NaN, Units.DEGREE.divide(Units.DAY));
        var epoch = b.addName("Parameter reference epoch").create(Double.NaN, Units.TROPICAL_YEAR);
        return b.addName("Time-dependent longitude rotation (equatorial CS)")
                .createGroup(init, rate, epoch);
    }

    /**
     * Creates a math transform from the specified group of parameter values.
     *
     * @param  factory     the factory to use if this constructor needs to create other math transforms.
     * @param  parameters  the parameter values that define the transform to create.
     * @return the math transform created from the given parameters.
     * @throws FactoryException if the math transform cannot be created.
     */
    @Override
    public MathTransform createMathTransform(MathTransformFactory factory, ParameterValueGroup parameters) throws FactoryException {
        return new Transform(parameters);
    }

    /**
     * The transform operating on coordinate values.
     */
    private static class Transform extends AbstractMathTransform {
        /**
         * Longitude axis rotation in degrees.
         */
        private final double longitudeRotation;

        /**
         * Rate of change of longitude axis rotation in degrees per day.
         */
        private final double rateOfChange;

        /**
         * Parameter reference epoch in truncated Julian day.
         */
        private final double epoch;

        /**
         * Creates a new transform from the given parameters.
         */
        Transform(final ParameterValueGroup parameters) {
            longitudeRotation  = parameters.parameter("Longitude axis rotation").doubleValue(Units.DEGREE);
            rateOfChange       = parameters.parameter("Rate of change of longitude axis rotation").doubleValue(Units.DEGREE.divide(Units.DAY));
            double yearAndDay  = parameters.parameter("Parameter reference epoch").doubleValue(Units.TROPICAL_YEAR);
            epoch              = toTruncatedJulian(yearAndDay);
        }

        @Override public int getSourceDimensions() {return 4;}
        @Override public int getTargetDimensions() {return 4;}

        /**
         * Transforms a single coordinate tuple in an array.
         * Input and output coordinates are (latitude, longitude, radius, time) with latitude/longitude in degrees.
         * In input, coordinates are relative to ECI. In output, coordinates are relative to ECEF.
         * Time is measured in days since Truncated Julian epoch for both input and output coordinates.
         *
         * @param  srcPts    the array containing the source coordinates (cannot be {@code null}).
         * @param  srcOff    the offset to the point to be transformed in the source array.
         * @param  dstPts    the array into which the transformed coordinates is returned.
         *                   May be the same than {@code srcPts}.
         *                   May be {@code null} if only the derivative matrix is desired.
         * @param  dstOff    the offset to the location of the transformed point that is stored in the destination array.
         * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
         * @return the matrix of the transform derivative at the given source position,
         *         or {@code null} if the {@code derivate} argument is {@code false}.
         * @throws TransformException if the point cannot be transformed or
         *         if a problem occurred while calculating the derivative.
         */
        @Override
        public Matrix transform(final double[] srcPts, int srcOff,
                                final double[] dstPts, int dstOff, boolean derivate)
                throws TransformException
        {
            if (dstPts != null) {
                double β = srcPts[  srcOff];        // Ecliptic latitude
                double λ = srcPts[++srcOff];        // Ecliptic longitude
                double r = srcPts[++srcOff];        // Radius
                double t = srcPts[++srcOff];        // Truncated Julian day
                λ += longitudeRotation + rateOfChange * (t - epoch);
                λ  = Longitude.normalize(λ);
                dstPts[  dstOff] = β;
                dstPts[++dstOff] = λ;
                dstPts[++dstOff] = r;
                dstPts[++dstOff] = t;
            }
            if (derivate) {
                Matrix derivative = Matrices.createDiagonal(5, 5);
                derivative.setElement(0, 0, rateOfChange);
                return derivative;
            }
            return null;
        }
    }
}
