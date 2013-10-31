A plugin that allows adding default reviewers to a change.

The configuration for adding reviewers to submitted changes can be
[configured per project](config.html).

Build
-----

Prerequisites is to install gerrit-plugin-api in local Maven repository.
It can be done from the Gerrit tree:

```
  $>buck build api_install
```

Alternatively GERRIT_MAVEN repository can be selected if the needed artifactes
for the used release were published. The following line must be changed in
root BUCK file:

```
REPO = GERRIT
```

The `reviewers.jar` can be built with the following command:

```
  $>buck build plugin
```

Deploy
------

```
$>cp buck-out/gen/reviewers.jar `'$site_path'/plugin`
```

SEE ALSO
--------

* [Building with Buck](../../../Documentation/dev-buck.html)
