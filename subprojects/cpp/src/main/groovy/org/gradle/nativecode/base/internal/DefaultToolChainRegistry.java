/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativecode.base.internal;

import org.gradle.api.Action;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.nativecode.base.ToolChain;
import org.gradle.nativecode.base.ToolChainRegistry;

import java.util.ArrayList;
import java.util.List;

public class DefaultToolChainRegistry extends DefaultNamedDomainObjectSet<ToolChain> implements ToolChainRegistry {
    private final List<ToolChainInternal> searchOrder = new ArrayList<ToolChainInternal>();

    public DefaultToolChainRegistry(Instantiator instantiator) {
        super(ToolChain.class, instantiator);
        whenObjectAdded(new Action<ToolChain>() {
            public void execute(ToolChain toolChain) {
                searchOrder.add((ToolChainInternal) toolChain);
            }
        });
        whenObjectRemoved(new Action<ToolChain>() {
            public void execute(ToolChain toolChain) {
                searchOrder.remove(toolChain);
            }
        });
    }

    public List<ToolChainInternal> getSearchOrder() {
        return searchOrder;
    }

    public ToolChainInternal getDefaultToolChain() {
        return new LazyToolChain() {
            @Override
            protected ToolChainInternal findFirstAvailableToolChain() {
                List<String> messages = new ArrayList<String>();
                for (ToolChainInternal adapter : searchOrder) {
                    ToolChainAvailability availability = adapter.getAvailability();
                    if (availability.isAvailable()) {
                        return adapter;
                    }
                    messages.add(String.format("Could not load '%s': %s", adapter.getName(), availability.getUnavailableMessage()));
                }
                throw new IllegalStateException(String.format("No tool chain is available: %s", messages));
            }
        };
    }

    private abstract class LazyToolChain implements ToolChainInternal {
        private ToolChainInternal toolChain;

        public String getName() {
            return getToolChain().getName();
        }

        public ToolChainAvailability getAvailability() {
            return new ToolChainAvailability();
        }

        public <T extends BinaryCompileSpec> Compiler<T> createCompiler(final Class<T> specType) {
            return new Compiler<T>() {
                public WorkResult execute(T spec) {
                    return getToolChain().createCompiler(specType).execute(spec);
                }
            };
        }

        public <T extends LinkerSpec> Compiler<T> createLinker() {
            return new Compiler<T>() {
                 public WorkResult execute(T spec) {
                     return getToolChain().createLinker().execute(spec);
                 }
             };
        }

        public <T extends StaticLibraryArchiverSpec> Compiler<T> createStaticLibraryArchiver() {
            return new Compiler<T>() {
                public WorkResult execute(T spec) {
                    return getToolChain().createStaticLibraryArchiver().execute(spec);
                }
            };
        }


        private ToolChainInternal getToolChain() {
            if (toolChain == null) {
                toolChain = findFirstAvailableToolChain();
            }
            return toolChain;
        }

        protected abstract ToolChainInternal findFirstAvailableToolChain();
    }
}