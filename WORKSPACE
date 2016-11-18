workspace(name = "reviewers")

# Official bazlets repository
git_repository(
    name = "com_github_davido_bazlets",
    remote = "https://github.com/davido/bazlets.git",
    tag = "v0.1",
)

# Local bazlets repository
#local_repository(
#  name = "com_github_davido_bazlets",
#  path = "/home/davido/projects/bazlets",
#)

# Release Plugin API
#load("@com_github_davido_bazlets//:gerrit_api.bzl",
#     "gerrit_api")

# Snapshot Plugin API
load(
    "@com_github_davido_bazlets//:gerrit_api_maven_local.bzl",
    "gerrit_api_maven_local",
)
load(
    "@com_github_davido_bazlets//:gerrit_gwt.bzl",
    "gerrit_gwt",
)

# Load release Plugin API
# gerrit_api()

# Load snapshot Plugin API
gerrit_api_maven_local()

gerrit_gwt()
