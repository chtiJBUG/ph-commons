package com.helger.xml.serialize.write;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.charset.CharsetHelper;
import com.helger.commons.collection.NonBlockingStack;
import com.helger.commons.collection.iterate.CombinedIterator;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.xml.EXMLVersion;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * A special stream writer, that ensures that special XML characters are handled
 * correctly.<br>
 * See https://github.com/javaee/jaxb-v2/issues/614<br>
 * See https://github.com/javaee/jaxb-v2/issues/960
 *
 * @author Philip Helger
 * @since 9.0.0
 */
public class SafeXMLStreamWriter implements XMLStreamWriter
{
  private static final class ElementState
  {
    private final String m_sPrefix;
    private final String m_sLocalName;
    private final EXMLSerializeBracketMode m_eBracketMode;

    public ElementState (@Nullable final String sPrefix,
                         @Nonnull final String sLocalName,
                         @Nonnull final EXMLSerializeBracketMode eBracketMode)
    {
      m_sPrefix = sPrefix;
      m_sLocalName = sLocalName;
      m_eBracketMode = eBracketMode;
    }
  }

  private static final class MultiNamespaceContext implements NamespaceContext
  {
    private final MapBasedNamespaceContext m_aInternalContext = new MapBasedNamespaceContext ();
    private NamespaceContext m_aUserContext;

    public MultiNamespaceContext ()
    {}

    @Override
    @Nonnull
    public String getNamespaceURI (@Nonnull final String prefix)
    {
      String ret = m_aInternalContext.getNamespaceURI (prefix);
      if ((ret == null || XMLConstants.NULL_NS_URI.equals (ret)) && m_aUserContext != null)
        ret = m_aUserContext.getNamespaceURI (prefix);
      return ret;
    }

    @Override
    @Nullable
    public String getPrefix (@Nonnull final String uri)
    {
      String ret = m_aInternalContext.getPrefix (uri);
      if (ret == null && m_aUserContext != null)
        ret = m_aUserContext.getPrefix (uri);
      return ret;
    }

    @Override
    @Nonnull
    public Iterator getPrefixes (@Nonnull final String uri)
    {
      final Iterator <?> aIter1 = m_aInternalContext.getPrefixes (uri);
      if (m_aUserContext == null)
        return aIter1;

      final Iterator <?> aIter2 = m_aUserContext.getPrefixes (uri);
      return new CombinedIterator <> (aIter1, aIter2);
    }

    @Override
    public String toString ()
    {
      return new ToStringGenerator (this).append ("InternalContext", m_aInternalContext)
                                         .append ("UserContext", m_aUserContext)
                                         .getToString ();
    }
  }

  private static final Logger s_aLogger = LoggerFactory.getLogger (SafeXMLStreamWriter.class);

  private final XMLEmitter m_aEmitter;
  private final MultiNamespaceContext m_aNamespaceContext = new MultiNamespaceContext ();
  private final NonBlockingStack <ElementState> m_aElementStateStack = new NonBlockingStack <> ();
  private boolean m_bInElementStart = false;
  private boolean m_bDebugMode = false;

  public SafeXMLStreamWriter (@Nonnull final XMLEmitter aEmitter)
  {
    ValueEnforcer.notNull (aEmitter, "Emitter");
    m_aEmitter = aEmitter;
  }

  /**
   * @return <code>true</code> if debug mode is enabled, <code>false</code> if
   *         it is disabled. By default it is disabled.
   * @see #setDebugMode(boolean)
   */
  public final boolean isDebugMode ()
  {
    return m_bDebugMode;
  }

  /**
   * Enable or disable debug mode
   *
   * @param bDebugMode
   *        <code>true</code> to enable debug mode, <code>false</code> to
   *        disable it.
   * @return this for chaining
   * @see #isDebugMode()
   */
  @Nonnull
  public final SafeXMLStreamWriter setDebugMode (final boolean bDebugMode)
  {
    m_bDebugMode = bDebugMode;
    return this;
  }

  @OverrideOnDemand
  protected void debug (@Nonnull final Supplier <String> aSupplier)
  {
    if (m_bDebugMode)
      s_aLogger.info (aSupplier.get ());
  }

  @Nonnull
  private IXMLWriterSettings _getSettings ()
  {
    return m_aEmitter.getXMLWriterSettings ();
  }

  public void writeStartDocument () throws XMLStreamException
  {
    writeStartDocument (_getSettings ().getCharset (), _getSettings ().getXMLVersion ());
  }

  public void writeStartDocument (@Nullable final String sVersion) throws XMLStreamException
  {
    writeStartDocument (_getSettings ().getCharset (), EXMLVersion.getFromVersionOrNull (sVersion));
  }

  public void writeStartDocument (@Nullable final String sEncoding,
                                  @Nullable final String sVersion) throws XMLStreamException
  {
    writeStartDocument (CharsetHelper.getCharsetFromName (sEncoding), EXMLVersion.getFromVersionOrNull (sVersion));
  }

