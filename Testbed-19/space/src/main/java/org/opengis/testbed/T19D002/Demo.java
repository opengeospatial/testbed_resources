/*
 *    OGC TestBed 19 D002
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T19D002;

import java.util.Locale;
import java.io.File;
import java.nio.file.StandardOpenOption;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;

// GeoAPI interfaces and code lists
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.identification.TopicCategory;

// Implementations of GeoAPI interfaces for ISO 19115 metadata
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;

// Specific to Apache SIS
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.geotiff.Compression;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.coverage.grid.GridCoverage;


/**
 * Demonstrates the use of new GeoKeys in a GeoTIFF file.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public class Demo {
    /**
     * Executes the demo.
     * This method assumes that the current directory if the TestBed-19 {@code demo} directory.
     *
     * @param  args  command-line arguments (currently ignored).
     * @throws Exception if an JAXB, and I/O or any other kind of errors occurred.
     */
    public static void main(String[] args) throws Exception {
        /*
         * Prepare ISO 19115 metadata as below:
         *
         * Metadata
         *   └─Identification info
         *       ├─Citation………………………………………………… Didymos and Dimorphos seen by DART
         *       │   ├─Cited responsible party
         *       │   │   ├─Role……………………………………… Editor
         *       │   │   └─Organisation………………… European Space Agency
         *       │   └─Presentation form……………… Image digital
         *       ├─Topic category………………………………… Extra terrestrial
         *       └─Locale……………………………………………………… English
         */
        var metadata       = new DefaultMetadata();
        var citation       = new DefaultCitation("Didymos and Dimorphos seen by DART");
        var identification = new DefaultDataIdentification(citation, null, Locale.ENGLISH, TopicCategory.EXTRA_TERRESTRIAL);
        var party          = new DefaultOrganisation("European Space Agency", null, null, null);
        var responsibility = new DefaultResponsibility(Role.PUBLISHER, null, party);
        citation.getPresentationForms().add(PresentationForm.IMAGE_DIGITAL);
        citation.getCitedResponsibleParties().add(responsibility);
        metadata.getIdentificationInfo().add(identification);
        System.out.println("Metadata to write:");
        System.out.println(metadata);
        /*
         * Get the raster data from the PNG + PGW + PRJ files.
         */
        GridCoverage data = DataStores.readGridCoverage(new File("data/DART_camera.png"), null).orElseThrow();
        System.out.println("Referencing information of \"DART_camera.png\":");
        System.out.println(data);
        /*
         * Write the GeoTIFF.
         */
        var sc = new StorageConnector(new File("target/DART_camera.tiff"));
        sc.setOption(Compression.OPTION_KEY, Compression.DEFLATE.withPredictor(BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING));
        sc.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        });
        try (GeoTiffStore ds = new GeoTiffStore(null, sc)) {
            ds.append(data, metadata);
        }
        /*
         * Re-read the metadata and referencing information, now from the GeoTIFF.
         */
        sc = new StorageConnector(new File("target/DART_camera.tiff"));
        try (GeoTiffStore ds = new GeoTiffStore(null, sc)) {
            GridCoverageResource firstImage = ds.findResource("1");
            System.out.println("Metadata read for the GeoTIFF file:");
            System.out.println(ds.getMetadata());
            System.out.println("Metadata read for the first image:");
            System.out.println(firstImage.getMetadata());
            System.out.println("Referencing information of \"DART_camera.tiff\":");
            System.out.println(firstImage.getGridGeometry());
        }
    }
}
