# tls-over-ndn

This project is a proof-of-concept demonstrating how TLS can be implemented over
NDN using Interest payloads

## Build

To build, run `mvn install` in the cloned directory.

## Use

TODO

## Logging

This project uses Java's default logging utilities (see http://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html). Most messages are logged with the FINER or FINEST status; one way to change this is to add a `logging.properties` file in the classpath with the following lines:
```
handlers=java.util.logging.ConsoleHandler
.level=FINEST
java.util.logging.ConsoleHandler.level=FINEST
```

## License

This program is free software; you can redistribute it and/or modify it under the terms and conditions of the GNU Lesser General Public License, version 3, as published by the Free Software Foundation.

This program is distributed in the hope it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the [GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl-3.0.en.html) for more details.
