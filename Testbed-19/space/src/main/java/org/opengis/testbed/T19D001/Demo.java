/*
 *    OGC TestBed 19 D002
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T19D001;

import java.io.File;
import java.util.Arrays;
import java.time.Instant;

// Implementation-neutral interfaces (GeoAPI)
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Specific implementation (Apache SIS)
import org.apache.sis.xml.XML;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
//import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.geometry.GeneralDirectPosition;


/**
 * A demonstration of the capability to read a CRS definition in GML as extended by D001.
 * This demo read the extended GML document, then rewrites it in Well-Known Text 2.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public class Demo {
    /**
     * Directory where are provided the CRS definitions produced by TestBed-19 as examples.
     * This directory may depend on your system.
     */
    private static final File EXAMPLE_DIRECTORY = new File("data/coordinate-transformations");

    /**
     * The file generated by D002.
     */
    private static final File GEOTIFF_IMAGE = new File("target/DART_camera.tiff");

    /**
     * Loads the GML file and prints the result of an arbitrary coordinate transformation.
     * This method assumes that the current directory is the TestBed-19 {@code demo} directory.
     *
     * @param  args  command-line arguments (currently ignored).
     * @throws Exception if an JAXB, and I/O or any other kind of errors occurred.
     */
    public static void main(String[] args) throws Exception {
        /*
         * Load the XML file that describe the coordinate operations from Dimorphos moonlet to DART.
         * The operation is made of the following steps:
         *
         *    - Dimorphos to solar system barycenter
         *    - Solar system berycenter to DART (Cartesian)
         *    - DART (Cartesian) to DART (Spherical)
         */
        var dimorphosToDART = (CoordinateOperation) XML.unmarshal(new File(EXAMPLE_DIRECTORY, "OperationChain.xml"));
        /*
         * Get the temporal component of the CRS as the implementation-specific SIS class.
         * We could use the GeoAPI implementation-neutral interface, but the SIS class has
         * a convenience mehod for the conversion of instants.
         */
        var timeCRS = DefaultTemporalCRS.castOrCopy(CRS.getTemporalComponent(dimorphosToDART.getSourceCRS()));
        /*
         * Set a coordinates on a bright spot of the moonlet in coordinates as seeen from DART.
         * Those coordinates come from the TIFF image in Testbed-19 D001 example.
         * Then, transform to coordinates relative to Dimorphos.
         */
        double[] coordinates = {
            24, 18,                         // Pixel coordinates in arc-seconds from image center.
            920,                            // Distance between DART and Dimorphos (km).
            timeCRS.toValue(Instant.parse("2022-09-29T16:00:00Z"))
        };
        MathTransform mt = dimorphosToDART.getMathTransform();
        mt.inverse().transform(coordinates, 0, coordinates, 0, 1);
        /*
         * Prints result. The result is a bit far (about 50,000 km) because we
         * did not adjusted the data for being very close to the impact time.
         */
        System.out.println("Transformation of (24″, 18″, 920 km):");
        System.out.println(Arrays.toString(coordinates));
        /*
         * If the GeoTIFF file exists, do the transformation in the opposite way (from Dimorphos to DART),
         * but with the operation chain completed up to pixel coordinates.
         */
        if (GEOTIFF_IMAGE.exists()) {
            try (DataStore ds = DataStores.open(GEOTIFF_IMAGE)) {
                GridCoverageResource firstImage = (GridCoverageResource) ds.findResource("1");
                CoordinateReferenceSystem crs = firstImage.getGridGeometry().getCoordinateReferenceSystem();
                /*
                 * Ask SIS to find the operation from the target CRS of the previous operation chain
                 * and the CRS of the GeoTIFF image. It should be the identity transform, because we
                 * have set the image CRS to the same EngineeringCRS in D001.
                 *
                 * TODO: does not work because the datum name in the GeoTIFF image is lost.
                 */
//              CoordinateOperation toGeoTIFF = CRS.findOperation(dimorphosToDART.getTargetCRS(), crs, null);
//              mt = MathTransforms.concatenate(mt, toGeoTIFF.getMathTransform());
                /*
                 * Finally, complete to the conversion from GeoTIFF to pixel coordinates.
                 * We will transform DirectPosition instead of array because the number of dimensions changes.
                 */
//              mt = MathTransforms.concatenate(mt, firstImage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER));
            }
            System.out.println("Dimorphos to pixel coordinates:");
            System.out.println(mt.transform(new GeneralDirectPosition(coordinates), null));
        }
    }
}
