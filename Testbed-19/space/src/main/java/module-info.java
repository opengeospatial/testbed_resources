/*
 *    OGC TestBed 19 D001 and D002
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */

/**
 * Proof of concept of the new elements introduced in D001 and D002.
 * The two main classes are:
 *
 * <ul>
 *   <li>{@link org.opengis.testbed.T19D001.Demo}</li>
 *   <li>{@link org.opengis.testbed.T19D002.Demo}</li>
 * </ul>
 *
 * This demo requires a branch of GeoAPI and Apache SIS. As of January 2024, those branches have not been
 * merged to the main projects because they implement features that are not yet adopted as OGC standards.
 * See <a href="https://geomatys.github.io/testbed/19.html">https://geomatys.github.io/testbed/19.html</a>
 * for more up-to-date instructions.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
module org.opengis.testbed.space {
    requires org.apache.sis.referencing;
    requires org.apache.sis.storage.geotiff;
    requires org.glassfish.jaxb.runtime;
}
