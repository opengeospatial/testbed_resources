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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

// Implementation-specific
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;


/**
 * Definition of the operation method for "Geographic/spherical conversions".
 *
 * @author Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
public class SphericalToGeographic extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * Creates a new operation method.
     */
    public SphericalToGeographic() {
        super(Map.of(IDENTIFIERS_KEY,  new ImmutableIdentifier(Citations.OGC, "TB18-D025", "SphericalToGeographic"),
                     NAME_KEY,         "Geographic/spherical conversions",
                     FORMULA_KEY,      "See TB18 D025.",
                     REMARKS_KEY,      "Simple implementation for TestBed purposes."),
              parameters());
    }

    /**
     * Builds the set of parameters accepted by this operation method.
     */
    private static ParameterDescriptorGroup parameters() {
        var b = new ParameterBuilder();
        return b.addName("Geographic/spherical conversions").createGroup();
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
        /*
         * The latitude and radius values should be slightly modified.
         * For Testbed 18, we will ignore that change.
         */
        return MathTransforms.identity(3);
    }
}
