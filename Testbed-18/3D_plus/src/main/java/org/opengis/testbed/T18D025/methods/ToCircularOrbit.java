/*
 *    OGC TestBed 18 D025
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T18D025.methods;

import java.util.Map;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

// Implementation-specific
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;


/**
 * Definition of the operation method for "To circular orbit (Spherical domain)".
 *
 * @author Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
public class ToCircularOrbit extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * Creates a new operation method.
     */
    public ToCircularOrbit() {
        super(Map.of(IDENTIFIERS_KEY,  new ImmutableIdentifier(Citations.OGC, "TB18-D025", "ToCircularOrbit"),
                     NAME_KEY,         "To circular orbit (Spherical domain)",
                     FORMULA_KEY,      "See TB18 D025.",
                     REMARKS_KEY,      "Simple implementation for TestBed purposes."),
              parameters());
    }

    /**
     * Builds the set of parameters accepted by this operation method.
     */
    private static ParameterDescriptorGroup parameters() {
        var b       = new ParameterBuilder();
        var radius  = b.addName("Orbit radius").create(0, Units.KILOMETRE);
        var speed   = b.addName("Orbit speed") .create(0, Units.KILOMETRE.divide(Units.SECOND));
        var equinox = b.addName("Vernal equinox").create(Double.NaN, Units.TROPICAL_YEAR);
        return b.addName("To circular orbit (Spherical domain)")
                .createGroup(radius, speed, equinox);
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
         * Orbital parameters.
         */
        private final double radius, speed, equinox;

        /**
         * Creates a math transform from the specified group of parameter values.
         */
        Transform(ParameterValueGroup parameters) {
            radius   = parameters.parameter("Orbit radius").doubleValue(Units.KILOMETRE);
            double v = parameters.parameter("Orbit speed").doubleValue(Units.KILOMETRE.divide(Units.SECOND));
            double t = parameters.parameter("Vernal equinox").doubleValue(Units.TROPICAL_YEAR);
            equinox  = TimeDependentLongitudeRotation.toTruncatedJulian(t);
            speed    = (v / radius) * (24*60*60);       // Convert km/s to radian/day.
        }

        @Override public int getSourceDimensions() {return 4;}
        @Override public int getTargetDimensions() {return 4;}

        /**
         * Transforms a single coordinate tuple in an array.
         * Input and output coordinates are (longitude, latitude, radius, time) with longitude/latitude in degrees.
         * Input coordinates are heliocentric and output coordinates are geocentric.
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
                var earth  = new CartesianAndSpherical();
                var coords = new CartesianAndSpherical();
                coords.β   = srcPts[  srcOff];              // Ecliptic latitude
                coords.λ   = srcPts[++srcOff];              // Ecliptic longitude
                coords.r   = srcPts[++srcOff];              // Radius
                double t   = srcPts[++srcOff];              // Truncated Julian day
                earth.λ    = speed * (t - equinox);         // I don't know if the sign is right.
                earth.r    = radius;
                earth.toCartesian();
                coords.toCartesian();
                coords.x -= earth.x;
                coords.y -= earth.y;
                coords.z -= earth.z;
                coords.toSpherical();
                dstPts[  dstOff] = coords.β;
                dstPts[++dstOff] = coords.λ;
                dstPts[++dstOff] = coords.r;
                dstPts[++dstOff] = t;
            }
            return null;
        }
    }
}
