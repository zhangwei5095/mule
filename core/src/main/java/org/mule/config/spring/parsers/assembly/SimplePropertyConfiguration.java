/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;

/**
 * A direct implementation of {@link PropertyConfiguration}
 */
public class SimplePropertyConfiguration implements PropertyConfiguration
{

    private List references = new ArrayList();
    private Properties nameMappings = new Properties();
    private Map valueMappings = new HashMap();
    private Set collections = new HashSet();
    private Map ignored = new HashMap();
    private boolean ignoreAll = false;

    public void addReference(String propertyName)
    {
        references.add(dropRef(propertyName));
    }

    public void addMapping(String propertyName, Map mappings)
    {
        valueMappings.put(propertyName, new NamedValueMap(propertyName, mappings));
    }

    public void addMapping(String propertyName, String mappings)
    {
        valueMappings.put(propertyName, new NamedValueMap(propertyName, mappings));
    }

    public void addMapping(String propertyName, ValueMap mappings)
    {
        valueMappings.put(propertyName, new NamedValueMap(propertyName, mappings));
    }

    public void addAlias(String alias, String propertyName)
    {
        nameMappings.put(alias, propertyName);
    }

    public void addCollection(String propertyName)
    {
        collections.add(dropRef(propertyName));
    }

    public void addIgnored(String propertyName)
    {
        ignored.put(dropRef(propertyName), Boolean.TRUE);
    }

    public void removeIgnored(String propertyName)
    {
        ignored.put(dropRef(propertyName), Boolean.FALSE);
    }

    public void setIgnoredDefault(boolean ignoreAll)
    {
        this.ignoreAll = ignoreAll;
    }

    public String getAttributeMapping(String alias)
    {
        return getAttributeMapping(alias, alias);
    }

    public String getAttributeMapping(String alias, String deflt)
    {
        return nameMappings.getProperty(alias, deflt);
    }

    public boolean isCollection(String propertyName)
    {
        return collections.contains(dropRef(propertyName));
    }

    public boolean isIgnored(String propertyName)
    {
        String name = dropRef(propertyName);
        if (ignored.containsKey(name))
        {
            return ((Boolean) ignored.get(name)).booleanValue();
        }
        else
        {
            return ignoreAll;
        }
    }

    /**
     * A property can be explicitly registered as a bean reference via registerBeanReference()
     * or it can simply use the "-ref" suffix.
     * @param attributeName true if the name appears to correspond to a reference
     */
    public boolean isReference(String attributeName)
    {
        return (references.contains(dropRef(attributeName))
                || attributeName.endsWith(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF_SUFFIX)
                || attributeName.endsWith(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REFS_SUFFIX)
                || attributeName.equals(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF));
    }

    public SingleProperty getSingleProperty(String name)
    {
        return new SinglePropertyWrapper(name, this);
    }

     /**
     * Extract a JavaBean property name from the supplied attribute name.
     * <p>The default implementation uses the {@link org.springframework.core.Conventions#attributeNameToPropertyName(String)}
     * method to perform the extraction.
     * <p>The name returned must obey the standard JavaBean property name
     * conventions. For example for a class with a setter method
     * '<code>setBingoHallFavourite(String)</code>', the name returned had
     * better be '<code>bingoHallFavourite</code>' (with that exact casing).
     *
     * @param oldName the attribute name taken straight from the XML element being parsed; will never be <code>null</code>
     * @return the extracted JavaBean property name; must never be <code>null</code>
     */
    public String translateName(String oldName)
    {
        // Remove the bean reference suffix if any.
        String name = dropRef(oldName);
        // Map to the real property name if necessary.
        name = getAttributeMapping(name);
        // JavaBeans property convention.
        name = Conventions.attributeNameToPropertyName(name);
        if (!StringUtils.hasText(name))
        {
            throw new IllegalStateException("Illegal property name for " + oldName + ": cannot be null or empty.");
        }
        return name;
    }

    protected String dropRef(String name)
    {
        return org.mule.util.StringUtils.chomp(
                org.mule.util.StringUtils.chomp(name, AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF_SUFFIX),
                AbstractMuleBeanDefinitionParser.ATTRIBUTE_REFS_SUFFIX);
    }

    public String translateValue(String name, String value)
    {
        NamedValueMap vm = (NamedValueMap) valueMappings.get(name);
        if (vm != null)
        {
            return vm.getValue(value);
        }
        else
        {
            return value;
        }
    }


    public static class NamedValueMap
    {
        private String propertyName;
        private ValueMap valueMap;

        public NamedValueMap(String propertyName, String mappingsString)
        {
            this.propertyName = propertyName;
            valueMap = new MapValueMap(mappingsString);
        }

        public NamedValueMap(String propertyName, Map valueMap)
        {
            this.propertyName = propertyName;
            this.valueMap = new MapValueMap(valueMap);
        }

        public NamedValueMap(String propertyName, ValueMap valueMap)
        {
            this.propertyName = propertyName;
            this.valueMap = valueMap;
        }

        public String getPropertyName()
        {
            return propertyName;
        }

        public String getValue(String key)
        {
            return valueMap.rewrite(key);
        }
    }

    public static class MapValueMap implements ValueMap
    {

        private Map map;

        public MapValueMap(Map map)
        {
            this.map = map;
        }

        public MapValueMap(String definition)
        {
            map = new HashMap();

            String[] values = StringUtils.tokenizeToStringArray(definition, ",");
            for (int i = 0; i < values.length; i++)
            {
                String value = values[i];
                int x = value.indexOf("=");
                if (x == -1)
                {
                    throw new IllegalArgumentException("Mappings string not properly defined: " + definition);
                }
                map.put(value.substring(0, x), value.substring(x+1));
            }

        }

        public String rewrite(String value)
        {
            Object result = map.get(value);
            if (null == result)
            {
                return value;
            }
            else
            {
                return result.toString();
            }
        }

    }

}