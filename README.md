# github-actions-autograding

This actions plugin will build your project with Maven, parse the generated reports, grade the results and give feedback on the pull request.


### How to use?

This is an example config you could use in your workflow.

```
name: Java CI with Maven

on:
  pull_request:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Java CI with Maven
        uses: TobiMichael96/github-actions-autograding@0.1.0
        with:
          TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

#### Configuration

- ``TOKEN: ThisIsSomeToken`` (mandatory) to change the token.
- ``CONFIG: /path/to/config`` or ``CONFIG: "{\"analysis\": { \"maxScore\": 100, \"errorImpact\": -5}}"`` to provide a config, defaults to /default.conf.
- ``DEBUG: true`` to enable debug.
