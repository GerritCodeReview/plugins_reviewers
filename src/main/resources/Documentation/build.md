Build
=====

This plugin is built with Bazel.

Two build modes are supported: Standalone and in Gerrit tree.
The standalone build mode is recommended, as this mode doesn't require
the Gerrit tree to exist locally.

### Build standalone

```
  bazel build @PLUGIN@
```

The output is created in

```
  bazel-genfiles/@PLUGIN@.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.sh
```

### Build in Gerrit tree

```
  bazel build plugins/@PLUGIN@
```

The output is created in

```
  bazel-genfiles/plugins/@PLUGIN@/@PLUGIN@.jar
```

This project can be imported into the Eclipse IDE.
Add the plugin name to the `CUSTOM_PLUGINS` set in
Gerrit core in `tools/bzl/plugins.bzl`, and execute:

```
  ./tools/eclipse/project.py
```

### Backend-only

There are two separate plugin targets, one containing UI components
(`reviewers`), and one with only backend components (`reviewers-backend`). The
UI plugin is only compatible with the GWT UI, and does not work with PolyGerrit.
Both build instructions will work with either `reviewers` or
`reviewers-backend`.
