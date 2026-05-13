"""Macro for running JUnit 5 tests via the JUnit Platform ConsoleLauncher."""

load("@rules_java//java:defs.bzl", "java_test")

def junit5_test(name, test_class, srcs, deps, resources = None, runtime_deps = None, **kwargs):
    java_test(
        name = name,
        srcs = srcs,
        deps = deps,
        resources = resources or [],
        runtime_deps = (runtime_deps or []) + [
            "@maven//:org_junit_jupiter_junit_jupiter_engine",
            "@maven//:org_junit_platform_junit_platform_launcher",
            "@maven//:org_junit_platform_junit_platform_console",
        ],
        use_testrunner = False,
        main_class = "org.junit.platform.console.ConsoleLauncher",
        args = ["--select-class=" + test_class],
        **kwargs
    )
