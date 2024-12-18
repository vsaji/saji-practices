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




package com.example;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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
        // This plugin will be configured to accept a map directly from build.gradle
        project.afterEvaluate(evaluatedProject -> {
            // Ensure that the map of dependencies and versions is passed from the configuration
            Map<String, String> enforcedVersions = getEnforcedDependencies(evaluatedProject);
            if (enforcedVersions != null && !enforcedVersions.isEmpty()) {
                // Apply the version-checking logic to all resolvable configurations
                evaluatedProject.getConfigurations().all(configuration -> {
                    if (configuration.isCanBeResolved()) {
                        configuration.resolutionStrategy(resolutionStrategy -> {
                            enforceVersions(evaluatedProject, configuration, enforcedVersions);
                        });
                    }
                });
            }
        });
    }

    // Mock method to simulate the input from build.gradle (will be replaced in usage)
    private Map<String, String> getEnforcedDependencies(Project project) {
        // You can pass this map from build.gradle using a custom task or configuration
        return Map.of(
                "org.springframework:spring-core", "5.3.8",
                "com.google.guava:guava", "30.1.1-jre"
        );
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

        for (ResolvedDependencyResult dependency : resolvedDependencies) {
            ModuleComponentIdentifier id = (ModuleComponentIdentifier) dependency.getSelected().getId();
            String dependencyNotation = id.getGroup() + ":" + id.getModule();
            String resolvedVersion = id.getVersion();

            if (enforcedVersions.containsKey(dependencyNotation)) {
                String targetVersion = enforcedVersions.get(dependencyNotation);
                if (isVersionLower(resolvedVersion, targetVersion)) {
                    logger.lifecycle("Forcing dependency {}:{} to version {}", id.getGroup(), id.getModule(), targetVersion);
                    forceVersion(configuration.getResolutionStrategy(), dependencyNotation, targetVersion);
                }
            }
        }
    }

    // Helper method to compare versions
    private boolean isVersionLower(String currentVersion, String targetVersion) {
        return new ComparableVersion(currentVersion).compareTo(new ComparableVersion(targetVersion)) < 0;
    }

    // Helper method to force dependency version
    private void forceVersion(ResolutionStrategy resolutionStrategy, String dependency, String version) {
        resolutionStrategy.force(dependency + ":" + version);
    }
}



public class DependencyResetPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Define your map of dependencies and their enforced versions
        Map<String, String> enforcedVersions = Map.of(
                "org.springframework:spring-core", "5.3.8",
                "com.google.guava:guava", "30.1.1-jre"
        );

        // Create a new configuration that extends compileClasspath (or any configuration)
        project.getConfigurations().create("resetCompileClasspath", configuration -> {
            configuration.extendsFrom(project.getConfigurations().getByName("compileClasspath"));
            configuration.setCanBeResolved(true); // Ensure it is resolvable

            // Apply beforeResolve hook to enforce dependency versions
            configuration.getIncoming().beforeResolve(dependencies -> {
                enforceVersions(configuration.getResolutionStrategy(), enforcedVersions);
            });
        });

         project.afterEvaluate(evaluatedProject -> {
        resetCompileClasspath.resolve();
    });
    }

    // Enforce the versions of dependencies before the new configuration is resolved
    private void enforceVersions(ResolutionStrategy resolutionStrategy, Map<String, String> enforcedVersions) {
        enforcedVersions.forEach((dependency, version) -> {
            resolutionStrategy.force(dependency + ":" + version);
        });
    }
}

