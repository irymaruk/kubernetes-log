# Azure CosmosDB command line client for SQL queries

### Preconditions:

1. `mvn clean package`
2. Copy _kubernetes-log-jar-with-dependencies.jar_ into user home
3. Add alias in git-bash and restart `echo alias log=\'java -jar ~/kubernetes-log.jar\' >> ~/.bashrc`

### Usage examples:

In git-bash type "log", select namespace number and available pod number.

![](https://github.com/irymaruk/kubernetes-log/blob/master/src/test/resources/kubernetes-log.png)

### Output

On the screen above we choose to stream logs from all pods (in parallel) that have label app=clinicaldecisionsupport on
Dev environment.

The line _">>>>> clinicaldecisionsupport-devenv1-v084-8x7wm"_ indicates from what pod below log lines.

If another pod start producing logs you'll see ending line for current pod and starting line for different one:

```
<<<<< clinicaldecisionsupport-devenv1-v084-8x7wm

>>>>> clinicaldecisionsupport-devenv1-v084-sxph5
```
