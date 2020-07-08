# github-actions-autograding

This actions plugin will build your project with Maven, parse the generated reports, grade the results and give feedback on the pull request.


### How to use?

TODO


#### Configuration

- ``-d`` to enable debug.

- ``-c /path/to/config/`` or ``-c "{"analysis": { "maxScore": 600, "errorImpact": -5}}"`` to set the config, see [autograding-model](https://github.com/uhafner/autograding-model) for more.

- ``-t GITHUB_TOKEN`` to set the token.
