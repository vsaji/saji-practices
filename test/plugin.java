import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;

import java.util.Set;
import java.util.stream.Collectors;

public class ModifyDependencyPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.afterEvaluate(evaluatedProject -> {
            // Access the compileClasspath configuration
            Configuration compileClasspathConfig = project.getConfigurations().getByName("compileClasspath");

            if (compileClasspathConfig.isCanBeResolved()) {
                // Get resolved dependencies
                Set<ResolvedDependencyResult> resolvedDependencies = getResolvedDependencies(compileClasspathConfig);

                // Force new versions dynamically based on the resolved dependencies
                compileClasspathConfig.getResolutionStrategy().eachDependency(details -> {
                    resolvedDependencies.forEach(resolvedDependency -> {
                        ModuleComponentIdentifier id = (ModuleComponentIdentifier) resolvedDependency.getSelected().getId();

                        // Apply your custom logic here to force a specific version
                        if ("org.springframework".equals(id.getGroup()) && "spring-core".equals(id.getModule())) {
                            details.useVersion("5.3.8");  // Force a newer version
                        }

                        // Example of forcing a version based on existing version
                        if ("com.google.guava".equals(id.getGroup()) && "guava".equals(id.getModule())) {
                            details.useVersion("30.1.1-jre");  // Change to a higher version
                        }
                    });
                });

                // Trigger the resolution of the dependencies (optional)
                compileClasspathConfig.resolve();
            }
        });
    }

    // Helper method to get resolved dependencies from a configuration
    private Set<ResolvedDependencyResult> getResolvedDependencies(Configuration configuration) {
        return configuration.getIncoming()
                .getResolutionResult()
                .getAllDependencies()
                .stream()
                .filter(dependencyResult -> dependencyResult instanceof ResolvedDependencyResult)
                .map(dependencyResult -> (ResolvedDependencyResult) dependencyResult)
                .collect(Collectors.toSet());
    }
}


import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class ReplaceDependencyPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.afterEvaluate(evaluatedProject -> {
            // Access the compileClasspath configuration
            Configuration compileClasspathConfig = project.getConfigurations().getByName("compileClasspath");

            if (compileClasspathConfig.isCanBeResolved()) {
                // Replace dependencies before resolution
                compileClasspathConfig.getDependencies().forEach(dependency -> {
                    if ("org.springframework".equals(dependency.getGroup()) && "spring-core".equals(dependency.getName())) {
                        // Replace with a new version
                        project.getDependencies().add("compileClasspath", "org.springframework:spring-core:5.3.8");
                    }

                    if ("com.google.guava".equals(dependency.getGroup()) && "guava".equals(dependency.getName())) {
                        // Remove old version and add a new one
                        compileClasspathConfig.getDependencies().remove(dependency);
                        project.getDependencies().add("compileClasspath", "com.google.guava:guava:30.1.1-jre");
                    }
                });

                // Trigger resolution (optional)
                compileClasspathConfig.resolve();
            }
        });
    }
}




import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;

import java.util.Set;
import java.util.stream.Collectors;

public class ModifyDependencyPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Access the compileClasspath configuration
        Configuration compileClasspathConfig = project.getConfigurations().getByName("compileClasspath");

        // Apply resolution strategy before resolution happens
        compileClasspathConfig.getResolutionStrategy().eachDependency(details -> {
            // Custom logic to force versions before resolution
            if ("org.springframework".equals(details.getRequested().getGroup()) &&
                "spring-core".equals(details.getRequested().getName())) {
                details.useVersion("5.3.8"); // Force a newer version of spring-core
            }

            if ("com.google.guava".equals(details.getRequested().getGroup()) &&
                "guava".equals(details.getRequested().getName())) {
                details.useVersion("30.1.1-jre"); // Change to a higher version of guava
            }
        });

        // Optional: Add a task to print resolved dependencies after the configuration is resolved
        project.getTasks().create("printResolvedDependencies", task -> {
            task.doLast(action -> {
                Set<ResolvedDependencyResult> resolvedDependencies = getResolvedDependencies(compileClasspathConfig);
                resolvedDependencies.forEach(dependency -> {
                    ModuleComponentIdentifier id = (ModuleComponentIdentifier) dependency.getSelected().getId();
                    String group = id.getGroup();
                    String name = id.getModule();
                    String version = id.getVersion();
                    project.getLogger().lifecycle("Resolved Dependency: {}:{}:{}", group, name, version);
                });
            });
        });
    }

    // Helper method to get resolved dependencies from a configuration
    private Set<ResolvedDependencyResult> getResolvedDependencies(Configuration configuration) {
        return configuration.getIncoming()
                .getResolutionResult()
                .getAllDependencies()
                .stream()
                .filter(dependencyResult -> dependencyResult instanceof ResolvedDependencyResult)
                .map(dependencyResult -> (ResolvedDependencyResult) dependencyResult)
                .collect(Collectors.toSet());
    }
}

//------------------------------------------------------------------------------------------------------------

