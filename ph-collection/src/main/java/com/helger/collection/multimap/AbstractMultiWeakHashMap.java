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
package com.helger.collection.multimap;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsWeakHashMap;
import com.helger.commons.collection.impl.ICommonsCollection;

/**
 * Abstract multi map based on {@link java.util.WeakHashMap}.
 *
 * @author Philip Helger
 * @param <KEYTYPE>
 *        key type
 * @param <VALUETYPE>
 *        value type
 * @param <COLLTYPE>
 *        contained collection type
 */
@NotThreadSafe
public abstract class AbstractMultiWeakHashMap <KEYTYPE, VALUETYPE, COLLTYPE extends ICommonsCollection <VALUETYPE>>
                                               extends
                                               CommonsWeakHashMap <KEYTYPE, COLLTYPE> implements
                                               IMultiMap <KEYTYPE, VALUETYPE, COLLTYPE>
{
  public AbstractMultiWeakHashMap ()
  {}

  public AbstractMultiWeakHashMap (@Nonnull final KEYTYPE aKey, @Nullable final VALUETYPE aValue)
  {
    putSingle (aKey, aValue);
  }

  public AbstractMultiWeakHashMap (@Nonnull final KEYTYPE aKey, @Nullable final COLLTYPE aCollection)
  {
    put (aKey, aCollection);
  }

  public AbstractMultiWeakHashMap (@Nullable final Map <? extends KEYTYPE, ? extends COLLTYPE> aCont)
  {
    if (aCont != null)
      putAll (aCont);
  }

  @Nonnull
  @ReturnsMutableCopy
  protected abstract COLLTYPE createNewCollection ();

  @Nonnull
  public COLLTYPE getOrCreate (@Nullable final KEYTYPE aKey)
  {
    return computeIfAbsent (aKey, k -> createNewCollection ());
  }
}
