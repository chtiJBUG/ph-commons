/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.security.keystore;

import java.security.Key;

import javax.annotation.Nullable;
import javax.xml.crypto.KeySelectorResult;

import com.helger.commons.string.ToStringGenerator;

/**
 * Special implements of {@link KeySelectorResult} with a constant, nullable
 * key.
 *
 * @author Philip Helger
 */
public final class ConstantKeySelectorResult implements KeySelectorResult
{
  private final Key m_aKey;

  public ConstantKeySelectorResult (@Nullable final Key aKey)
  {
    m_aKey = aKey;
  }

  @Nullable
  public Key getKey ()
  {
    return m_aKey;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("key", m_aKey).toString ();
  }
}
