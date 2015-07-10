/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.commons.xml.ls;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import javax.xml.XMLConstants;

import org.apache.felix.framework.FrameworkFactory;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.w3c.dom.ls.LSInput;

import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.resource.URLResource;

/**
 * Test class for class {@link OSGIBundleLSResourceResolver}.
 *
 * @author Philip Helger
 */
public final class OSGIBundleLSResourceResolverTest
{
  @Test
  public void testOSGIBundle () throws BundleException
  {
    LSInput aRes;

    // Initializing Apache Felix as OSGI container is required to get the
    // "bundle" URL protocol installed correctly
    // Otherwise the first call would end up as a "file" resource ;-)
    final Framework aOSGI = new FrameworkFactory ().newFramework (new HashMap <String, String> ());
    aOSGI.start ();
    try
    {
      // Bundle 0 is the org.apache.felix.framework bundle
      final Bundle b = aOSGI.getBundleContext ().getBundle (0);
      assertNotNull (b);
      assertTrue (b.getState () == Bundle.ACTIVE);

      // No leading slash is important as the ClassLoader is used!
      assertNotNull (b.getResource ("org/apache/felix/framework/util/Mutex.class"));

      final OSGIBundleLSResourceResolver aRR = new OSGIBundleLSResourceResolver (aOSGI.getBundleContext ());

      // No class loader
      aRes = aRR.resolveResource (XMLConstants.W3C_XML_SCHEMA_NS_URI,
                                  null,
                                  null,
                                  "../Felix.class",
                                  "bundle://0.0:1/org/apache/felix/framework/util/Mutex.class");
      assertTrue (aRes instanceof ResourceLSInput);
      final IHasInputStream aISP = ((ResourceLSInput) aRes).getInputStreamProvider ();
      assertTrue (aISP instanceof URLResource);
      // Path maybe a "jar:file:" resource
      assertTrue (((URLResource) aISP).getPath ().endsWith ("org/apache/felix/framework/Felix.class"));
    }
    finally
    {
      aOSGI.stop ();
    }
  }
}
