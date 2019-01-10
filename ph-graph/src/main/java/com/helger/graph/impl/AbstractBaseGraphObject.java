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
package com.helger.graph.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.attr.AttributeContainerAny;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.graph.IMutableBaseGraphObject;

/**
 * Base class for graph nodes and graph relations.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public abstract class AbstractBaseGraphObject implements IMutableBaseGraphObject
{
  private final String m_sID;
  private final AttributeContainerAny <String> m_aAttrs = new AttributeContainerAny <> ();

  /**
   * Constructor
   *
   * @param sID
   *        If <code>null</code> a new ID is generated by the
   *        {@link GraphObjectIDFactory}.
   */
  public AbstractBaseGraphObject (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      m_sID = GraphObjectIDFactory.createNewGraphObjectID ();
    else
      m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @ReturnsMutableObject
  public final AttributeContainerAny <String> attrs ()
  {
    return m_aAttrs;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final AbstractBaseGraphObject rhs = (AbstractBaseGraphObject) o;
    return m_sID.equals (rhs.m_sID) && m_aAttrs.equals (rhs.m_aAttrs);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).append (m_aAttrs).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID).append ("Attrs", m_aAttrs).getToString ();
  }
}
