Usage
=============

When plugin is installed and the WebUI is enabled each project has a submenu called "Reviewers"

```
	Project -> Reviewers
```

The @PLUGIN@ plugin provides a web UI to define and manage filters and reviewers per chosen project.
Project inheritance for configuration is supported.
In that case if you would like to become a reviewer for specific content in all
hosted projects, its enough to define that rule in main project `All-Project` from
which all others projects inherit.

To define new filter you have to fill out form and click "Add" button.
The form is composed out of 3 input fields:

* filter - filter which will be used to match commits against. If a commit matches, reviewers are added.
* reviewer - comma separated list of an account (email or full user name) or a group name.
* excluded - comma separated list of paths (or regular expressions ) which will be excluded from the filters.


You can use already converted filters as a references.


## Editing reviewer/filters

Press 'EDIT' button close to the filter which you want to edit. The form will
be fill out with current values and then you can modify it and save.

## Remove reviewer/filter

To remove reviewer from the list just click small 'x' icon close to the name of
the reviewer. If you will remove last reviewer from the filter, the filter will
be removed. To remove filter just click 'REMOVE' button below the filter. You
will be ask to confirm that operation and the filter will be removed.

## Advanced

Even if the web ui is disabled, configuration for this plugin can be changed using the standard
refs/meta/config configuration mechanism.