import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

public class FopHttp095Servlet extends HttpServlet
{
    private URIResolver uriResolver;
    private FopFactory fopFactory;
    private TransformerFactory transformerFactory;

    @Override
    public void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);

        // Batik (the SVG renderer included in FOP) uses Java2D/AWT to
        // load fonts instead of the FOP font loader, so we need to
        // register all TTF fonts.  This means that fonts available
        // in XSL-FO and fonts available in SVG's may not match
        // in name or behavior.
        registerAllFonts(config);

        this.uriResolver = new ServletContextURIResolver(getServletContext());
        this.transformerFactory = TransformerFactory.newInstance();
        this.transformerFactory.setURIResolver(this.uriResolver);
        this.fopFactory = FopFactory.newInstance();
        this.fopFactory.setURIResolver(this.uriResolver);

        String resourceName = "/WEB-INF/userconfig.xml";
        InputStream inConfig = config.getServletContext().getResourceAsStream(resourceName);
        if (inConfig == null)
            throw new ServletException("Could not find FOP config resource " + resourceName);
        try {
            DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            Configuration cfg = cfgBuilder.build(inConfig);
            fopFactory.setUserConfig(cfg);
        } catch (Exception ex) {
            throw new ServletException("Error loading user configuration", ex);
        } finally {
            try { inConfig.close(); } catch (IOException ex) {}
        }
    }

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException
    {
        PrintWriter out = response.getWriter();
        out.println("GET not supported, use POST");
    }

    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
        throws ServletException, IOException
    {
        // A user can POST a XSL-FO document to this servlet and get a PDF response.
        //
        // We generate the output to a ByteArrayOutputStream and then copy that
        // output stream to the response because it allows us to display nicely-
        // formatted error messages upon render failures.  It also avoids potential
        // problems with the Acrobat Reader Plug-in in Microsoft Internet
        // Explorer.
        InputStream inXSLFO = request.getInputStream();
        try {
            ByteArrayOutputStream outPDF = new ByteArrayOutputStream();
            try {
                generatePDF(inXSLFO, outPDF);

                // Copy the byte output stream to the response in one call
                response.setContentType("application/pdf");
                response.setContentLength(outPDF.size());
                OutputStream outResponse = response.getOutputStream();
                try {
                    outPDF.writeTo(outResponse);
                    outPDF.flush();
                } finally {
                    outResponse.close();
                }
            } catch (Exception ex) {
                throw new ServletException("Error generating PDF: " + ex.toString(), ex);
            } finally {
                outPDF.close();
            }
        } finally {
            inXSLFO.close();
        }
    }

    private void registerAllFonts(ServletConfig config)
    {
        // Register all TTF files in the font directory.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        File fontDir = new File(config.getServletContext().getRealPath("/WEB-INF/fonts"));
        for (File ttfFile : fontDir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".ttf");                
            }
        }))
        {
            try {
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, ttfFile));
            } catch (IOException ex) {
                System.err.println("Could not register font " + ttfFile + ": " + ex);
            } catch (FontFormatException ex) {
                System.err.println("Could not register font " + ttfFile + ": " + ex);
            }
        }
    }

    private void generatePDF(InputStream inXSLFO,
                             OutputStream outPDF)
        throws Exception
    {
        FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
        Fop fop = this.fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, outPDF);

        Transformer transformer = this.transformerFactory.newTransformer();
        transformer.setURIResolver(this.uriResolver);

        Source src = new StreamSource(inXSLFO);
        Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);
    }
}
