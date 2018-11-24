workspace(name = "reviewers")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "60bb597a9b8b0700334f8845ca61a7abc604ffcc",
    #local_path = "/home/<user>/projects/bazlets",
)

# Release Plugin API
#load(
#    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
#    "gerrit_api",
#)

# Snapshot Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api_maven_local.bzl",
    "gerrit_api_maven_local",
)

# Load release Plugin API
#gerrit_api()

# Load snapshot Plugin API
gerrit_api_maven_local()
