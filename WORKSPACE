# Official bazlets repository
git_repository(
  name = "io_bazlets",
  remote = "https://github.com/davido/bazlets.git",
  commit = "1b6ee5dcbd686d4c5b69e20e5154463e48176060",
)

# Local bazlets repository
#local_repository(
#  name = "io_bazlets",
#  path = "/home/davido/projects/bazlets",
#)

# Release Plugin API
#load("@io_bazlets//:gerrit_api.bzl", "gerrit_api")
# Snapshot Plugin API
load("@io_bazlets//:gerrit_api_maven_local.bzl", "gerrit_api_maven_local")

load("@io_bazlets//:gerrit_gwt.bzl", "gerrit_gwt")

# Load release Plugin API
# gerrit_api()
# Load snapshot Plugin API
gerrit_api_maven_local()

gerrit_gwt()
