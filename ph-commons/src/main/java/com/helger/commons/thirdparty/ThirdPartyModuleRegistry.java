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
package com.helger.commons.thirdparty;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.Singleton;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.state.EChange;

/**
 * This class manages all registered third party modules
 *
 * @author Philip Helger
 */
@ThreadSafe
@Singleton
public final class ThirdPartyModuleRegistry
{
  private static final class SingletonHolder
  {
    static final ThirdPartyModuleRegistry s_aInstance = new ThirdPartyModuleRegistry ();
  }

  private static final Logger s_aLogger = LoggerFactory.getLogger (ThirdPartyModuleRegistry.class);
  private static boolean s_bDefaultInstantiated = false;

  private final ReadWriteLock m_aRWLock = new ReentrantReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final Set <IThirdPartyModule> m_aModules = new LinkedHashSet <IThirdPartyModule> ();

  private ThirdPartyModuleRegistry ()
  {
    reinitialize ();
  }

  public static boolean isInstantiated ()
  {
    return s_bDefaultInstantiated;
  }

  @Nonnull
  public static ThirdPartyModuleRegistry getInstance ()
  {
    final ThirdPartyModuleRegistry ret = SingletonHolder.s_aInstance;
    s_bDefaultInstantiated = true;
    return ret;
  }

  @Nonnull
  public EChange registerThirdPartyModule (@Nonnull final IThirdPartyModule aModule)
  {
    ValueEnforcer.notNull (aModule, "Module");

    m_aRWLock.writeLock ().lock ();
    try
    {
      return EChange.valueOf (m_aModules.add (aModule));
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <IThirdPartyModule> getAllRegisteredThirdPartyModules ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return CollectionHelper.newSet (m_aModules);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnegative
  public int getRegisteredThirdPartyModuleCount ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aModules.size ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  public void reinitialize ()
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aModules.clear ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    // Load all SPI implementations
    for (final IThirdPartyModuleProviderSPI aTPM : ServiceLoaderHelper.getAllSPIImplementations (IThirdPartyModuleProviderSPI.class))
    {
      final IThirdPartyModule [] aModules = aTPM.getAllThirdPartyModules ();
      if (aModules != null)
        for (final IThirdPartyModule aModule : aModules)
          registerThirdPartyModule (aModule);
    }

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Reinitialized " + ThirdPartyModuleRegistry.class.getName ());
  }
}