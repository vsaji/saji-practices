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