import groovy.lang.Closure;
import java.util.HashMap;
import java.util.Map;

public class ExcludeForceDependencyExtension {

    private final Map<String, String> dependencies = new HashMap<>();

    // Method to allow adding dependencies via direct string input
    public void exclude(String dependency) {
        // Split group, name, and version (assumes it's in the format "group:name:version")
        String[] parts = dependency.split(":");
        if (parts.length == 3) {
            String group = parts[0];
            String name = parts[1];
            String version = parts[2];
            dependencies.put(group + ":" + name, version);
        } else {
            throw new IllegalArgumentException("Invalid dependency format. Expected 'group:name:version'.");
        }
    }

    // Method to allow configuration via closure
    public void exclude(Closure<?> closure) {
        // Delegate the closure to this instance, so it can call the `exclude(String dependency)` method
        closure.setDelegate(this);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }

    // Getter for the map of dependencies
    public Map<String, String> getDependencies() {
        return dependencies;
    }
}
//--------------------------------------------

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CustomPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Register the extension with the project
        ExcludeForceDependencyExtension extension = project.getExtensions()
                .create("excludeForceDependency", ExcludeForceDependencyExtension.class);

        // After the project is evaluated, you can access the map and take action
        project.afterEvaluate(p -> {
            // Access the dependencies map from the extension
            Map<String, String> dependencies = extension.getDependencies();

            // For example, print out all excluded dependencies
            dependencies.forEach((key, value) -> {
                System.out.println("Excluding dependency: " + key + ":" + value);
            });

            // Additional logic for handling exclusions can go here
        });
    }
}

//--------------------------------------------------------------

plugins {
    id 'com.example.customplugin' // Your plugin's ID
}

excludeForceDependency {
    exclude "org.springframework:spring-core:6.1.12"
    exclude "org.apache.commons:commons-lang3:3.12.0"
}

//-------------------------------------------------------------
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.Map;

public class PrintExclusionsTask extends DefaultTask {

    private ExcludeForceDependencyExtension excludeExtension;

    // Setter method to inject the extension
    public void setExcludeExtension(ExcludeForceDependencyExtension excludeExtension) {
        this.excludeExtension = excludeExtension;
    }

    @TaskAction
    public void printExclusions() {
        Map<String, String> dependencies = excludeExtension.getDependencies();
        if (dependencies.isEmpty()) {
            getLogger().lifecycle("No dependencies have been excluded.");
        } else {
            getLogger().lifecycle("Excluded dependencies:");
            dependencies.forEach((key, value) -> 
                getLogger().lifecycle(key + ":" + value)
            );
        }
    }
}
//-------------------------------------------------------------
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.Map;

public class PrintMainMapTask extends DefaultTask {

    private Map<String, String> mainDependencyMap;

    // Setter to inject the main map
    public void setMainDependencyMap(Map<String, String> mainDependencyMap) {
        this.mainDependencyMap = mainDependencyMap;
    }

    @TaskAction
    public void printMainMap() {
        if (mainDependencyMap == null || mainDependencyMap.isEmpty()) {
            getLogger().lifecycle("Main dependency map is empty or not defined.");
        } else {
            getLogger().lifecycle("Main dependency map contents:");
            mainDependencyMap.forEach((key, value) -> 
                getLogger().lifecycle(key + ":" + value)
            );
        }
    }
}
//------------------------------------------------------------------------

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

public class CustomPlugin implements Plugin<Project> {

    // Main map to store the complete list of dependencies
    private final Map<String, String> mainDependencyMap = new HashMap<>();

    @Override
    public void apply(Project project) {
        // Populate the main map (this could be based on logic, configuration, etc.)
        mainDependencyMap.put("org.springframework:spring-core", "6.1.12");
        mainDependencyMap.put("org.apache.commons:commons-lang3", "3.12.0");
        mainDependencyMap.put("com.google.guava:guava", "31.0-jre");

        // Register the excludeForceDependency extension (as before)
        ExcludeForceDependencyExtension extension = project.getExtensions()
                .create("excludeForceDependency", ExcludeForceDependencyExtension.class);

        // Register a task to print the main map
        project.getTasks().register("printMainMap", PrintMainMapTask.class, task -> {
            task.setGroup("Custom");
            task.setDescription("Prints the main map of dependencies.");
            task.setMainDependencyMap(mainDependencyMap);  // Inject the main map into the task
        });

        // Optional: You could also register the exclusions printing task as before
        project.getTasks().register("printExclusions", PrintExclusionsTask.class, task -> {
            task.setGroup("Custom");
            task.setDescription("Prints the excluded dependencies.");
            task.setExcludeExtension(extension);  // Inject the extension into the task
        });
    }
}

//--------------------------------------------------------------------------------

plugins {
    id 'com.example.customplugin' // Your plugin's ID
}

excludeForceDependency {
    exclude "org.springframework:spring-core:6.1.12"
}

// Task to print the main map of dependencies
task printMainMap(type: PrintMainMapTask)

./gradlew printMainMap


