
# Official bazlets repository
git_repository(
  name = "io_bazlets",
  remote = "https://github.com/davido/bazlets.git",
  commit = "1d6e6912a0be15cd1f0a85912647304ab2993991",
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
