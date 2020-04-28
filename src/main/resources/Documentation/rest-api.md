@PLUGIN@ - /reviewers/ REST API
===============================

This page describes the REST endpoints that are added by the @PLUGIN@ plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="project-endpoints"> Reviewers Endpoints
----------------------------------------------

### <a id="get-reviewers-filters"> Get Reviewers filters
_GET /projects/project_name/reviewers-filters

Gets the reviewers filters for specified project.

#### Request

```
  GET /projects/myproject/reviewers-filters HTTP/1.0
```

As response a List of [ReviewerFilter](#reviewer-filter) is returned
that describes the default reviewers for myproject.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8
  )]}'
  [
    {
      "filter": "branch:master",
      "reviewers": [
        "UserA",
        "UserB"
      ]
    }
  ]
```

### <a id="upsert-reviewer-filter"> Upsert Reviewers Filter
_PUT /projects/project_name/reviewers-filters/$FILTER

Sets the default reviewers for open changes in the specified project that
matches the query expression $FILTER.

The change to reviewers must be provided in the request body inside
a [ReviewersFilterInput](#reviewers-filter-input) entity.

Caller must be a member of a group that is granted the 'Modify Reviewers Config'
capability (provided by this plugin) or be a Project Owner for the project.

#### Request

```
  PUT /projects/myproject/reviewers-filters/file%3A%5Elib%2F* HTTP/1.0
  Content-Type: application/json;charset=UTF-8
  {
    "reviewers": ["UserB", "UserC"]
  }
```

As response the default reviewers are returned as a list of
[ReviewerFilterSection](#reviewer-filter-section).

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8
  )]}'
  [
    {
      "filter": "branch:master",
      "reviewers": [
        "UserA",
        "UserB"
      ]
    },
    {
      "filter": "file:^lib/*",
      "reviewers": [
        "UserB",
        "UserC"
      ]
    }
  ]

```


<a id="json-entities">JSON Entities
-----------------------------------

### <a id="reviewer-filter"></a>ReviewerFilterSection

The `ReviewerFilter` entity contains a filter section of the
default reviewers.

* _filter_: A filter that is used to assign default reviewers.
* _reviewers_: List of usernames which are assigned as default reviewers
 under the filter.

### <a id="config-reviewers-input"></a>ConfigReviewersInput

The `ReviewersFilterInput` entity contains an update for the default
reviewers.

* _reviewers_: List of account ids that should be added as reviewers on changes
               matching the filter.

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
