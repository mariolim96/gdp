package it.csipiemonte.gdp.gdporch.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfUtils {

    private static final Logger LOG = Logger.getLogger(PdfUtils.class);

    /**
     * Checks if a PDF file is readable and valid.
     */
    public static boolean isReadable(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            return true;
        } catch (IOException e) {
            LOG.warnf("PDF non leggibile: %s - %s", file.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a PDF has multiple pages and splits it if necessary.
     * Returns a list of single-page PDF files.
     */
    public static List<PDDocument> splitIfMultiPage(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.getNumberOfPages() > 1) {
                Splitter splitter = new Splitter();
                return splitter.split(document);
            }
        }
        return null;
    }

    /**
     * Extracts text from a PDF file.
     */
    public static String extractText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extracts text from the first page of a PDF file.
     */
    public static String extractTextFromFirstPage(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            return stripper.getText(document);
        }
    }
}
