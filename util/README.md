This directory contains files you'll want to copy to the actual
data source modules you build based on the data platform.

The "resources" directory provides a persistence configuration
and properties files needed to access EveKit account management
\(if you require ESI tokens\), as well as the data platform
management for managing data sources and update trackers.

The "ekdptool" provides command line access for data sources
which need to run outside of Java.  You'll need to customize
this tool to point to the proper jar assembly you build
in your data source module.
