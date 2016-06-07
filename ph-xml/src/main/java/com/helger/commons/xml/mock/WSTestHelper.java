package com.helger.commons.xml.mock;

import java.io.File;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.serialize.MicroReader;

public final class WSTestHelper
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (WSTestHelper.class);

  private WSTestHelper ()
  {}

  @Nonnegative
  public static int testIfAllSunJaxwsFilesAreValid (@Nonnull final String sBaseDir,
                                                    final boolean bContinueOnError) throws Exception
  {
    final int nTotalImplementationCount = 0;
    final File aFile = new File (sBaseDir, "sun-jaxws.xml");
    if (aFile.isFile ())
    {
      s_aLogger.info ("Checking file " + aFile.getAbsolutePath ());

      final IMicroDocument aDoc = MicroReader.readMicroXML (aFile);
      if (aDoc == null)
      {
        final String sMsg = "The file is invalid XML!";
        s_aLogger.warn (sMsg);
        if (!bContinueOnError)
          throw new Exception (sMsg);
      }
      else
      {
        for (final IMicroElement eEndpoint : aDoc.getDocumentElement ().getAllChildElements ("endpoint"))
        {
          final String sName = eEndpoint.getAttributeValue ("name");
          final String sImplementation = eEndpoint.getAttributeValue ("implementation");

          // Check if implementation class exists
          Class <?> aImplClass = null;
          try
          {
            aImplClass = Class.forName (sImplementation);
          }
          catch (final Throwable t)
          {
            final String sMsg = "The implementation class '" +
                                sImplementation +
                                "' of endpoint '" +
                                sName +
                                "' is invalid - " +
                                t.getMessage ();
            s_aLogger.warn (sMsg);
            if (!bContinueOnError)
              throw new Exception (sMsg);
          }

          if (aImplClass != null)
          {
            if (s_aLogger.isDebugEnabled ())
              s_aLogger.debug ("Implementation class '" + sImplementation + "' found");

            final WebService aWebService = aImplClass.getAnnotation (WebService.class);
            if (aWebService == null)
            {
              final String sMsg = "The implementation class '" +
                                  sImplementation +
                                  "' is missing the @WebService annotation";
              s_aLogger.warn (sMsg);
              if (!bContinueOnError)
                throw new Exception (sMsg);
            }
            else
            {
              final String sEndpointInterface = aWebService.endpointInterface ();

              // Check if interface exists
              Class <?> aInterfaceClass = null;
              try
              {
                aInterfaceClass = Class.forName (sEndpointInterface);
              }
              catch (final Throwable t)
              {
                final String sMsg = "The endpoint interface class '" +
                                    sEndpointInterface +
                                    "' of implementation class '" +
                                    sImplementation +
                                    "' is invalid - " +
                                    t.getMessage ();
                s_aLogger.warn (sMsg);
                if (!bContinueOnError)
                  throw new Exception (sMsg);
              }

              if (aInterfaceClass != null)
              {
                if (!aInterfaceClass.isInterface ())
                {
                  final String sMsg = "The endpoint interface class '" +
                                      sEndpointInterface +
                                      "' of endpoint '" +
                                      sName +
                                      "' is not an interface!";
                  s_aLogger.warn (sMsg);
                  if (!bContinueOnError)
                    throw new Exception (sMsg);
                }
              }
            }
          }
        }
      }
    }
    return nTotalImplementationCount;
  }

  @Nonnegative
  public static int testIfAllSunJaxwsFilesAreValid (final boolean bContinueOnError) throws Exception
  {
    int ret = 0;
    ret += testIfAllSunJaxwsFilesAreValid ("src/main/resources/WEB-INF", bContinueOnError);
    ret += testIfAllSunJaxwsFilesAreValid ("src/main/webapp/WEB-INF", bContinueOnError);
    return ret;
  }

  public static void testIfAllSunJaxwsFilesAreValid () throws Exception
  {
    testIfAllSunJaxwsFilesAreValid (false);
  }
}
