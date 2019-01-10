/**
 * Copyright (C) 2014-2019 Philip Helger (www.helger.com)
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
package com.helger.commons.http;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsIterable;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.lang.IHasSize;
import com.helger.commons.state.EChange;
import com.helger.commons.state.IClearable;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * Abstracts HTTP header interface for external usage.<br>
 * Note: since version 9.1.8 (issue #11) the internal scheme changed and the
 * original case is stored. Older versions always stored the lower case header
 * names. The implications are that the casing of the first header is sustained.
 * So if the first API call uses name "Foo" and the second is "foo" they both
 * refer to the same header name and "Foo" will be the name that is retrieved.
 *
 * @author Philip Helger
 * @since 9.0.0
 */
@NotThreadSafe
public class HttpHeaderMap implements
                           IHasSize,
                           ICommonsIterable <Map.Entry <String, ICommonsList <String>>>,
                           ICloneable <HttpHeaderMap>,
                           IClearable,
                           Serializable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HttpHeaderMap.class);

  private final ICommonsOrderedMap <String, ICommonsList <String>> m_aHeaders = new CommonsLinkedHashMap <> ();

  /**
   * Default constructor.
   */
  public HttpHeaderMap ()
  {}

  /**
   * Copy constructor.
   *
   * @param aOther
   *        Map to copy from. May not be <code>null</code>.
   */
  public HttpHeaderMap (@Nonnull final HttpHeaderMap aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    m_aHeaders.putAll (aOther.m_aHeaders);
  }

  /**
   * Avoid having header values spanning multiple lines. This has been
   * deprecated by RFC 7230 and Jetty 9.3 refuses to parse these requests with
   * HTTP 400 by default.
   *
   * @param sValue
   *        The source header value. May be <code>null</code>.
   * @return The unified header value without \r, \n and \t. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static String getUnifiedValue (@Nullable final String sValue)
  {
    final StringBuilder aSB = new StringBuilder ();
    StringHelper.replaceMultipleTo (sValue, new char [] { '\r', '\n', '\t' }, ' ', aSB);
    return aSB.toString ();
  }

  /**
   * Remove all contained headers.
   *
   * @return {@link EChange}.
   */
  @Nonnull
  public EChange removeAll ()
  {
    return m_aHeaders.removeAll ();
  }

  @Nullable
  @ReturnsMutableObject
  private Map.Entry <String, ICommonsList <String>> _getHeaderEntry (@Nullable final String sName)
  {
    if (StringHelper.hasText (sName))
      for (final Map.Entry <String, ICommonsList <String>> aEntry : m_aHeaders.entrySet ())
        if (aEntry.getKey ().equalsIgnoreCase (sName))
          return aEntry;
    return null;
  }

  @Nullable
  @ReturnsMutableObject
  private ICommonsList <String> _getHeaderList (@Nullable final String sName)
  {
    final Map.Entry <String, ICommonsList <String>> aEntry = _getHeaderEntry (sName);
    return aEntry == null ? null : aEntry.getValue ();
  }

  @Nonnull
  @ReturnsMutableObject
  private ICommonsList <String> _getOrCreateHeaderList (@Nonnull @Nonempty final String sName)
  {
    ICommonsList <String> ret = _getHeaderList (sName);
    if (ret == null)
    {
      ret = new CommonsArrayList <> (2);
      m_aHeaders.put (sName, ret);
    }
    return ret;
  }

  @Nonnull
  private EChange _setHeader (@Nonnull @Nonempty final String sName, @Nonnull final String sValue)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    ValueEnforcer.notNull (sValue, "Value");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Setting HTTP header: '" + sName + "' = '" + sValue + "'");
    return _getOrCreateHeaderList (sName).set (sValue);
  }

  @Nonnull
  private EChange _addHeader (@Nonnull @Nonempty final String sName, @Nonnull final String sValue)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    ValueEnforcer.notNull (sValue, "Value");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Adding HTTP header: '" + sName + "' = '" + sValue + "'");
    return _getOrCreateHeaderList (sName).addObject (sValue);
  }

  /**
   * Set the passed header as is.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param sValue
   *        The value to be set. May be <code>null</code> in which case nothing
   *        happens.
   */
  public void setHeader (@Nonnull @Nonempty final String sName, @Nullable final String sValue)
  {
    if (sValue != null)
      _setHeader (sName, sValue);
  }

  /**
   * Add the passed header as is.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param sValue
   *        The value to be set. May be <code>null</code> in which case nothing
   *        happens.
   */
  public void addHeader (@Nonnull @Nonempty final String sName, @Nullable final String sValue)
  {
    if (sValue != null)
      _addHeader (sName, sValue);
  }

  @Nonnull
  public static String getDateTimeAsString (@Nonnull final ZonedDateTime aDT)
  {
    ValueEnforcer.notNull (aDT, "DateTime");

    // This method internally converts the date to UTC
    return PDTWebDateHelper.getAsStringRFC822 (aDT);
  }

  @Nonnull
  public static String getDateTimeAsString (@Nonnull final LocalDateTime aLDT)
  {
    ValueEnforcer.notNull (aLDT, "DateTime");

    // This method internally converts the date to UTC
    return PDTWebDateHelper.getAsStringRFC822 (aLDT);
  }

  /**
   * Set the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param nMillis
   *        The milliseconds to set as a date.
   */
  public void setDateHeader (@Nonnull @Nonempty final String sName, final long nMillis)
  {
    setDateHeader (sName, PDTFactory.createZonedDateTime (nMillis));
  }

  /**
   * Set the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param aLD
   *        The LocalDate to set as a date. The time is set to start of day. May
   *        not be <code>null</code>.
   */
  public void setDateHeader (@Nonnull @Nonempty final String sName, @Nonnull final LocalDate aLD)
  {
    setDateHeader (sName, PDTFactory.createZonedDateTime (aLD));
  }

  /**
   * Set the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param aLDT
   *        The LocalDateTime to set as a date. May not be <code>null</code>.
   */
  public void setDateHeader (@Nonnull @Nonempty final String sName, @Nonnull final LocalDateTime aLDT)
  {
    setDateHeader (sName, PDTFactory.createZonedDateTime (aLDT));
  }

  /**
   * Set the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param aDT
   *        The DateTime to set as a date. May not be <code>null</code>.
   */
  public void setDateHeader (@Nonnull @Nonempty final String sName, @Nonnull final ZonedDateTime aDT)
  {
    _setHeader (sName, getDateTimeAsString (aDT));
  }

  /**
   * Add the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param nMillis
   *        The milliseconds to set as a date.
   */
  public void addDateHeader (@Nonnull @Nonempty final String sName, final long nMillis)
  {
    addDateHeader (sName, PDTFactory.createZonedDateTime (nMillis));
  }

  /**
   * Add the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param aLD
   *        The LocalDate to set as a date. The time is set to start of day. May
   *        not be <code>null</code>.
   */
  public void addDateHeader (@Nonnull @Nonempty final String sName, @Nonnull final LocalDate aLD)
  {
    addDateHeader (sName, PDTFactory.createZonedDateTime (aLD));
  }

  /**
   * Add the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param aLDT
   *        The LocalDateTime to set as a date. May not be <code>null</code>.
   */
  public void addDateHeader (@Nonnull @Nonempty final String sName, @Nonnull final LocalDateTime aLDT)
  {
    addDateHeader (sName, PDTFactory.createZonedDateTime (aLDT));
  }

  /**
   * Add the passed header as a date header.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param aDT
   *        The DateTime to set as a date. May not be <code>null</code>.
   */
  public void addDateHeader (@Nonnull @Nonempty final String sName, @Nonnull final ZonedDateTime aDT)
  {
    _addHeader (sName, getDateTimeAsString (aDT));
  }

  /**
   * Set the passed header as a number.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param nValue
   *        The value to be set. May not be <code>null</code>.
   */
  public void setIntHeader (@Nonnull @Nonempty final String sName, final int nValue)
  {
    _setHeader (sName, Integer.toString (nValue));
  }

  /**
   * Add the passed header as a number.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param nValue
   *        The value to be set. May not be <code>null</code>.
   */
  public void addIntHeader (@Nonnull @Nonempty final String sName, final int nValue)
  {
    _addHeader (sName, Integer.toString (nValue));
  }

  /**
   * Set the passed header as a number.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param nValue
   *        The value to be set. May not be <code>null</code>.
   */
  public void setLongHeader (@Nonnull @Nonempty final String sName, final long nValue)
  {
    _setHeader (sName, Long.toString (nValue));
  }

  /**
   * Add the passed header as a number.
   *
   * @param sName
   *        Header name. May neither be <code>null</code> nor empty.
   * @param nValue
   *        The value to be set. May not be <code>null</code>.
   */
  public void addLongHeader (@Nonnull @Nonempty final String sName, final long nValue)
  {
    _addHeader (sName, Long.toString (nValue));
  }

  /**
   * Set all headers from the passed map. Existing headers with the same name
   * are overwritten. Existing headers are not changed!
   *
   * @param aOther
   *        The header map to add. May not be <code>null</code>.
   */
  public void setAllHeaders (@Nonnull final HttpHeaderMap aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    // Don't use putAll for case sensitivity
    for (final Map.Entry <String, ICommonsList <String>> aEntry : aOther.m_aHeaders.entrySet ())
    {
      final String sKey = aEntry.getKey ();
      removeHeaders (sKey);
      for (final String sValue : aEntry.getValue ())
        addHeader (sKey, sValue);
    }
  }

  /**
   * Add all headers from the passed map. Existing headers with the same name
   * are extended.
   *
   * @param aOther
   *        The header map to add. May not be <code>null</code>.
   */
  public void addAllHeaders (@Nonnull final HttpHeaderMap aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    for (final Map.Entry <String, ICommonsList <String>> aEntry : aOther.m_aHeaders.entrySet ())
    {
      final String sKey = aEntry.getKey ();
      for (final String sValue : aEntry.getValue ())
        addHeader (sKey, sValue);
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, ICommonsList <String>> getAllHeaders ()
  {
    return m_aHeaders.getClone ();
  }

  /**
   * @return A copy of all contained header names. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedSet <String> getAllHeaderNames ()
  {
    return m_aHeaders.copyOfKeySet ();
  }

  /**
   * Get all header values of a certain header name
   *
   * @param sName
   *        The name to be searched.
   * @return The list with all matching values. Never <code>null</code> but
   *         maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAllHeaderValues (@Nullable final String sName)
  {
    if (StringHelper.hasText (sName))
    {
      final ICommonsList <String> aValues = _getHeaderList (sName);
      if (aValues != null)
        return aValues.getClone ();
    }
    return new CommonsArrayList <> ();
  }

  /**
   * Get the first header value of a certain header name
   *
   * @param sName
   *        The name to be searched.
   * @return The first matching value or <code>null</code>.
   */
  @Nullable
  public String getFirstHeaderValue (@Nullable final String sName)
  {
    if (StringHelper.hasText (sName))
    {
      final ICommonsList <String> aValues = _getHeaderList (sName);
      if (aValues != null)
        return aValues.getFirst ();
    }
    return null;
  }

  /**
   * Get the header value as a combination of all contained values
   *
   * @param sName
   *        The header name to retrieve. May be <code>null</code>.
   * @param sDelimiter
   *        The delimiter to be used. May not be <code>null</code>.
   * @return <code>null</code> if no such header is contained.
   */
  @Nullable
  public String getHeaderCombined (@Nullable final String sName, @Nonnull final String sDelimiter)
  {
    if (StringHelper.hasText (sName))
    {
      final ICommonsList <String> aValues = _getHeaderList (sName);
      if (aValues != null)
        return StringHelper.getImploded (sDelimiter, aValues);
    }
    return null;
  }

  public boolean containsHeaders (@Nullable final String sName)
  {
    if (StringHelper.hasNoText (sName))
      return false;
    return _getHeaderList (sName) != null;
  }

  /**
   * Remove all header values where the name matches the provided filter.
   *
   * @param aNameFilter
   *        The name filter to be applied. May not be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  public EChange removeHeadersIf (@Nonnull final Predicate <? super String> aNameFilter)
  {
    return m_aHeaders.removeIfKey (aNameFilter);
  }

  /**
   * Remove all header values with the provided name
   *
   * @param sName
   *        The name to be removed. May be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  public EChange removeHeaders (@Nullable final String sName)
  {
    if (StringHelper.hasNoText (sName))
      return EChange.UNCHANGED;

    final Map.Entry <String, ICommonsList <String>> aEntry = _getHeaderEntry (sName);
    if (aEntry != null)
      return m_aHeaders.removeObject (aEntry.getKey ());

    return EChange.UNCHANGED;
  }

  @Nonnull
  public EChange removeHeader (@Nullable final String sName, @Nullable final String sValue)
  {
    final Map.Entry <String, ICommonsList <String>> aEntry = _getHeaderEntry (sName);
    final boolean bRemoved = aEntry != null && aEntry.getValue ().remove (sValue);
    if (bRemoved && aEntry.getValue ().isEmpty ())
    {
      // If the last value was removed, remove the whole header
      m_aHeaders.remove (aEntry.getKey ());
    }

    return EChange.valueOf (bRemoved);
  }

  @Nonnull
  public Iterator <Map.Entry <String, ICommonsList <String>>> iterator ()
  {
    return m_aHeaders.entrySet ().iterator ();
  }

  @Nonnegative
  public int size ()
  {
    return m_aHeaders.size ();
  }

  public boolean isEmpty ()
  {
    return m_aHeaders.isEmpty ();
  }

  /**
   * Invoke the provided consumer for every name/value pair.
   *
   * @param aConsumer
   *        Consumer to be invoked. May not be <code>null</code>.
   */
  public void forEachSingleHeader (@Nonnull final BiConsumer <? super String, ? super String> aConsumer)
  {
    for (final Map.Entry <String, ICommonsList <String>> aEntry : m_aHeaders.entrySet ())
    {
      final String sKey = aEntry.getKey ();
      for (final String sValue : aEntry.getValue ())
        aConsumer.accept (sKey, sValue);
    }
  }

  public void forEachHeaderLine (@Nonnull final Consumer <? super String> aConsumer)
  {
    for (final Map.Entry <String, ICommonsList <String>> aEntry : m_aHeaders.entrySet ())
    {
      final String sKey = aEntry.getKey ();
      for (final String sValue : aEntry.getValue ())
        aConsumer.accept (sKey + ": " + sValue);
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAllHeaderLines ()
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    forEachHeaderLine (ret::add);
    return ret;
  }

  public void setContentLength (final long nLength)
  {
    _setHeader (CHttpHeader.CONTENT_LENGTH, Long.toString (nLength));
  }

  public void setContentType (@Nonnull final String sContentType)
  {
    _setHeader (CHttpHeader.CONTENT_TYPE, sContentType);
  }

  @Nonnull
  @ReturnsMutableCopy
  public HttpHeaderMap getClone ()
  {
    return new HttpHeaderMap (this);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final HttpHeaderMap rhs = (HttpHeaderMap) o;
    return m_aHeaders.equals (rhs.m_aHeaders);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aHeaders).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Headers", m_aHeaders).getToString ();
  }
}
