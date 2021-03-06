/**
 * Copyright (C) 2014-2020 Philip Helger (www.helger.com)
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
package com.helger.commons.io.resource.inmemory;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.string.StringHelper;

/**
 * An in-memory {@link IReadableResource} based on a {@link String} which is
 * converted to a byte array with the provided charset.
 *
 * @author Philip Helger
 */
public class ReadableResourceString extends ReadableResourceByteArray
{
  public ReadableResourceString (@Nonnull final String sString, @Nonnull final Charset aCharset)
  {
    this (null, sString, aCharset);
  }

  public ReadableResourceString (@Nullable final String sResourceID,
                                 @Nonnull final String sString,
                                 @Nonnull final Charset aCharset)
  {
    super (StringHelper.hasText (sResourceID) ? sResourceID : "String[" + aCharset.name () + "]",
           sString.getBytes (aCharset));
  }
}
