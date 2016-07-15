package org.mksmart.ecapi.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.mksmart.ecapi.web.resources.CompilerResource;
import org.mksmart.ecapi.web.resources.DatasetResource;
import org.mksmart.ecapi.web.resources.EntityResourceWithProvenance;

/**
 * Core entity-centric API Web Application fragment.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class CompilerApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(CompilerResource.class);
        // classes.add(EntityResource.class);
        classes.add(EntityResourceWithProvenance.class);
        classes.add(DatasetResource.class);
        return classes;
    }

}
