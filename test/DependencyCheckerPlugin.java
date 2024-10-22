package com.example;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.logging.Logger;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyCheckerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Define the dependencies and versions you want to enforce
        Map<String, String> enforcedVersions = Map.of(
                "org.springframework:spring-core", "5.3.8",
                "com.google.guava:guava", "30.1.1-jre"
        );

        // Apply the version-checking logic to all configurations
        project.getConfigurations().all(configuration -> {
            configuration.resolutionStrategy(resolutionStrategy -> {
                enforceVersions(project, configuration, enforcedVersions);
            });
        });
    }

    // Method to enforce dependency versions if they are lower
    private void enforceVersions(Project project, Configuration configuration, Map<String, String> enforcedVersions) {
        Set<ResolvedDependencyResult> resolvedDependencies = configuration
                .getIncoming()
                .getResolutionResult()
                .getAllDependencies()
                .stream()
                .filter(dependencyResult -> dependencyResult instanceof ResolvedDependencyResult)
                .map(dependencyResult -> (ResolvedDependencyResult) dependencyResult)
                .collect(Collectors.toSet());

        Logger logger = project.getLogger();

        // Check the versions of resolved dependencies
        for (ResolvedDependencyResult dependency : resolvedDependencies) {
            ModuleComponentIdentifier id = (ModuleComponentIdentifier) dependency.getSelected().getId();
            String dependencyNotation = id.getGroup() + ":" + id.getModule();
            String resolvedVersion = id.getVersion();

            if (enforcedVersions.containsKey(dependencyNotation)) {
                String targetVersion = enforcedVersions.get(dependencyNotation);
                
                // Compare the versions and force the target version if needed
                if (isVersionLower(resolvedVersion, targetVersion)) {
                    logger.lifecycle("Forcing dependency {}:{} to version {}", id.getGroup(), id.getModule(), targetVersion);
                    forceVersion(configuration.getResolutionStrategy(), dependencyNotation, targetVersion);
                }
            }
        }
    }

    // Helper method to compare versions
    private boolean isVersionLower(String currentVersion, String targetVersion) {
        // Use Gradle's version comparator or a custom comparator based on your needs
        return new ComparableVersion(currentVersion).compareTo(new ComparableVersion(targetVersion)) < 0;
    }

    // Helper method to force dependency version
    private void forceVersion(ResolutionStrategy resolutionStrategy, String dependency, String version) {
        resolutionStrategy.force(dependency + ":" + version);
    }
}
