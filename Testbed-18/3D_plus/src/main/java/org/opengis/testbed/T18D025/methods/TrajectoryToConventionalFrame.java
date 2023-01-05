/*
 *    OGC TestBed 18 D025
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T18D025.methods;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;
import java.time.Instant;
import java.io.FileNotFoundException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

// Temporarily implementation-specific, to be replaced by GeoAPI interfaces in future GeoAPI version.
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.AbstractAttribute;

// Implementation-specific
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.FeatureSet;
import org.locationtech.jts.geom.LineString;


/**
 * Definition of the operation method for "Trajectory to Conventional frame".
 *
 * @author Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
public class TrajectoryToConventionalFrame extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * Creates a new operation method.
     */
    public TrajectoryToConventionalFrame() {
        super(Map.of(IDENTIFIERS_KEY,  new ImmutableIdentifier(Citations.OGC, "TB18-D025", "TrajectoryToConventional"),
                     NAME_KEY,         "Trajectory to Conventional frame",
                     FORMULA_KEY,      "Use trajectory coordinates and yaw/roll/pitch attributes from Moving Feature CSV file.",
                     REMARKS_KEY,      "Simple implementation for TestBed purposes."),
              parameters());
    }

    /**
     * Builds the set of parameters accepted by this operation method.
     */
    private static ParameterDescriptorGroup parameters() {
        var b    = new ParameterBuilder();
        var file = b.addName("Feature trajectory file").create(URI.class, null);
        return b.addName("Trajectory to Conventional frame").createGroup(file);
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
        try {
            return new Transform(parameters);
        } catch (FileNotFoundException | DataStoreException e) {
            throw new FactoryException("Cannot read the moving feature file.", e);
        } catch (IllegalArgumentException | ClassCastException | ArrayIndexOutOfBoundsException e) {
            throw new FactoryException("Moving feature file does not contain the expected properties.", e);
        }
    }

    /**
     * The transform operating on coordinate values.
     */
    private static class Transform extends AbstractMathTransform {
        /**
         * Number of dimensions of the CRS and of coordinate tuples.
         * This value should be fetched from the CRS definition.
         * However, for making the demo simpler, we use a hard-coded value.
         */
        private static final int TRAJECTORY_DIMENSION = 3;

        /**
         * Values extracted from parameters declared in the moving features file.
         * This is a sequence of (x, y, z) coordinate tuples in Cartesian CS.
         */
        private final double[] trajectory;

        /**
         * The start time of each coordinate tuple in {@link #trajectory}.
         */
        private final double[] startTimes;

        /**
         * The yaw, pitch and roll attribute values found in the Moving Feature file.
         * Not used in this simple demo.
         */
        private final double[] yaw, pitch, roll;

        /**
         * Creates a math transform from the specified group of parameter values.
         *
         * @throws FileNotFoundException if the file specified in the parameters is not found.
         * @throws DataStoreException if the moving feature file cannot be parsed.
         * @throws IllegalArgumentException if a required feature property is not found.
         * @throws ClassCastException if a property value is not of the expected type.
         * @throws ArrayIndexOutOfBoundsException if a property has more values than expected.
         */
        Transform(ParameterValueGroup parameters) throws FileNotFoundException, DataStoreException {
            final URI filename = parameters.parameter("Feature trajectory file").valueFile();
            URL file = Transform.class.getClassLoader().getResource(filename.getPath());
            if (file == null) {
                throw new FileNotFoundException(filename.getPath());
            }
            /*
             * The file should be a set of features containing only one feature.
             * This is not verified for this demo application.
             */
            AbstractFeature feature;
            try (DataStore ds = DataStores.open(file)) {
                feature = ((FeatureSet) ds).features(false).findFirst().get();
            }
            /*
             * The remaining code in this constructor use hard-coded property names and types.
             * If an assumption does not hold, IllegalArgumentException or ClassCastException
             * will be thrown. A more industrial code would do an analysis of feature type.
             */
            var attribute = (AbstractAttribute<?>) feature.getProperty("trajectory");
            var datetimes = attribute.characteristics().get("datetimes").getValues();
            var timeCount = datetimes.size();
            var geometry  = (LineString) attribute.getValue();
            startTimes = datetimes.stream().mapToDouble((t) -> TimeDependentLongitudeRotation.TIME_CRS.toValue((Instant) t)).toArray();
            trajectory = new double[timeCount * TRAJECTORY_DIMENSION];
            for (int i=0, c=0; i<timeCount; i++) {
                var coordinates = geometry.getCoordinateN(i);
                var coords = new CartesianAndSpherical();
                coords.β = coordinates.getX();          // Ecliptic latitude
                coords.λ = coordinates.getY();          // Ecliptic longitude
                coords.r = coordinates.getZ();          // Radius
                coords.toCartesian();
                trajectory[c++] = coords.x;             // Heliocentric X
                trajectory[c++] = coords.y;             // Heliocentric Y
                trajectory[c++] = coords.z;             // Heliocentric Z
            }
            yaw   = toArray(feature, "yaw",   timeCount);
            pitch = toArray(feature, "pitch", timeCount);
            roll  = toArray(feature, "roll",  timeCount);
        }

        /**
         * Returns the value of given property as an array of floating point numbers.
         *
         * <h4>Limitations</h4>
         * We should verify that the {@code MF:datetimes} characteristic of the specified property is the same
         * as the one of the trajectory. For simplicity reason, we don't do this verification in this demo.
         *
         * @param  feature        the feature from which to get the property.
         * @param  propertyName   name of the property to get.
         * @param  expectedCount  expected number of values in the array.
         * @return the property value as an array of floating points.
         * @throws IllegalArgumentException if the specified feature property is not found.
         * @throws ClassCastException if the property value is not of the expected type.
         * @throws ArrayIndexOutOfBoundsException if the property has more values than expected.
         */
        private static double[] toArray(AbstractFeature feature, String propertyName, int expectedCount) {
            double[] values = new double[expectedCount];
            int actualCount = 0;
            for (Object item : (Collection<?>) feature.getPropertyValue(propertyName)) {
                values[actualCount++] = ((Number) item).doubleValue();
            }
            // If the array is shorter than expected, repeat the last value.
            Arrays.fill(values, actualCount, expectedCount, values[actualCount - 1]);
            return values;
        }

        @Override public int getSourceDimensions() {return 4;}
        @Override public int getTargetDimensions() {return 4;}

        /**
         * Transforms a single coordinate tuple in an array.
         * Input coordinates are Cartesian coordinates relative to Voyager spacecraft + time.
         * Output coordinates are (latitude, longitude, radius, time) with latitude/longitude in degrees.
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
                var coords = new CartesianAndSpherical();
                coords.x = srcPts[  srcOff];
                coords.y = srcPts[++srcOff];
                coords.z = srcPts[++srcOff];
                double t = srcPts[++srcOff];
                int i = Arrays.binarySearch(startTimes, t);
                if (i < 0) {
                    i = ~i;
                    if (i == 0 || i >= startTimes.length) {
                        throw new TransformException("Time out of range.");
                    }
                    /*
                     * We should interpolate `trajectory` values here.
                     * For keeping this demo simple, we skip that step.
                     */
                }
                i *= TRAJECTORY_DIMENSION;
                coords.x += trajectory[  i];
                coords.y += trajectory[++i];
                coords.z += trajectory[++i];
                /*
                 * Convert to spherical coordinates.
                 */
                coords.toSpherical();
                dstPts[  dstOff] = coords.β;    // Ecliptic latitude
                dstPts[++dstOff] = coords.λ;    // Ecliptic longitude
                dstPts[++dstOff] = coords.r;
                dstPts[++dstOff] = t;
            }
            return null;
        }
    }
}
