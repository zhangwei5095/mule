/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.junit.Assert.fail;
import org.mule.functional.functional.FlowAssert;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.container.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.FilteringContainerClassLoader;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.schedule.Scheduler;
import org.mule.runtime.core.api.schedule.Schedulers;
import org.mule.runtime.core.component.AbstractJavaComponent;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.construct.AbstractPipeline;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;

/**
 * A base test case for tests that initialize Mule using a configuration file. The
 * default configuration builder used is SpringXmlConfigurationBuilder. To use this
 * test case, ensure you have the mule-modules-builders JAR file on your classpath.
 * To use a different builder, just overload the <code>getBuilder()</code> method of
 * this class to return the type of builder you want to use with your test.
 */
public abstract class FunctionalTestCase extends AbstractMuleContextTestCase
{

    private FilteringContainerClassLoader executionClassLoader;

    public FunctionalTestCase()
    {
        super();
        // A functional test case that starts up the management context by default.
        setStartContext(true);
    }

    /**
     * @return
     * @deprecated use getConfigFile instead.
     */
    @Deprecated
    protected String getConfigResources()
    {
        return null;
    }


    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        String configResources = getConfigResources();
        if (configResources != null)
        {
            return new SpringXmlConfigurationBuilder(configResources);
        }
        configResources = getConfigFile();
        if (configResources != null)
        {
            if (configResources.contains(","))
            {
                throw new RuntimeException("Do not use this method when the config is composed of several files. Use getConfigFiles method instead.");
            }
            return new SpringXmlConfigurationBuilder(configResources);
        }
        String[] multipleConfigResources = getConfigFiles();
        return new SpringXmlConfigurationBuilder(multipleConfigResources);
    }

    /**
     * @return a single file that defines a mule application configuration
     */
    protected String getConfigFile()
    {
        return null;
    }

    /**
     * @return a several files that define a mule application configuration
     */
    protected String[] getConfigFiles()
    {
        return null;
    }

    /**
     * Returns an instance of the service's component object. Note that depending on
     * the type of ObjectFactory used for the component, this may create a new
     * instance of the object. If you plan to set properties on the returned object,
     * make sure your component is declared as a singleton, otherwise this will not
     * work.
     */
    protected Object getComponent(String serviceName) throws Exception
    {
        final FlowConstruct flowConstruct = muleContext.getRegistry().lookupObject(serviceName);

        if (flowConstruct != null)
        {
            return getComponent(flowConstruct);
        }
        else
        {
            throw new RegistrationException(MessageFactory.createStaticMessage("Service " + serviceName
                                                                               + " not found in Registry"));
        }
    }

    /**
     * Returns an instance of the service's component object. Note that depending on
     * the type of ObjectFactory used for the component, this may create a new
     * instance of the object. If you plan to set properties on the returned object,
     * make sure your component is declared as a singleton, otherwise this will not
     * work.
     */
    protected Object getComponent(FlowConstruct flowConstruct) throws Exception
    {
        if (flowConstruct instanceof AbstractPipeline)
        {
            AbstractPipeline flow = (AbstractPipeline) flowConstruct;
            // Retrieve the first component
            for (MessageProcessor processor : flow.getMessageProcessors())
            {
                if (processor instanceof Component)
                {
                    return getComponentObject(((Component) processor));
                }
            }
        }

        throw new RegistrationException(
            MessageFactory.createStaticMessage("Can't get component from flow construct "
                                               + flowConstruct.getName()));
    }

    /**
     * A convenience method to get a type-safe reference to the FunctionTestComponent
     * 
     * @param serviceName service name as declared in the config
     * @return test component
     * @since 2.2
     * @see FunctionalTestComponent
     */
    protected FunctionalTestComponent getFunctionalTestComponent(String serviceName) throws Exception
    {
        return (FunctionalTestComponent) getComponent(serviceName);
    }

    protected FlowConstruct getFlowConstruct(String flowName) throws Exception
    {
        return muleContext.getRegistry().lookupFlowConstruct(flowName);
    }

    //TODO(pablo.kraan): move this constant to another place in container module
    public static final Set<String> BOOT_PACKAGES = ImmutableSet.of(
            "java.", "javax.", "org.apache.xerces", "org.mule.mvel2",
            "org.apache.logging.log4j", "org.slf4j", "org.apache.commons.logging", "org.apache.log4j",
            //TODO(pablo.kraan): check why these apckages are required
            "org.dom4j", "org.w3c.dom", "com.sun", "sun", "org.springframework"
    );

    public static final Set<String> SYSTEM_PACKAGES = ImmutableSet.of(
            "org.mule.runtime", "com.mulesoft.mule.runtime"
    );

    @Override
    protected ClassLoader getExecutionClassLoader()
    {
        if (executionClassLoader == null)
        {
            final Set<String> parentOnlyPackages = new HashSet<>(BOOT_PACKAGES);
            parentOnlyPackages.addAll(SYSTEM_PACKAGES);
            final MuleClassLoaderLookupPolicy containerLookupPolicy = new MuleClassLoaderLookupPolicy(Collections.emptyMap(), parentOnlyPackages);
            final ArtifactClassLoader containerClassLoader = new MuleArtifactClassLoader("mule", new URL[0], getClass().getClassLoader(), containerLookupPolicy);

            executionClassLoader = new FilteringContainerClassLoader(containerClassLoader, new ContainerClassLoaderFilterFactory().create(BOOT_PACKAGES));
        }
        return executionClassLoader;
    }

    protected String loadResourceAsString(String resourceName) throws IOException
    {
        return IOUtils.getResourceAsString(resourceName, getClass());
    }

    protected InputStream loadResource(String resourceName) throws IOException
    {
        return IOUtils.getResourceAsStream(resourceName, getClass());
    }

    private Object getComponentObject(Component component) throws Exception
    {
        if (component instanceof JavaComponent)
        {
            return ((AbstractJavaComponent) component).getObjectFactory().getInstance(muleContext);
        }
        else
        {
            fail("Component is not a JavaComponent and therefore has no component object instance");
            return null;
        }
    }

    protected void stopFlowConstruct(String flowName) throws Exception
    {
        FlowConstruct flowConstruct = getFlowConstruct(flowName);
        Flow flow = (Flow) flowConstruct;
        flow.stop();
    }

    /**
     * Initializes a builder to construct an event and the running context to run it through a flow.
     * 
     * @param flowName
     * @return the {@link FlowRunner}
     */
    protected FlowRunner flowRunner(String flowName)
    {
        return new FlowRunner(muleContext, flowName);
    }

    /**
     * Runs the given flow with a default event
     * 
     * @param flowName the name of the flow to be executed
     * @return the resulting <code>MuleEvent</code>
     * @throws Exception
     */
    protected MuleEvent runFlow(String flowName) throws Exception
    {
        return flowRunner(flowName).run();
    }

    /**
     * Retrieve a flow by name from the registry
     * 
     * @param name Name of the flow to retrieve
     */
    protected Flow lookupFlowConstruct(String name)
    {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct(name);
    }

    @After
    public final void clearFlowAssertions() throws Exception
    {
        FlowAssert.reset();
    }

    protected void stopFlowSchedulers(String flowName) throws MuleException
    {
        final Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.flowConstructPollingSchedulers(flowName));
        for (final Scheduler scheduler : schedulers)
        {
            scheduler.stop();
        }
    }

    protected SubflowInterceptingChainLifecycleWrapper getSubFlow(String subflowName)
    {
        return (SubflowInterceptingChainLifecycleWrapper) muleContext.getRegistry().lookupObject(subflowName);
    }

    protected void runSchedulersOnce(String flowConstructName) throws Exception
    {
        final Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.flowConstructPollingSchedulers(flowConstructName));

        for (final Scheduler scheduler : schedulers)
        {
            scheduler.schedule();
        }
    }
}
