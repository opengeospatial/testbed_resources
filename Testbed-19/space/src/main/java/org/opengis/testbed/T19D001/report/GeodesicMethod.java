/*
 *    OGC TestBed 19 D001
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T19D001.report;

import java.util.Map;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.GeodeticCalculator;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.GeographicCRS;


/**
 * A method for computing distance and azimuth using geodesic calculator.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
final class GeodesicMethod extends Method {
    /**
     * The geodetic calculator for computing distances and azimuths.
     */
    private final GeodeticCalculator geodesy;

    /**
     * Whether to disable the automatic application of datum shift.
     * This datum shift is applied when converting geodetic latitudes
     * form WGS84 ellipsoid to a sphere.
     */
    private final boolean disableDatumShift;

    /**
     * Creates a new method based on the specified geodetic calculator.
     *
     * @param  name     arbitrary name for identifying this method.
     * @param  baseCRS  the coordinate reference system to use.
     */
    GeodesicMethod(final String name, final GeodeticCalculator geodesy) {
        super(name);
        this.geodesy = geodesy;
        disableDatumShift = false;
    }

    /**
     * Creates a new method using the same geodetic calculator that the specified method,
     * but with datum shift disabled.
     *
     * @param  name      arbitrary name for identifying this method.
     * @param  template  geodesic method to copy.
     */
    GeodesicMethod(final String name, final GeodesicMethod template) {
        super(name);
        geodesy = template.geodesy;
        disableDatumShift = true;
    }

    /**
     * Creates a new method based on a geodetic calculator for a CRS with the given radius.
     *
     * @param  name     arbitrary name for identifying this method.
     * @param  baseCRS  the coordinate reference system to use as a template.
     * @param  radius   the radius of the new sphere to build.
     */
    GeodesicMethod(final String name, GeographicCRS baseCRS, final double radius) {
        super(name);
        var datum         = baseCRS.getDatum();
        var ellipsoid     = datum.getEllipsoid();
        var primeMeridian = datum.getPrimeMeridian();
        Map<String,?> properties = Map.of(DefaultEllipsoid.NAME_KEY, name);
        ellipsoid  = DefaultEllipsoid.createEllipsoid(properties, radius, radius, ellipsoid.getAxisUnit());
        properties = Map.of(DefaultEllipsoid.NAME_KEY, ellipsoid.getName());
        datum      = new DefaultGeodeticDatum(properties, ellipsoid, primeMeridian);
        baseCRS    = new DefaultGeographicCRS(properties, datum, baseCRS.getCoordinateSystem());
        geodesy    = GeodeticCalculator.create(baseCRS);
        disableDatumShift = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void computeErrors(DirectPosition startPoint, DirectPosition endPoint,
                              final double distance, final double azimuth)
    {
        if (disableDatumShift) {
            // Reconstruct the points without CRS information.
            startPoint = new DirectPosition2D(startPoint.getOrdinate(0), startPoint.getOrdinate(1));
            endPoint   = new DirectPosition2D(  endPoint.getOrdinate(0),   endPoint.getOrdinate(1));
        }
        geodesy.setStartPoint(startPoint);
        geodesy.setEndPoint(endPoint);
        addErrors(distance, geodesy.getGeodesicDistance() - distance,
                            geodesy.getStartingAzimuth()  - azimuth);
    }
}
