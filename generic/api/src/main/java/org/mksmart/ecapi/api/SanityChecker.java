/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mksmart.ecapi.api;

import java.util.Set;

/**
 * A utility class that checks if the {@link AssemblyProvider} is in a consistent state, and if not it may be
 * used to attempt to fix it.
 * 
 * @author alessandro <alexdma@apache.org>
 *
 * @param <M>
 *            the microcompiler type supported by this sanity checker.
 */
public interface SanityChecker<M> {

    /**
     * Rebuilds only the inconsistent elements of the assembly provider state.
     * 
     * @param provider
     *            the assembly provider
     */
    public void repair(AssemblyProvider<M> provider);

    /**
     * Returns the elements of an assembly provider that are in a consistent state.
     * 
     * @param provider
     *            the assembly provider
     */
    public Set<Object> getFailingItems(AssemblyProvider<M> provider);

    /**
     * Returns the elements of an assembly provider that are in an inconsistent state.
     * 
     * @param provider
     *            the assembly provider
     */
    public Set<Object> getSaneItems(AssemblyProvider<M> provider);

    public Set<Class<?>> getSupportedProviderTypes();

    /**
     * Checks if the state of an assembly provider is consistent.
     * 
     * @param provider
     *            the assembly provider
     */
    public boolean isSane(AssemblyProvider<M> provider);

    /**
     * Rebuilds the entire assembly provider state.
     * 
     * @param provider
     *            the assembly provider
     */
    public void rebuild(AssemblyProvider<M> provider);
}
