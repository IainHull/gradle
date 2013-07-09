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
import org.gradle.api.DomainObjectSet;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.nativecode.base.NativeBinary;
import org.gradle.nativecode.base.NativeComponent;
import org.gradle.util.GUtil;

public class DefaultNativeComponent implements NativeComponent {
    private final String name;
    private final DomainObjectSet<LanguageSourceSet> sourceSets;
    private final DefaultDomainObjectSet<NativeBinary> binaries;
    private String baseName;

    public DefaultNativeComponent(String name) {
        this.name = name;
        this.sourceSets = new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet.class);
        binaries = new DefaultDomainObjectSet<NativeBinary>(NativeBinary.class);
    }

    public String getName() {
        return name;
    }

    public DomainObjectSet<LanguageSourceSet> getSource() {
        return sourceSets;
    }

    public void source(FunctionalSourceSet sourceSet) {
        sourceSet.all(new Action<LanguageSourceSet>() {
            public void execute(LanguageSourceSet languageSourceSet) {
                source(languageSourceSet);
            }
        });
    }

    public void source(LanguageSourceSet sourceSet) {
        sourceSets.add(sourceSet);
    }

    public DomainObjectSet<NativeBinary> getBinaries() {
        return binaries;
    }

    public String getBaseName() {
        return GUtil.elvis(baseName, name);
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }
}