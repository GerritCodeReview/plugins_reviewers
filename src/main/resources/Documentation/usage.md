Usage
=============

When plugin is installed correctly each project should have new submenu called "Reviewers"

```
	Project -> Reviewers
```

Plugin provides web UI to define and manage filters and reviewers per chosen project.
Each project inherit the configuration from parent project if are defined.
In that case if you would like to become a reviewer for specific content in all
hosted projects, its enough to define that rule in main project `All-Project` from
which all others projects inherit.

To define new filter you have to fill out form and click "Add" button.
The form is composed out of 3 input fields:

* filter - filter which will be used to filter content of the commit, if it will match the reviewer will be added.
This field provides auto completion for supported filters: 
	* file:
	* owner:
	* branch:
	* ownerin:
	* topic:
	* OR
	* AND
	* NOT

All of above filters works almost exactly the same as in the main UI search box. See the
[documentation](/Documentation/user-search.html) for details. The only
difference is that if you will use complex regular expression you need to
always surround the filters with { instead of "

So the filter will look like this:

```
    file:{^com.wamas..*.di[.|/].*}
```
and value for the `file` filter have to be started from "^", even if we do not want to have regular expresion, so for example:

```
    file:^src
```

This is probably related with missing secondary index, we are working to fix that issue.


* reviewer - coma separated list of an account (email or full user name) or a group name.
* excluded - coma separated list of paths (or regular expressions ) which will be excluded from the filters.


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

The configuration is stored directly in each repository under
refs/meta/config. Plugin use own configuration file called reviewers.conf. It
is plain text file. All changes are versioned and you can easily restore
undesirable changes in case of any problems. Since there is no validation for
syntax of the configuration file it is recommended to do all modification via
review process and check it twice if we do not break anything. To do so you can
simply push local changes for the review like this:


```
	git push origin meta/config:refs/for/refs/meta/config
```