  public void writeStartDocument (@Nonnull final Charset aEncoding, @Nonnull final EXMLVersion eVersion)
  {
    debug ( () -> "writeStartDocument (" + aEncoding + ", " + eVersion + ")");
    m_aEmitter.onXMLDeclaration (eVersion, aEncoding.name (), false);
  }

  public void writeDTD (@Nonnull final String sDTD) throws XMLStreamException
  {
    debug ( () -> "writeDTD (" + sDTD + ")");
    m_aEmitter.onDTD (sDTD);
  }

  public void writeStartElement (final String sLocalName) throws XMLStreamException
  {
    writeStartElement (null, sLocalName);
  }

  public void writeStartElement (final String sNamespaceURI, final String sLocalName) throws XMLStreamException
  {
    writeStartElement (null, sLocalName, sNamespaceURI);
  }

  public void writeStartElement (final String sPrefix,
                                 final String sLocalName,
                                 final String sNamespaceURI) throws XMLStreamException
  {
    debug ( () -> "writeStartElement (" + sPrefix + ", " + sLocalName + ", " + sNamespaceURI + ")");
    _elementStartClose ();
    m_aEmitter.elementStartOpen (sPrefix, sLocalName);
    m_aElementStateStack.push (new ElementState (sPrefix, sLocalName, EXMLSerializeBracketMode.OPEN_CLOSE));
    m_bInElementStart = true;
  }

  public void writeEmptyElement (final String sLocalName) throws XMLStreamException
  {
    writeEmptyElement (null, sLocalName);
  }

  public void writeEmptyElement (final String sNamespaceURI, final String sLocalName) throws XMLStreamException
  {
    writeStartElement (null, sLocalName, sNamespaceURI);
  }

  public void writeEmptyElement (final String sPrefix,
                                 final String sLocalName,
                                 final String sNamespaceURI) throws XMLStreamException
  {
    debug ( () -> "writeEmptyElement (" + sPrefix + ", " + sLocalName + ", " + sNamespaceURI + ")");
    _elementStartClose ();
    m_aEmitter.elementStartOpen (sPrefix, sLocalName);
    m_aElementStateStack.push (new ElementState (sPrefix, sLocalName, EXMLSerializeBracketMode.SELF_CLOSED));
    m_bInElementStart = true;
  }

  public void writeAttribute (final String sLocalName, final String sValue) throws XMLStreamException
  {
    writeAttribute (null, sLocalName, sValue);
  }

  public void writeAttribute (final String sPrefix,
                              final String sNamespaceURI,
                              final String sLocalName,
                              final String sValue) throws XMLStreamException
  {
    debug ( () -> "writeAttribute (" + sPrefix + ", " + sNamespaceURI + ", " + sLocalName + ", " + sValue + ")");
    if (!m_bInElementStart)
      throw new IllegalStateException ("No element open");
    m_aEmitter.elementAttr (sPrefix, sLocalName, sValue);
  }

  public void writeAttribute (final String sNamespaceURI,
                              final String sLocalName,
                              final String sValue) throws XMLStreamException
  {
    writeAttribute (null, sNamespaceURI, sLocalName, sValue);
  }

  public void writeNamespace (@Nullable final String sPrefix,
                              @Nonnull final String sNamespaceURI) throws XMLStreamException
  {
    debug ( () -> "writeNamespace (" + sPrefix + ", " + sNamespaceURI + ")");
    if (!m_bInElementStart)
      throw new IllegalStateException ("No element open");

    final boolean bIsDefault = sPrefix == null || sPrefix.equals ("") || sPrefix.equals ("xmlns");
    if (bIsDefault)
      m_aNamespaceContext.m_aInternalContext.addDefaultNamespaceURI (sNamespaceURI);
    else
      m_aNamespaceContext.m_aInternalContext.addMapping (sPrefix, sNamespaceURI);
  }

  public void writeDefaultNamespace (final String sNamespaceURI) throws XMLStreamException
  {
    writeNamespace (null, sNamespaceURI);
  }

  private void _elementStartClose ()
  {
    if (m_bInElementStart)
    {
      m_aEmitter.elementStartClose (m_aElementStateStack.peek ().m_eBracketMode);
      m_bInElementStart = false;
    }
  }

  public void writeEndElement () throws XMLStreamException
  {
    debug ( () -> "writeEndElement ()");
    _elementStartClose ();
    final ElementState eState = m_aElementStateStack.pop ();
    m_aEmitter.onElementEnd (eState.m_sPrefix, eState.m_sLocalName, eState.m_eBracketMode);
  }

  public void writeComment (final String sData) throws XMLStreamException
  {
    debug ( () -> "writeComment (" + sData + ")");
    _elementStartClose ();
    m_aEmitter.onComment (sData);
  }

  public void writeCData (final String sData) throws XMLStreamException
  {
    debug ( () -> "writeCData (" + sData + ")");
    _elementStartClose ();
    m_aEmitter.onCDATA (sData);
  }

