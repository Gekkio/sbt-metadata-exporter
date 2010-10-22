sbt-metadata-exporter
=================

## Introduction

sbt-metadata-exporter is a processor for [Simple Build Tool](http://code.google.com/p/simple-build-tool/) that exports SBT project metadata into an XML file that can be consumed by an external tool. _This is a proof of concept project that will initially be used for better Eclipse integration for SBT projects_. In theory it could be used for other SBT-related external tool integrations.

## Usage

First, tell SBT about the processor (this needs to be done only once per user/machine):
    *jawsy at http://oss.jawsy.fi/maven2/releases
    *metadata is fi.jawsy sbt-metadata-exporter 0.1.0

Then, in _any SBT project_, you can run
    metadata
