/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.wikistream.instance.input;

import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;

/**
 * @param <E>
 * @param <P>
 * @param <F>
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public abstract class AbstractEntityEventGenerator<E, P, F> implements EntityEventGenerator<E, P>, Initializable
{
    @Inject
    private FilterDescriptorManager filterDescriptorManager;

    protected Class<F> filterType;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the type of the internal filter
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractEntityEventGenerator.class, getClass());
        this.filterType = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[2]);
    }

    @Override
    public void write(E entity, Object filter, P properties) throws WikiStreamException
    {
        F internalFilter = this.filterDescriptorManager.createFilterProxy(this.filterType, filter);

        write(entity, filter, internalFilter, properties);
    }

    protected abstract void write(E entity, Object filter, F internalFilter, P properties)
        throws WikiStreamException;
}