  public void writeEntityRef (final String sName) throws XMLStreamException
  {
    debug ( () -> "writeEntityRef (" + sName + ")");
    _elementStartClose ();
    m_aEmitter.onEntityReference (sName);
  }

  public void writeCharacters (final String sText) throws XMLStreamException
  {
    debug ( () -> "writeCharacters (" + sText + ")");
    _elementStartClose ();
    m_aEmitter.onText (sText);
  }

  public void writeCharacters (final char [] aText, final int nStart, final int nLen) throws XMLStreamException
  {
    debug ( () -> "writeCharacters (" + String.valueOf (aText) + ", " + nStart + ", " + nLen + ")");
    _elementStartClose ();
    m_aEmitter.onText (aText, nStart, nLen);
  }

  public void writeProcessingInstruction (@Nonnull final String sTarget) throws XMLStreamException
  {
    writeProcessingInstruction (sTarget, null);
  }

  public void writeProcessingInstruction (@Nonnull final String sTarget,
                                          @Nullable final String sData) throws XMLStreamException
  {
    debug ( () -> "writeProcessingInstruction (" + sTarget + ", " + sData + ")");
    _elementStartClose ();
    m_aEmitter.onProcessingInstruction (sTarget, sData);
  }

  public void writeEndDocument () throws XMLStreamException
  {
    debug ( () -> "writeEndDocument ()");
    _elementStartClose ();

    if (m_aElementStateStack.isNotEmpty ())
      throw new IllegalStateException ("Internal inconsistency - element stack is not empty: " + m_aElementStateStack);
  }

  public void flush () throws XMLStreamException
  {
    debug ( () -> "flush ()");
    try
    {
      m_aEmitter.flush ();
    }
    catch (final IOException ex)
    {
      throw new XMLStreamException ("Error flushing XML emitter", ex);
    }
  }

  public void close () throws XMLStreamException
  {
    debug ( () -> "close ()");
    try
    {
      m_aEmitter.close ();
    }
    catch (final IOException ex)
    {
      throw new XMLStreamException ("Error closing XML emitter", ex);
    }
  }

  public String getPrefix (@Nonnull final String sUri) throws XMLStreamException
  {
    debug ( () -> "getPrefix (" + sUri + ")");
    return m_aNamespaceContext.getPrefix (sUri);
  }

  public void setPrefix (@Nonnull final String sPrefix, @Nonnull final String sUri) throws XMLStreamException
  {
    debug ( () -> "setPrefix (" + sPrefix + ", " + sUri + ")");
    m_aNamespaceContext.m_aInternalContext.addMapping (sPrefix, sUri);
  }

  public void setDefaultNamespace (@Nonnull final String sUri) throws XMLStreamException
  {
    debug ( () -> "setDefaultNamespace (" + sUri + ")");
    m_aNamespaceContext.m_aInternalContext.addDefaultNamespaceURI (sUri);
  }

  public void setNamespaceContext (@Nullable final NamespaceContext aContext) throws XMLStreamException
  {
    debug ( () -> "setNamespaceContext (" + aContext + ")");
    m_aNamespaceContext.m_aUserContext = aContext;
  }

  @Nonnull
  public NamespaceContext getNamespaceContext ()
  {
    debug ( () -> "getNamespaceContext ()");
    return m_aNamespaceContext;
  }

  @Nullable
  public Object getProperty (final String sName) throws IllegalArgumentException
  {
    debug ( () -> "getProperty (" + sName + ") - UNSUPPORTED");
    return null;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Emitter", m_aEmitter)
                                       .append ("NamespaceContext", m_aNamespaceContext)
                                       .append ("ElementStateStack", m_aElementStateStack)
                                       .append ("InElementStart", m_bInElementStart)
                                       .append ("DebugMode", m_bDebugMode)
                                       .getToString ();
  }

  @Nonnull
  public static SafeXMLStreamWriter create (@Nonnull @WillCloseWhenClosed final Writer aWriter,
                                            @Nonnull final IXMLWriterSettings aSettings)
  {
    return new SafeXMLStreamWriter (new XMLEmitter (aWriter, aSettings));
  }

  @Nonnull
  public static SafeXMLStreamWriter create (@Nonnull @WillCloseWhenClosed final OutputStream aOS,
                                            @Nonnull final IXMLWriterSettings aSettings)
  {
    ValueEnforcer.notNull (aOS, "OutputStream");
    return create (new OutputStreamWriter (aOS, aSettings.getCharset ()), aSettings);
  }

  @Nonnull
  public static SafeXMLStreamWriter create (@Nonnull final File aFile, @Nonnull final IXMLWriterSettings aSettings)
  {
    ValueEnforcer.notNull (aFile, "File");
    return create (FileHelper.getOutputStream (aFile), aSettings);
  }

  @Nonnull
  public static SafeXMLStreamWriter create (@Nonnull final Path aPath, @Nonnull final IXMLWriterSettings aSettings)
  {
    ValueEnforcer.notNull (aPath, "Path");
    return create (aPath.toFile (), aSettings);
  }
}
