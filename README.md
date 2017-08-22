CytoMCS
=======

CytoMCS is a Cytoscape 3.0 app for computing the maximum common edge subgraph between two or more large networks.
The app uses an iterative local search algorithm to find large conserved subgraphs, and is able to detect not only fully conserved edges,
but also partially conserved. CytoMCS supports both directed and undirected networks.

## Installation

The Cytoscape app is available through the Cytoscape app store here: http://apps.cytoscape.org/apps/cytomcs.

## Compilation

First clone and compile the FaithMCS repository:

```
git clone https://github.com/SimonLarsen/faithmcs.git
cd faithmcs
mvn package
```

Then clone the CytoMCS repository, install the FaithMCS package locally and compile:

```
git clone https://github.com/SimonLarsen/cytomcs.git
cd cytomcs
mvn install:install-file -Dfile=/path/to/faithmcs-0.2.jar
mvn package
```

The compiled Cytoscape 3.0 app can then be found in cytomcs/target/cytomcs-1.1.jar.

## Source code

The source code for CytoMCS and FaithMCS is available here:

* https://github.com/SimonLarsen/cytomcs
* https://github.com/SimonLarsen/faithmcs

## License

CytoMCS is licensed under the GPU General Public License v3.0.
See https://www.gnu.org/licenses/gpl-3.0.en.html for more information.

## Reference

Larsen, Simon J., and Jan Baumbach. "CytoMCS: A Multiple Maximum Common Subgraph Detection Tool for Cytoscape." *Journal of Integrative Bioinformatics* 14.2 (2017).
