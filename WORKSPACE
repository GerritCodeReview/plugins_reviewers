workspace(name = "reviewers")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "91ea277efd6d9f24c18d6ddeba65bed866b00872",
    #local_path = "/home/<user>/projects/bazlets",
)

# Polymer dependencies
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_polymer.bzl",
    "gerrit_polymer",
)

gerrit_polymer()

# Load closure compiler with transitive dependencies
load("@io_bazel_rules_closure//closure:repositories.bzl", "rules_closure_dependencies", "rules_closure_toolchains")

rules_closure_dependencies()

rules_closure_toolchains()

# Load Gerrit npm_binary toolchain
load("@com_googlesource_gerrit_bazlets//tools:js.bzl", "GERRIT", "npm_binary")

npm_binary(
    name = "polymer-bundler",
    repository = GERRIT,
)

npm_binary(
    name = "crisper",
    repository = GERRIT,
)

# Load plugin API
load("@com_googlesource_gerrit_bazlets//:gerrit_api.bzl", "gerrit_api")

gerrit_api()
