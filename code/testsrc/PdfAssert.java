import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import org.junit.Assert;

public class PdfAssert extends Assert
{
    public static void assertPdfSame(String msg, InputStream in1, InputStream in2) throws IOException {
        assertPdfSame(msg, new PDFFile(IOUtils.toByteBuffer(in1)), new PDFFile(IOUtils.toByteBuffer(in2)));
    }

    public static void assertPdfSame(String msg, PDFFile pdf1, PDFFile pdf2)
    {
        if (pdf1.getNumPages() != pdf2.getNumPages())
            fail(msg);

        for (int i = 0; i != pdf1.getNumPages(); ++i) {
            PDFPage page1 = pdf1.getPage(i);
            PDFPage page2 = pdf2.getPage(i);
            assertPdfPageSame(msg, page1, page2);
        }
    }

    public static void assertPdfPageSame(String msg, PDFPage page1, PDFPage page2)
    {
        if (page1.getWidth() != page2.getWidth())
            fail(msg);
        if (page1.getHeight() != page2.getHeight())
            fail(msg);

        BufferedImage img1 = (BufferedImage)page1.getImage((int)page1.getWidth(), (int)page1.getHeight(), null, null, true, true);
        BufferedImage img2 = (BufferedImage)page2.getImage((int)page2.getWidth(), (int)page2.getHeight(), null, null, true, true);
        assertBufferedImageSame(msg, img1, img2);
    }

    private static void assertBufferedImageSame(String msg, BufferedImage img1, BufferedImage img2)
    {
        if (img1.getWidth() != img2.getWidth())
            fail(msg);
        if (img1.getHeight() != img2.getHeight())
            fail(msg);

        for (int x = 0; x != img1.getWidth(); ++x) {
            for (int y = 0; y != img1.getHeight(); ++y) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                if (rgb1 != rgb2)
                    fail(msg);
            }
        }
    }
}
