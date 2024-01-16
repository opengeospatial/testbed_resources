/*
 *    OGC TestBed 19 D002
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
package org.opengis.testbed.T19D001;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;


/**
 * Converts the Horizons On-Line Ephemeris System files to Moving Feature CSV files.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public class ToCSV implements Closeable {
    /** Identifier of the CRS of the data to read. */
    private static final String CRS = "urn:ogc:def:crs:JPL::0";

    /** The line that indicates the beginning of data. */
    private static final String START = "$$SOE";

    /** The line that indicates the end of data. */
    private static final String END = "$$EOE";

    /**
     * Column where the Julian date is stored in the source file.
     * This column is followed by calendar date (1) and delta-T (2).
     */
    private static final int COLUMN_JULIAN_DATE = 0;

    /** Column where the <var>x</var> coordinate is stored in the source file. */
    private static final int COLUMN_X = 3;

    /** Column where the <var>y</var> coordinate is stored in the source file. */
    private static final int COLUMN_Y = 4;

    /** Column where the <var>z</var> coordinate is stored in the source file. */
    private static final int COLUMN_Z = 5;

    /** Number of character of the time column. Used for right-alignment. */
    private static final int TIME_WIDTH = 6;

    /** Subsampling to apply on the rows, for reducing file size. */
    private static final int SUBSAMPLING = 100;

    /** Julian days used in source files. */
    private final DefaultTemporalCRS sourceTimeCRS;

    /** The platform-dependent line separator. */
    private final String lineSeparator;

    /** Temporary file where to write data without header. */
    private final Path temporaryFile;

    /** Minimal and maximal coordinates found in the data. */
    private final double[] lowerCorner, upperCorner;

    /**
     * Creates a new instance.
     *
     * @throws IOException if an error occurred while creating the temporary file.
     */
    private ToCSV() throws IOException {
        sourceTimeCRS = DefaultTemporalCRS.castOrCopy(CommonCRS.Temporal.JULIAN.crs());
        lineSeparator = System.lineSeparator();
        temporaryFile = Files.createTempFile("DART", ".csv");
        lowerCorner   = new double[COLUMN_Z - COLUMN_X + 1];
        upperCorner   = new double[COLUMN_Z - COLUMN_X + 1];
        Arrays.fill(lowerCorner, Double.POSITIVE_INFINITY);
        Arrays.fill(upperCorner, Double.NEGATIVE_INFINITY);
    }

    /**
     * Translates the file specified on the command line.
     *
     * @param  args  source file and target file.
     * @throws IOException if an error occurred while reading or writing the files.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Expected arguments: [source file] [destination file]");
        } else {
            try (ToCSV t = new ToCSV()) {
                t.translate(Path.of(args[0]), Path.of(args[1]));
            }
        }
    }

    /**
     * Reads a mandatory line from the given reader, never returns {@code null}.
     *
     * @param  input  the reader from which to read a line.
     * @return the next line from the given reader, never null.
     * @tnrows EOFException if there is no more line to read.
     * @throws IOException if another error occurred while reading the file.
     */
    private static String readLine(BufferedReader input) throws IOException {
        String line = input.readLine();
        if (line != null) return line;
        throw new EOFException();
    }

    /**
     * Splits a comma-separated line.
     *
     * @param  line  the comma-separated line of values.
     * @return columns in the given line.
     */
    private static String[] split(String line) {
        return line.split(",");
    }

    /**
     * Translates the specified file.
     *
     * @param  source  Horizons On-Line Ephemeris System file to read.
     * @param  target  Moving Feature CSV file to write.
     * @throws IOException if an error occurred while reading or writing the files.
     */
    private void translate(final Path source, final Path target) throws IOException {
        double julianDay;
        final double epoch;
        try (BufferedReader input  = Files.newBufferedReader(source);
             BufferedWriter output = Files.newBufferedWriter(temporaryFile))
        {
            String line;
            do line = readLine(input);
            while (!line.equals(START));
            String[] columns = split(readLine(input));
            julianDay = Double.parseDouble(columns[COLUMN_JULIAN_DATE]);
            epoch = julianDay;
            double startTime = 0;
            int skipCount = SUBSAMPLING;
            while (!(line = readLine(input)).equals(END)) {
                final String[] nextColumns = split(line);
                julianDay = Double.parseDouble(nextColumns[COLUMN_JULIAN_DATE]);
                double endTime = Math.rint((julianDay - epoch) * (24*60*60)) / 60;
                for (int i = COLUMN_X; i <= COLUMN_Z; i++) {
                    final double value = Double.parseDouble(columns[i]);
                    final int j = i - COLUMN_X;
                    if (value < lowerCorner[j]) lowerCorner[j] = value;
                    if (value > upperCorner[j]) upperCorner[j] = value;
                }
                if (--skipCount <= 0) {
                    output.write("DART, ");
                    writeTime(output, startTime);
                    writeTime(output, endTime);
                    output.write(columns[COLUMN_X]); output.write(' ');
                    output.write(columns[COLUMN_Y]); output.write(' ');
                    output.write(columns[COLUMN_Z]); output.write(lineSeparator);
                    startTime = endTime;
                    columns   = nextColumns;
                    skipCount = SUBSAMPLING;
                }
            }
        }
        /*
         * Write the header. CRS is hard coded in this version.
         */
        try (BufferedWriter output = Files.newBufferedWriter(target)) {
            output.write("@stboundedby, " + CRS + ", 3D,");
            writeCorner(output, lowerCorner);
            writeCorner(output, upperCorner);
            output.write(' ');
            writeTimeLimit(output, epoch);
            writeTimeLimit(output, julianDay);
            output.write("minute");
            output.write(lineSeparator);
            output.write("@columns, mfidref, trajectory");
            output.write(lineSeparator);
        }
        try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.APPEND)) {
            Files.copy(temporaryFile, output);
        }
    }

    /**
     * Writes the spatial part of a corner of the bounding box.
     *
     * @param  output  where to write.
     * @param  corner  coordinates of the corner.
     * @throws IOException if an error occurred while writing the values.
     */
    private static void writeCorner(final BufferedWriter output, final double[] corner) throws IOException {
        for (double value : corner) {
            output.write(' ');
            output.write(String.valueOf(value));
        }
        output.write(',');
    }

    /**
     * Writes the temporal part of a corner of the bounding box.
     *
     * @param  output  where to write.
     * @param  corner  coordinates of the corner.
     * @throws IOException if an error occurred while writing the values.
     */
    private void writeTimeLimit(final BufferedWriter output, final double corner) throws IOException {
        output.write(sourceTimeCRS.toInstant(corner).toString());
        output.write(", ");
    }

    /**
     * Writes the given temporal value, with right alignment and without trailing {@code ".0"} characters.
     *
     * @param  output  where to write.
     * @param  time    the temporal value to write.
     * @throws IOException if an error occurred while writing the value.
     */
    private static void writeTime(final BufferedWriter output, final double time) throws IOException {
        String value = Double.toString(time);
        int length = value.length();
        if (value.endsWith(".0")) length -= 2;
        for (int i = TIME_WIDTH - length; --i >= 0;) {
            output.write(' ');
        }
        output.write(value, 0, length);
        output.write(", ");
    }

    /**
     * Releases resources used by this translator.
     *
     * @throws IOException if an error occurred while deleting the temporary file.
     */
    @Override
    public void close() throws IOException {
        Files.delete(temporaryFile);
    }
}
