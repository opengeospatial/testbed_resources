/*
 *    OGC TestBed 18 D025
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T18D025.methods;

import static java.lang.Math.*;


/**
 * Convenience methods for converting Cartesian coordinates to spherical and conversely.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
final class CartesianAndSpherical {
    /**
     * Cartesian coordinates.
     */
    double x, y, z;

    /**
     * Spherical coordinates (longitude, latitude, radius). Angles are in degrees.
     */
    double β, λ, r;

    /**
     * Creates tuple initialized to zero.
     */
    CartesianAndSpherical() {
    }

    /**
     * Computes spherical coordinates from the Cartesian ones.
     */
    void toSpherical() {
        r = sqrt(x*x + y*y + z*z);
        β = toDegrees(asin(z / r));
        λ = toDegrees(atan2(y, x));
    }

    /**
     * Computes Cartesian coordinates from the spherical ones.
     */
    void toCartesian() {
        double  radλ = toRadians(λ);
        double  radβ = toRadians(β);
        double rsinβ = r * sin(radβ);
        double rcosβ = r * cos(radβ);
        x = rcosβ * cos(radλ);
        y = rcosβ * sin(radλ);
        z = rsinβ;
    }
}
