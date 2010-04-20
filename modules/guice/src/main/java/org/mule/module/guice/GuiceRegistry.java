/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.registry.RegistrationException;
import org.mule.registry.AbstractRegistry;

import com.google.inject.Binding;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The internal Mule interface for retreiving objects from a Guice injector.  This registry is read-only since all
 * objects should be configured by {@link com.google.inject.Module} objects.  The lifecycle of objects will be
 * managed by Mule since Guice does not provide lifecycle support.
 *
 * To create modules extend the {@link org.mule.module.guice.AbstractMuleGuiceModule} since it provides hooks and helpers for
 * working with Mule configuration.  Any modules independent of Mule can just extend the Guice {@link com.google.inject.AbstractModule}
 * as normal.
 *
 * Mule will discover modules on the classpath, if you need to configure a module before passing it to the Guice injector you
 * need to implement a {@link org.mule.module.guice.GuiceModuleFactory} for your module.
 *
 * @see org.mule.module.guice.AbstractMuleGuiceModule
 * @see org.mule.module.guice.GuiceModuleFactory
 */
public class GuiceRegistry extends AbstractRegistry
{
    private Injector injector = null;

    public GuiceRegistry(MuleContext muleContext)
    {
        super("guice", muleContext);
    }


    GuiceRegistry(Injector injector, MuleContext muleContext)
    {
        this(muleContext);
        this.injector = injector;
    }

    protected void doInitialise() throws InitialisationException
    {
        //do nothing
        try
        {
            getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);
        }
        catch (LifecycleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected void doDispose()
    {
        try
        {
            getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);
        }
        catch (LifecycleException e)
        {
            logger.error("Failed to shut down Guice registry cleanly: " + getRegistryId(), e);
        }
    }

    public <T> T lookupObject(String key)
    {
        return null;
    }

    public <T> T get(String key)
    {
        return null;
    }

    public <T> Map<String, T> lookupByType(Class<T> type)
    {
        return null;
    }

    public <T> Collection<T> lookupObjects(Class<T> type)
    {
        try
        {
            List<Binding<T>> bindings = injector.findBindingsByType(TypeLiteral.get(type));
            if(bindings!=null && bindings.size()>0)
            {
                List<T> list = new ArrayList<T>(bindings.size());
                for (Binding<T> binding : bindings)
                {
                    list.add(binding.getProvider().get());
                }
                return list;
            }
            return Collections.emptyList();
        }
        catch (ConfigurationException e)
        {
            return Collections.emptyList();
        }
    }

    public void registerObject(String key, Object value) throws RegistrationException
    {
        throw new UnsupportedOperationException("registerObject");
    }

    public void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        throw new UnsupportedOperationException("registerObject");
    }

    public void registerObjects(Map objects) throws RegistrationException
    {
        throw new UnsupportedOperationException("registerObjects");
    }

    public void unregisterObject(String key) throws RegistrationException
    {
        throw new UnsupportedOperationException("unregisterObject");
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        throw new UnsupportedOperationException("unregisterObject");
    }

    public boolean isReadOnly()
    {
        return true;
    }

    public boolean isRemote()
    {
        return false;
    }
}
