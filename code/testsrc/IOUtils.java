import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class IOUtils
{
    private static final int BUFSIZE = 8192;

    public static ByteBuffer toByteBuffer(InputStream in) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFSIZE);
        pipe(out, in);
        return ByteBuffer.wrap(out.toByteArray());
    }

    public static void pipe(OutputStream out, InputStream in) throws IOException
    {
        byte buf[] = new byte[BUFSIZE];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
    }
}
