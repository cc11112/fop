import java.io.*;
import java.util.*;
import com.meterware.httpunit.*;
import com.meterware.servletunit.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class RenderTest
{
    private File xmlFile;
    private File pdfFile;

    public RenderTest(File xmlFile, File pdfFile)
    {
        super();
        this.xmlFile = xmlFile;
        this.pdfFile = pdfFile;
    }

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        return Arrays.asList(new Object[][] {
                { new File(findTestCaseRoot(), "test1.xml"), new File(findTestCaseRoot(), "test1.pdf") },
                { new File(findTestCaseRoot(), "test2.xml"), new File(findTestCaseRoot(), "test2.pdf") },
                { new File(findTestCaseRoot(), "test3.xml"), new File(findTestCaseRoot(), "test3.pdf") },
            });
    }

    @Test
    public void testPdfGeneration() throws Exception
    {
        // Start up servlet container
        File webContentRoot = findWebContentRoot();
        assertNotNull(webContentRoot);
        File webXML = new File(webContentRoot, "WEB-INF/web.xml");
        assertTrue(webXML.exists());
        ServletRunner sr = new ServletRunner(webXML, "/fop");

        // Issue post request with XSL-FO, compare to generated PDF
        String url = "http://test.meterware.com/fop/fop";
        InputStream source = new FileInputStream(this.xmlFile);
        String contentType = "application/xml";
        try {
            WebRequest request = new PostMethodWebRequest(url, source, contentType);
            ServletUnitClient sc = sr.newClient();
            WebResponse response = sc.getResponse(request);
            assertNotNull("No response received", response);
            assertEquals("content type", "application/pdf", response.getContentType());
            assertEquals("response code", 200, response.getResponseCode());

            InputStream generated = response.getInputStream();
            InputStream reference = new FileInputStream(this.pdfFile);
            try {
                PdfAssert.assertPdfSame("PDF mismatch", generated, reference);
            } finally {
                reference.close();
                generated.close();
            }
        } finally {
            source.close();
        }
    }

    private static File findProjectRoot() throws Exception
    {
        String currentPath = System.getProperty("user.dir");
        for (File f = new File(currentPath); f.exists(); f = f.getParentFile()) {
            if (Arrays.asList(f.list()).contains("build.xml")) {
                return f;
            }
        }
        return null;
    }

    private static File findWebContentRoot() throws Exception
    {
        return new File(findProjectRoot(), "assemble");
    }

    private static File findTestCaseRoot() throws Exception
    {
        return new File(findProjectRoot(), "testcases");
    }
}
