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
package org.gradle.nativecode.base.plugins;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.internal.FactoryNamedDomainObjectContainer;
import org.gradle.api.internal.ReflectiveNamedDomainObjectFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.base.BinariesContainer;
import org.gradle.language.base.plugins.LanguageBasePlugin;
import org.gradle.nativecode.base.Executable;
import org.gradle.nativecode.base.Library;
import org.gradle.nativecode.base.NativeBinary;
import org.gradle.nativecode.base.internal.*;
import org.gradle.nativecode.base.internal.LibraryInternal;

import javax.inject.Inject;
import java.io.File;
import java.util.concurrent.Callable;

/**
 * temp plugin, not sure what will provide the binaries container and model elements
 */
@Incubating
public class BinariesPlugin implements Plugin<ProjectInternal> {
    private final Instantiator instantiator;

    @Inject
    public BinariesPlugin(Instantiator instantiator) {
        this.instantiator = instantiator;
    }

    public void apply(final ProjectInternal project) {
        project.getPlugins().apply(BasePlugin.class);
        project.getPlugins().apply(LanguageBasePlugin.class);
        final BinariesContainer binaries = project.getExtensions().getByType(BinariesContainer.class);

        project.getExtensions().create("compilers",
                DefaultToolChainRegistry.class,
                instantiator
        );
        NamedDomainObjectSet<Executable> executables = project.getExtensions().create(
                "executables",
                FactoryNamedDomainObjectContainer.class,
                Executable.class,
                instantiator,
                new ReflectiveNamedDomainObjectFactory<Executable>(DefaultExecutable.class)
        );

        executables.all(new Action<Executable>() {
            public void execute(Executable executable) {
                binaries.add(setupDefaults(project, instantiator.newInstance(DefaultExecutableBinary.class, executable)));
            }
        });

        NamedDomainObjectSet<Library> libraries = project.getExtensions().create("libraries",
                FactoryNamedDomainObjectContainer.class,
                Library.class,
                instantiator,
                new ReflectiveNamedDomainObjectFactory<Library>(DefaultLibrary.class, project.getFileResolver())
        );

        libraries.withType(LibraryInternal.class, new Action<LibraryInternal>() {
            public void execute(LibraryInternal library) {
                DefaultSharedLibraryBinary sharedLibraryBinary = instantiator.newInstance(DefaultSharedLibraryBinary.class, library);
                library.setDefaultBinary(sharedLibraryBinary);
                binaries.add(setupDefaults(project, sharedLibraryBinary));
                binaries.add(setupDefaults(project, instantiator.newInstance(DefaultStaticLibraryBinary.class, library)));
            }
        });
    }

    private NativeBinary setupDefaults(final ProjectInternal project, final NativeBinary nativeBinary) {
        new DslObject(nativeBinary).getConventionMapping().map("outputFile", new Callable<File>() {
            public File call() throws Exception {
                return new File(project.getBuildDir(), "binaries/" + nativeBinary.getOutputFileName());
            }
        });
        return nativeBinary;
    }

}