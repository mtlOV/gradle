/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.projectmodule;

import com.google.common.collect.Sets;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;

import java.io.File;
import java.util.Set;

public class DefaultCompositeBuildContext implements CompositeBuildContext {
    private final Set<RegisteredProjectPublication> publications = Sets.newLinkedHashSet();

    @Override
    public String getReplacementProjectPath(ModuleComponentSelector selector) {
        String candidate = selector.getGroup() + ":" + selector.getModule();
        for (RegisteredProjectPublication publication : publications) {
            if (publication.getModuleId().toString().equals(candidate)) {
                return publication.getProjectPath();
            }
        }
        return null;
    }

    @Override
    public File getProjectDirectory(String projectPath) {
        for (RegisteredProjectPublication publication : publications) {
            if (publication.getProjectPath().equals(projectPath)) {
                return publication.getProjectDirectory();
            }
        }
        return null;
    }

    @Override
    public void register(String module, String projectPath, String projectDir) {
        publications.add(new RegisteredProjectPublication(module, projectPath, projectDir));
    }

    public static class RegisteredProjectPublication {
        ModuleIdentifier moduleId;
        String projectPath;
        File projectDirectory;

        public RegisteredProjectPublication(String module, String projectPath, String projectDir) {
            String[] ga = module.split(":");
            this.moduleId = DefaultModuleIdentifier.newId(ga[0], ga[1]);
            this.projectPath = projectPath;
            this.projectDirectory = new File(projectDir);
        }

        public ModuleIdentifier getModuleId() {
            return moduleId;
        }

        public String getProjectPath() {
            return projectPath;
        }

        public File getProjectDirectory() {
            return projectDirectory;
        }
    }
}