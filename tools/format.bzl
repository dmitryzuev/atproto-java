"""Rule for running google-java-format on all Java sources via `bazel run`."""

def _gjf_runner_impl(ctx):
    formatter = ctx.executable.formatter
    flags = "--dry-run --set-exit-if-changed" if ctx.attr.check_mode else "--replace"

    # formatter.short_path is e.g. "tools/google_java_format".
    # In bzlmod, the main module's canonical runfiles name is "_main".
    formatter_rlocation = "_main/" + formatter.short_path

    script = ctx.actions.declare_file(ctx.label.name + ".sh")
    ctx.actions.write(
        output = script,
        is_executable = True,
        # Use % formatting — { and } need no escaping here.
        content = "\n".join([
            "#!/usr/bin/env bash",
            "set -euo pipefail",
            'GJF="${RUNFILES_DIR:-$0.runfiles}/' + formatter_rlocation + '"',
            'WORKSPACE="${BUILD_WORKSPACE_DIRECTORY:-.}"',
            "if [[ $# -gt 0 ]]; then",
            '  "$GJF" ' + flags + ' "$@"',
            "else",
            '  find "$WORKSPACE/src" -name "*.java" -print0 | xargs -0 "$GJF" ' + flags,
            "fi",
            "",
        ]),
    )

    runfiles = ctx.runfiles().merge(
        ctx.attr.formatter[DefaultInfo].default_runfiles,
    )
    return [DefaultInfo(executable = script, runfiles = runfiles)]

gjf_runner = rule(
    implementation = _gjf_runner_impl,
    executable = True,
    attrs = {
        "formatter": attr.label(
            executable = True,
            cfg = "exec",
            mandatory = True,
        ),
        "check_mode": attr.bool(default = False),
    },
)
