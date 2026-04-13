package it.csipiemonte.gdp.gdporch;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility for generating PDF files for testing purposes.
 * Uses PDFBox 3.x API.
 */
public class TestPdfFactory {

    /**
     * Generates a valid single-page PDF/A-like file with a specific filename and text.
     */
    public static byte[] singlePage(String filename) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // PDFBox 3.x uses Standard14Font.FontName
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.beginText();
                cs.newLineAtOffset(50, 700);
                cs.showText("Giornale di Test — direttore responsabile");
                cs.newLineAtOffset(0, -20);
                cs.showText("Edizione del 01/03/2026");
                cs.newLineAtOffset(0, -20);
                cs.showText("Filename: " + filename);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Generates a multipage PDF.
     */
    public static byte[] multiPage(int nPages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 1; i <= nPages; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.beginText();
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Pagina " + i + " di " + nPages);
                    cs.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Generates a corrupted PDF (invalid bytes).
     */
    public static byte[] corrupted() {
        return new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37, 0x0A, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
    }

    /**
     * Generates a password-protected PDF.
     */
    public static byte[] passwordProtected(String password) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);
            doc.protect(spp);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
