# daf-event-producers

## Synopsis
This is a microservice which ingest data from several API and Web Services and send them to Kafka topics. Data are converted from their original schema into an Avro schema named `Event`.

The ingestion is scheduled and it pays attention to do not ingest duplicated data.

## Development

### Directory structure

- `kubernetes`: Kubernetes `.yml` description files
- `project`: project definition files
- `src`: source code and property files

### How to create a docker image with sbt
> sbt -Ddocker.name=daf-event-producers

### How to publish a docker image
> docker:publish

## Built with
- Scala 2.11.8
- Apache Kafka 0.10.1.1

More detailed information about dependencies and their version could be found within `build.sbt` file.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/teamdigitale/daf-event-producers/tags).

## Authors
* **Fabiana Lanotte** - [fabiana001](https://github.com/fabiana001)
* **Alessandro Tundo** - [aletundo](https://github.com/aletundo)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the AGPLv3+. See the [LICENSE.md](LICENSE.md) file for details
