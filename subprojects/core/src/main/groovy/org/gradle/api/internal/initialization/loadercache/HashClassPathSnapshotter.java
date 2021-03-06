/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.initialization.loadercache;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.gradle.api.internal.hash.Hasher;
import org.gradle.internal.FileUtils;
import org.gradle.internal.classpath.ClassPath;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.Adler32;

public class HashClassPathSnapshotter implements ClassPathSnapshotter {

    private final Hasher hasher;

    public HashClassPathSnapshotter(Hasher hasher) {
        this.hasher = hasher;
    }

    public HashClassPathSnapshot snapshot(ClassPath classPath) {
        final List<String> visitedFilePaths = Lists.newLinkedList();
        final Set<File> visitedDirs = Sets.newLinkedHashSet();
        final List<File> cpFiles = classPath.getAsFiles();

        final Adler32 checksum = new Adler32();
        hash(checksum, visitedFilePaths, visitedDirs, cpFiles.iterator());
        return new HashClassPathSnapshot(visitedFilePaths, checksum.getValue());
    }

    private void hash(Adler32 combinedHash, List<String> visitedFilePaths, Set<File> visitedDirs, Iterator<File> toHash) {
        while (toHash.hasNext()) {
            File file = FileUtils.canonicalize(toHash.next());
            if (file.isDirectory()) {
                if (visitedDirs.add(file)) {
                    //in theory, awkward symbolic links can lead to recursion problems.
                    //TODO - figure out a way to test it. I only tested it 'manually' and the feature is needed.
                    hash(combinedHash, visitedFilePaths, visitedDirs, Iterators.forArray(file.listFiles()));
                }
            } else if (file.isFile()) {
                visitedFilePaths.add(file.getAbsolutePath());
                combinedHash.update(hasher.hash(file).asByteArray());
            }
            //else an empty folder - a legit situation
        }
    }

    public static class HashClassPathSnapshot implements ClassPathSnapshot {
        private final List<String> files;
        private final long hash;

        public HashClassPathSnapshot(List<String> files, long hash) {
            assert files != null;

            this.files = files;
            this.hash = hash;
        }

        public long getHash() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            HashClassPathSnapshot that = (HashClassPathSnapshot) o;

            return hash == that.hash && files.equals(that.files);
        }

        @Override
        public int hashCode() {
            int result = files.hashCode();
            result = 31 * result + (int) (hash ^ (hash >>> 32));
            return result;
        }
    }
}
