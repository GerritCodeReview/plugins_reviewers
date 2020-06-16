workspace(name = "reviewers")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "69ae6ee516ec1cc51d0a07fe4419a325eb9063ce",
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

# Plugin APIs
load("@com_googlesource_gerrit_bazlets//:gerrit_api.bzl", "gerrit_api")

# To build against a local api snapshot jar, simply add the `version` parameter
# to the below `gerrit_api` macro, like:
#
# gerrit_api(version="3.0.10-SNAPSHOT")
gerrit_api()
