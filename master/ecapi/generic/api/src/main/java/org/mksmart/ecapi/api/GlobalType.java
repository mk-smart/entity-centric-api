package org.mksmart.ecapi.api;

import java.util.Set;

import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;

/**
 * Globalised type, generally aware of its surroundings in the type hierarchy, but not of its dataset support.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface GlobalType {

    /**
     * The global URI of the type that does not subsume any other type in any taxonomy.
     */
    public static final GlobalURI TOP_URI = new ScopedGlobalURI("type", "global", "id", "T");

    /**
     * Adds a new type that subsumes this one.
     * 
     * @param subType
     *            a subtype.
     */
    public void addSubtype(GlobalType subType);

    /**
     * Adds a new type that this type is known to subsume.
     * 
     * @param superType
     *            a supertype.
     */
    public void addSupertype(GlobalType superType);

    /**
     * Gets the global types subsumed by this one. Implementations of this interface or their managers are not
     * required to ensure consistency with calls to {@link GlobalType#getSubtypes()} - they might, for
     * efficiently reasons, want to traverse the taxonomy starting with supertypes only. Therefore,
     * applications should generally not rely on this method to construct the taxonomy, unless an
     * implementation is used that specifically guarantees it.
     * 
     * @return the set of asserted supertypes.
     */
    public Set<GlobalType> getAssertedSupertypes();

    /**
     * Gets the default query template associated with this global type. This template is used to generate
     * microcompiler queries when no template is specified for the dataset/type combination at hand.
     * 
     * TODO change to appropriate object once the query template parser is done.
     * 
     * @return the default query template
     */
    public String getDefaultQueryTemplate();

    /**
     * Gets the global URI of this type.
     * 
     * @return the global identifier of this type.
     */
    public GlobalURI getId();

    /**
     * Gets the global types that subsume this. Implementations should ensure consistency of subtypes, i.e. a
     * call to this method on a global type should return all the known subtypes, either asserted or inferred,
     * and this type should never appear as a supertype of a {@link GlobalType} that is not returned by this
     * method.
     * 
     * @return the set of subtypes.
     */
    public Set<GlobalType> getSubtypes();

    /**
     * Sets a default query template for this global type.
     * 
     * @param queryTemplate
     *            the default query template.
     */
    public void setDefaultQueryTemplate(String queryTemplate);

}
