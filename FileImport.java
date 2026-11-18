import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/*
    Reads a PDF into plain text, or turns an image into base64
    so the AI can look at it.
*/
class FileImport {

    // Pull text out of a PDF. Long files are cut so the AI request stays smaller.
    public static String readPdfText(File file) throws IOException {
        PDDocument doc = Loader.loadPDF(file);
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null) {
                text = "";
            }
            text = text.trim();
            if (text.length() > 12000) {
                text = text.substring(0, 12000) + "\n\n[Text cut here because the file was long.]";
            }
            return text;
        } finally {
            doc.close();
        }
    }

    public static boolean isImage(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".gif")
                || name.endsWith(".webp");
    }

    public static boolean isPdf(File file) {
        return file.getName().toLowerCase().endsWith(".pdf");
    }

    public static String imageMime(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    public static String imageBase64(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(bytes);
    }
}
