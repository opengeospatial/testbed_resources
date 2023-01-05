/*
 *    OGC TestBed 18 D025
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */

/**
 * Implementations of operation methods created for TestBed 18 task D025.
 * The classes in this package need to be declared as service providers in
 * {@code META-INF/services/org.opengis.referencing.operation.OperationMethod}.
 * Those classes need to implement an interface specific to Apache SIS project:
 * {@link org.apache.sis.referencing.operation.transform.MathTransformProvider}.
 * But this feature could be made implementation neutral if the same method is
 * added to GeoAPI.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
package org.opengis.testbed.T18D025.methods;
