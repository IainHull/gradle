/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.nativecode.base.internal

import org.gradle.nativecode.base.LibraryBinary
import org.gradle.nativecode.base.NativeComponent
import org.gradle.nativecode.base.NativeDependencySet
import spock.lang.Specification

class DefaultNativeBinaryTest extends Specification {
    def binary = new DefaultNativeBinary() {
        String getOutputFileName() {
            return "someBinary.bin"
        }

        NativeComponent getComponent() {
            throw new UnsupportedOperationException()
        }

        String getName() {
            return "someBinary"
        }
    }

    def "can add a library as a dependency of the binary"() {
        def dependency = Stub(NativeDependencySet)
        def libraryBinary = Mock(LibraryBinary)
        def library = Mock(LibraryInternal)

        given:
        library.defaultBinary >> libraryBinary
        libraryBinary.asNativeDependencySet >> dependency

        when:
        binary.lib(library)

        then:
        binary.libs.size() == 1
        binary.libs.contains(dependency)
    }

    def "can add a library binary as a dependency of the binary"() {
        def dependency = Stub(NativeDependencySet)
        def library = Mock(LibraryBinary)

        given:
        library.asNativeDependencySet >> dependency

        when:
        binary.lib(library)

        then:
        binary.libs.size() == 1
        binary.libs.contains(dependency)
    }

    def "can add a native dependency as a dependency of the binary"() {
        def dependency = Stub(NativeDependencySet)

        when:
        binary.lib(dependency)

        then:
        binary.libs.size() == 1
        binary.libs.contains(dependency)
    }
}
